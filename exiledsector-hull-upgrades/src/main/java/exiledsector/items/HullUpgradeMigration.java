package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.persistence.SaveData;
import org.apache.log4j.Logger;

import java.util.List;

public final class HullUpgradeMigration {

    private static final Logger LOG = Logger.getLogger(HullUpgradeMigration.class);

    private HullUpgradeMigration() {
    }

    public static void runOnSave(List<CargoAPI> savedCargoes) {
        run(SaveData.findOrNull(ItemStore.dataKey(), ItemStore.class), savedCargoes);
    }

    static void run(ItemStore store, List<CargoAPI> savedCargoes) {
        int convertedFromStorage = store == null ? 0 : store.convertLegacyHullUpgrades();
        int convertedFromCargo = 0;
        for (CargoAPI cargo : savedCargoes) {
            convertedFromCargo += convertCargo(cargo);
        }
        if (convertedFromStorage > 0 || convertedFromCargo > 0) {
            LOG.info("Turned old Hull Upgrades into Standard Refit Schematics: " + convertedFromStorage + " from storage, "
                    + convertedFromCargo + " from cargo");
        }
    }

    static int convertCargo(CargoAPI cargo) {
        if (cargo == null) {
            return 0;
        }
        int convertedCount = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            SpecialItemData special = stack.getSpecialDataIfSpecial();
            if (special == null || !RefitSchematic.ITEM_ID.equals(special.getId()) || !RefitSchematic.isLegacyData(special.getData())) {
                continue;
            }
            int stackCount = Math.round(stack.getSize());
            cargo.removeStack(stack);
            if (stackCount > 0) {
                cargo.addSpecial(RefitSchematic.STANDARD.toSpecialItem(), stackCount);
                convertedCount += stackCount;
            }
        }
        return convertedCount;
    }
}
