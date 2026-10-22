package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import exiledlib.items.Item;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemIdAliases;
import exiledlib.items.ItemStore;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DeletedSubroutines {

    static final Set<String> DELETED_DEFINITION_IDS = Set.of("domain_subroutine_military", "domain_subroutine_industrial",
            "domain_subroutine_consumer", "domain_subroutine_management");
    private static final Logger LOG = Logger.getLogger(DeletedSubroutines.class);

    private DeletedSubroutines() {
    }

    public static void removeFromSave(List<CargoAPI> savedCargoes) {
        if (ItemDefinitions.all().isEmpty()) {
            return;
        }
        int removedFromStorageCount = remove(ShipSkillDataManager.all(), ItemStore.get());
        int removedFromCargoCount = removeFromCargoes(savedCargoes);
        if (removedFromStorageCount > 0 || removedFromCargoCount > 0) {
            LOG.info("Removed deleted subroutines: " + removedFromStorageCount + " from storage and sockets, " + removedFromCargoCount + " from cargo");
        }
    }

    static int remove(Map<String, ShipSkillData> shipDataById, ItemStore store) {
        int removedCount = 0;
        for (ShipSkillData shipData : shipDataById.values()) {
            for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
                if (isDeleted(NpcItems.item(socketedEntry.getValue()))) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    removedCount++;
                } else if (isDeleted(store.find(socketedEntry.getValue()))) {
                    shipData.unsocketItem(socketedEntry.getKey());
                }
            }
        }
        for (Item ownedItem : new ArrayList<>(store.owned())) {
            if (isDeleted(ownedItem) && store.remove(ownedItem)) {
                removedCount++;
            }
        }
        return removedCount;
    }

    static int removeFromCargoes(List<CargoAPI> cargoes) {
        int removedCount = 0;
        for (CargoAPI cargo : cargoes) {
            removedCount += removeFromCargo(cargo);
        }
        return removedCount;
    }

    static int removeFromCargo(CargoAPI cargo) {
        if (cargo == null) {
            return 0;
        }
        int removedCount = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (isDeleted(ItemData.of(stack.getSpecialDataIfSpecial()))) {
                removedCount += Math.max(1, Math.round(stack.getSize()));
                cargo.removeStack(stack);
            }
        }
        return removedCount;
    }

    static boolean isDeleted(Item item) {
        return item != null && isDeleted(item.definitionId());
    }

    public static boolean isDeleted(ItemData itemData) {
        return itemData != null && isDeleted(itemData.definitionId());
    }

    private static boolean isDeleted(String definitionId) {
        String currentId = ItemIdAliases.currentId(definitionId);
        return currentId != null && DELETED_DEFINITION_IDS.contains(currentId) && ItemDefinitions.get(currentId) == null;
    }
}
