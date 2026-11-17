package exiledsector.items.custody;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import exiledsector.SectorMarkets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class SavedCargo {

    private SavedCargo() {
    }

    public static List<CargoAPI> cargoes(SectorAPI sector) {
        List<CargoAPI> cargoes = new ArrayList<>();
        if (sector == null) {
            return cargoes;
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet != null) {
            cargoes.add(playerFleet.getCargo());
        }
        SectorMarkets.forEach(sector, market -> {
            for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                CargoAPI submarketCargo = submarket.getCargoNullOk();
                if (submarketCargo != null) {
                    cargoes.add(submarketCargo);
                }
            }
        });
        return cargoes;
    }

    public static List<CargoAPI> withFleets(SectorAPI sector, List<CargoAPI> savedCargoes) {
        List<CargoAPI> cargoes = new ArrayList<>(savedCargoes);
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        for (LocationAPI location : sector.getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) {
                if (fleet != playerFleet && fleet.getCargo() != null) {
                    cargoes.add(fleet.getCargo());
                }
            }
        }
        return cargoes;
    }

    public static int removeStacks(List<CargoAPI> cargoes, Predicate<CargoStackAPI> matches) {
        return removeStacks(cargoes, matches, SavedCargo::units);
    }

    public static int removeStacks(List<CargoAPI> cargoes, Predicate<CargoStackAPI> matches, ToIntFunction<CargoStackAPI> unitsOf) {
        int removedCount = 0;
        for (CargoAPI cargo : cargoes) {
            removedCount += removeStacks(cargo, matches, unitsOf);
        }
        return removedCount;
    }

    public static int removeStacks(CargoAPI cargo, Predicate<CargoStackAPI> matches) {
        return removeStacks(cargo, matches, SavedCargo::units);
    }

    public static int removeStacks(CargoAPI cargo, Predicate<CargoStackAPI> matches, ToIntFunction<CargoStackAPI> unitsOf) {
        if (cargo == null) {
            return 0;
        }
        int removedCount = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (matches.test(stack)) {
                removedCount += unitsOf.applyAsInt(stack);
                cargo.removeStack(stack);
            }
        }
        return removedCount;
    }

    public static int replaceStacks(List<CargoAPI> cargoes, Function<CargoStackAPI, SpecialItemData> replacementOf) {
        int replacedCount = 0;
        for (CargoAPI cargo : cargoes) {
            replacedCount += replaceStacks(cargo, replacementOf);
        }
        return replacedCount;
    }

    public static int replaceStacks(CargoAPI cargo, Function<CargoStackAPI, SpecialItemData> replacementOf) {
        if (cargo == null) {
            return 0;
        }
        int replacedCount = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            SpecialItemData replacement = replacementOf.apply(stack);
            if (replacement == null) {
                continue;
            }
            int stackCount = units(stack);
            cargo.removeStack(stack);
            if (stackCount > 0) {
                cargo.addSpecial(replacement, stackCount);
                replacedCount += stackCount;
            }
        }
        return replacedCount;
    }

    private static int units(CargoStackAPI stack) {
        return Math.round(stack.getSize());
    }
}
