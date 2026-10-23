package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.compat.NightcrossUnlocks;
import exiledsector.skills.unlock.AnchorFinds;

import java.util.List;
import java.util.Map;

public final class AnchorFindMigration {

    static final String DONE_KEY = "exiledSector_anchorFindMigrationDone";
    static final String NIGHTCROSS_VOLUME = "nightcross";
    private static final String HELD_REASON = "the save already held one";

    private AnchorFindMigration() {
    }

    public static void run(SectorAPI sector) {
        Map<String, Object> persistentData = sector == null ? null : sector.getPersistentData();
        if (persistentData == null || Boolean.TRUE.equals(persistentData.get(DONE_KEY))) {
            return;
        }
        recordHeldAnchors(sector, MaterialMigration.savedCargoes(sector), ItemStore.get().owned());
        persistentData.put(DONE_KEY, Boolean.TRUE);
    }

    static void recordHeldAnchors(SectorAPI sector, List<CargoAPI> cargoes, List<Item> ownedItems) {
        if (NightcrossUnlocks.pascalVisitedBeforeAnchors(sector)) {
            AnchorFinds.markFound(sector, NIGHTCROSS_VOLUME, "the save had entered Pascal");
        }
        for (CargoAPI cargo : cargoes) {
            for (CargoStackAPI stack : cargo.getStacksCopy()) {
                SpecialItemData special = stack.isSpecialStack() ? stack.getSpecialDataIfSpecial() : null;
                ItemData itemData = special == null || ItemData.isMarketListing(special) ? null : ItemData.of(special);
                markIfAnchor(sector, itemData == null ? null : itemData.definition());
            }
        }
        for (Item item : ownedItems) {
            markIfAnchor(sector, item == null ? null : item.definition());
        }
    }

    private static void markIfAnchor(SectorAPI sector, ItemDefinition definition) {
        if (definition != null && definition.type() == ItemType.WORMHOLE_ANCHOR) {
            AnchorFinds.markFound(sector, WormholeAnchorDefinitions.volume(definition.id()), HELD_REASON);
        }
    }
}
