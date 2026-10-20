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
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.DropType;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemType;
import exiledsector.items.MaterialItems;
import exiledsector.items.ItemUnlock;
import exiledsector.ui.cargo.MaterialDropAnnouncer;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class ItemSalvageListener implements ShowLootListener {

    static final String ROLLED_KEY = "$exiledSector_socketablesRolled";
    private static final long SEED_SALT = 0x736F636B65746BL;
    private static final long CHART_SEED_SALT = 0x63686172746470L;
    static final String SHOWCASE_KEY = "$exiledSector_showcaseLoot";
    private static final long SHOWCASE_SEED_SALT = 0x73686F77636173L;
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
        ChartGenContext chartContext = chartContext(salvageEntity);
        ItemDrops.SiteLoot siteLoot = ItemDrops.rollSite(siteId, random, chartContext.itemChances(), chartContext.dropMult(DropType.FEEDSTOCK),
                chartContext.dropMult(DropType.CURRENCY), ItemUnlock.sectionsOpen(Global.getSector()));
        siteLoot.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
        siteLoot.upgrades().forEach(upgrade -> loot.addSpecial(upgrade.toSpecialItem(), 1f));
        siteLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announce(siteLoot.materials());
        ChartDrops.Loot chartLoot = chartLoot(Global.getSector(), salvageEntity);
        chartLoot.charts().forEach(chart -> loot.addSpecial(chart.toSpecialItem(), 1f));
        chartLoot.currencies().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announce(chartLoot.currencies());
        if (entityMemory.getBoolean(SHOWCASE_KEY)) {
            entityMemory.unset(SHOWCASE_KEY);
            addShowcaseLoot(loot, new Random(Misc.getSalvageSeed(salvageEntity) ^ SHOWCASE_SEED_SALT));
        }
    }

    public static boolean canShowcase(SectorEntityToken salvageEntity) {
        MemoryAPI entityMemory = salvageEntity == null ? null : salvageEntity.getMemoryWithoutUpdate();
        return entityMemory != null && ItemDrops.hasRule(siteId(salvageEntity)) && !entityMemory.getBoolean(ROLLED_KEY);
    }

    public static void markShowcase(SectorEntityToken salvageEntity) {
        salvageEntity.getMemoryWithoutUpdate().set(SHOWCASE_KEY, true);
    }

    static void addShowcaseLoot(CargoAPI loot, Random random) {
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            loot.addSpecial(ItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
        }
        Map<String, Integer> materials = new LinkedHashMap<>();
        MaterialItems.ids().forEach(itemId -> materials.put(itemId, 1));
        materials.forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announce(materials);
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

    static ChartGenContext chartContext(SectorEntityToken salvageEntity) {
        try {
            return ChartDrops.liveContextAt(salvageEntity.getContainingLocation());
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Reading the Star Chart loot bonuses failed", e);
            return ChartGenContext.none();
        }
    }

    static String siteId(SectorEntityToken salvageEntity) {
        MemoryAPI entityMemory = salvageEntity.getMemoryWithoutUpdate();
        if (entityMemory != null && entityMemory.contains(MemFlags.SALVAGE_SPEC_ID_OVERRIDE)) {
            return entityMemory.getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE);
        }
        return salvageEntity.getCustomEntityType();
    }
}
