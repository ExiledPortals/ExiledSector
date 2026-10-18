package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ShowLootListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.ChartDrops;
import exiledsector.charts.ChartIds;
import exiledsector.items.ItemDrops;
import exiledsector.items.MaterialItems;
import exiledsector.items.ItemData;
import exiledsector.items.ItemUnlock;
import exiledsector.ui.cargo.MaterialDropAnnouncer;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Random;

public class ItemSalvageListener implements ShowLootListener {

    static final String ROLLED_KEY = "$exiledSector_socketablesRolled";
    private static final long SEED_SALT = 0x736F636B65746BL;
    private static final long CHART_SEED_SALT = 0x63686172746470L;
    private static final Logger LOG = Logger.getLogger(ItemSalvageListener.class);

    @Override
    public void reportAboutToShowLootToPlayer(CargoAPI loot, InteractionDialogAPI dialog) {
        SectorEntityToken salvageEntity = dialog == null ? null : dialog.getInteractionTarget();
        if (loot == null || salvageEntity == null || salvageEntity instanceof CampaignFleetAPI) {
            return;
        }
        String siteId = siteId(salvageEntity);
        MemoryAPI entityMemory = salvageEntity.getMemoryWithoutUpdate();
        if (!ItemDrops.hasRule(siteId) || entityMemory == null || entityMemory.getBoolean(ROLLED_KEY)) {
            return;
        }
        entityMemory.set(ROLLED_KEY, true);
        Random random = new Random(Misc.getSalvageSeed(salvageEntity) ^ SEED_SALT);
        float itemChanceMult = itemChanceMult(entityMemory);
        for (ItemData item : ItemDrops.roll(siteId, random, itemChanceMult)) {
            loot.addSpecial(item.toSpecialItem(), 1f);
        }
        Map<String, Integer> droppedMaterials = ItemDrops.rollMaterials(siteId, random);
        droppedMaterials.forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announce(droppedMaterials);
        if (ItemUnlock.sectionsOpen(Global.getSector())) {
            ItemDrops.SectionItemLoot sectionItemLoot = ItemDrops.rollSectionItemLoot(siteId, random, itemChanceMult);
            sectionItemLoot.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
            sectionItemLoot.upgrades().forEach(upgrade -> loot.addSpecial(upgrade.toSpecialItem(), 1f));
        }
        ChartDrops.Loot chartLoot = chartLoot(Global.getSector(), salvageEntity);
        chartLoot.charts().forEach(chart -> loot.addSpecial(chart.toSpecialItem(), 1f));
        chartLoot.currencies().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announce(chartLoot.currencies());
    }

    static ChartDrops.Loot chartLoot(SectorAPI sector, SectorEntityToken salvageEntity) {
        if (sector == null) {
            return ChartDrops.Loot.NONE;
        }
        try {
            Random chartRandom = new Random(Misc.getSalvageSeed(salvageEntity) ^ CHART_SEED_SALT);
            return ChartDrops.forSalvage(sector, salvageEntity.getContainingLocation(), chartRandom);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling Star Chart salvage drops failed", e);
            return ChartDrops.Loot.NONE;
        }
    }

    static float itemChanceMult(MemoryAPI entityMemory) {
        if (!entityMemory.contains(ChartIds.ITEM_CHANCE_MEMORY_KEY)) {
            return 1f;
        }
        float chanceMult = entityMemory.getFloat(ChartIds.ITEM_CHANCE_MEMORY_KEY);
        return chanceMult > 0f ? chanceMult : 1f;
    }

    static String siteId(SectorEntityToken salvageEntity) {
        MemoryAPI entityMemory = salvageEntity.getMemoryWithoutUpdate();
        if (entityMemory != null && entityMemory.contains(MemFlags.SALVAGE_SPEC_ID_OVERRIDE)) {
            return entityMemory.getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE);
        }
        return salvageEntity.getCustomEntityType();
    }
}
