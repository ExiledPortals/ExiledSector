package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.items.ItemStore;
import exiledsector.loot.LootTakenListener;

public final class ChartStorageTransfer implements LootTakenListener {

    @Override
    public void reportLootTaken(CargoAPI playerCargo) {
        if (ItemStore.carriesStarCharts(playerCargo)) {
            ItemStore.starCharts().absorbFrom(playerCargo);
        }
    }
}
