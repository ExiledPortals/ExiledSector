package exiledsector.loot;

import com.fs.starfarer.api.campaign.CargoAPI;

public interface LootTakenListener {
    void reportLootTaken(CargoAPI playerCargo);
}
