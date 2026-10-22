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
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemType;
import exiledsector.items.MaterialItems;
import exiledsector.items.ItemUnlock;
import exiledsector.items.SubroutineKind;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.loot.LootModifiers;
import exiledsector.loot.LootSources;
import exiledsector.ui.cargo.MaterialDropAnnouncer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ItemSalvageListener implements ShowLootListener {

    static final String ROLLED_KEY = "$exiledSector_socketablesRolled";
    private static final long SEED_SALT = 0x736F636B65746BL;
    static final String SHOWCASE_KEY = "$exiledSector_showcaseLoot";
    private static final long SHOWCASE_SEED_SALT = 0x73686F77636173L;
    static final int SHOWCASE_PICKS = 3;

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
        LootModifiers modifiers = LootSources.modifiersAt(salvageEntity.getContainingLocation());
        ItemDrops.SiteLoot siteLoot = ItemDrops.rollSite(siteId, random, modifiers.itemChances(), modifiers.feedstockMult(), modifiers.currencyMult(),
                ItemUnlock.sectionsOpen(Global.getSector()));
        siteLoot.items().forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
        siteLoot.upgrades().forEach(upgrade -> loot.addSpecial(upgrade.toSpecialItem(), 1f));
        siteLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announce(siteLoot.materials());
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
        List<ItemDefinition> doctrines = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            if (definition.subroutineKind() == SubroutineKind.DOCTRINE) {
                doctrines.add(definition);
            } else if (!definition.unique()) {
                loot.addSpecial(ItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
            }
        }
        List<ItemDefinition> anchors = List.copyOf(ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR));
        List<ItemDefinition> activeAnchors = anchors.stream().filter(anchor -> WormholeAnchorDefinitions.areaActive(anchor.id())).toList();
        addRandomPicks(loot, doctrines, random);
        addRandomPicks(loot, activeAnchors.isEmpty() ? anchors : activeAnchors, random);
        Map<String, Integer> materials = new LinkedHashMap<>();
        MaterialItems.ids().forEach(itemId -> materials.put(itemId, 1));
        materials.forEach((itemId, quantity) -> MaterialItems.add(loot, itemId, quantity));
        MaterialDropAnnouncer.announce(materials);
    }

    private static void addRandomPicks(CargoAPI loot, List<ItemDefinition> definitions, Random random) {
        List<ItemDefinition> shuffled = new ArrayList<>(definitions);
        Collections.shuffle(shuffled, random);
        for (ItemDefinition definition : shuffled.subList(0, Math.min(SHOWCASE_PICKS, shuffled.size()))) {
            loot.addSpecial(ItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
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
