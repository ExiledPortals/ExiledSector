package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemStore;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.loot.LootTakenListener;
import exiledsector.skills.tags.SkillTags;

import java.util.List;

public final class OmegaAnchorAcquisition implements LootTakenListener {

    @Override
    public void reportLootTaken(CargoAPI playerCargo) {
        SectorAPI sector = Global.getSector();
        if (FactionEncounterGate.OMEGA.awaits(sector)) {
            recordIfHeld(sector, playerCargo, ItemStore.get().owned());
        }
    }

    static void recordIfHeld(SectorAPI sector, CargoAPI playerCargo, List<Item> ownedItems) {
        if (cargoHoldsOmegaAnchor(playerCargo) || storeHoldsOmegaAnchor(ownedItems)) {
            FactionEncounterGate.OMEGA.open(sector, "the player acquired an Omega anchor");
        }
    }

    private static boolean cargoHoldsOmegaAnchor(CargoAPI playerCargo) {
        if (playerCargo == null) {
            return false;
        }
        for (CargoStackAPI stack : playerCargo.getStacksCopy()) {
            SpecialItemData special = stack.isSpecialStack() ? stack.getSpecialDataIfSpecial() : null;
            ItemData itemData = special == null || ItemData.isMarketListing(special) ? null : ItemData.of(special);
            if (itemData != null && isOmegaAnchor(itemData.definitionId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean storeHoldsOmegaAnchor(List<Item> ownedItems) {
        for (Item item : ownedItems) {
            if (item != null && isOmegaAnchor(item.definitionId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOmegaAnchor(String definitionId) {
        return SkillTags.OMEGA_VOLUME.equals(WormholeAnchorDefinitions.volume(definitionId));
    }
}
