package exiledsector.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.items.ItemStore;

final class ItemStorageTransfer implements LootTakenListener {

    @Override
    public void reportLootTaken(CargoAPI playerCargo) {
        ItemStore.get().absorbFrom(playerCargo);
    }
}
