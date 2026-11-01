package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.ShowLootListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import exiledsector.items.ItemDrops;
import exiledsector.items.MaterialItems;
import exiledsector.items.ItemUnlock;
import exiledsector.loot.LootModifiers;
import exiledsector.loot.LootSources;
import exiledsector.ui.cargo.MaterialDropAnnouncer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class ItemSalvageListener implements ShowLootListener {

    static final String ROLLED_KEY = "$exiledSector_socketablesRolled";
    private static final long SEED_SALT = 0x736F636B65746BL;
    static final String SHOWCASE_KEY = "$exiledSector_showcaseLoot";
    private static final long SHOWCASE_SEED_SALT = 0x73686F77636173L;

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
        LootModifiers modifiers = LootSources.modifiersFor(salvageEntity);
        ItemDrops.SiteLoot siteLoot = LootSources.rollSite(Global.getSector(), siteId, random, modifiers);
        ItemLootListener.addSiteLoot(loot, siteLoot);
        MaterialDropAnnouncer.announceOnLootScreen(siteLoot.materials());
        LootSources.all().forEach(source -> source.addSalvageLoot(Global.getSector(), salvageEntity, loot));
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
        LootSources.all().forEach(source -> source.addShowcaseLoot(loot, random));
        Map<String, Integer> materials = new LinkedHashMap<>();
        MaterialItems.ids().stream().filter(itemId -> ItemUnlock.currencyUsable(itemId, Global.getSector())).forEach(itemId -> materials.put(itemId, 1));
        materials.forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announceOnLootScreen(materials);
    }

    public static String siteId(SectorEntityToken salvageEntity) {
        MemoryAPI entityMemory = salvageEntity.getMemoryWithoutUpdate();
        if (entityMemory != null && entityMemory.contains(MemFlags.SALVAGE_SPEC_ID_OVERRIDE)) {
            return entityMemory.getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE);
        }
        return salvageEntity.getCustomEntityType();
    }
}
