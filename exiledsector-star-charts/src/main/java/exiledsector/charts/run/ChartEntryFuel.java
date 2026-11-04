package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.charts.generation.ChartTiers;

public final class ChartEntryFuel {

    static final int LOWEST_TIER_FUEL = 1000;
    static final int FUEL_PER_TIER = 100;

    private ChartEntryFuel() {
    }

    public static int cost(int tier) {
        return LOWEST_TIER_FUEL + (ChartTiers.clamp(tier) - ChartTiers.LOWEST_TIER) * FUEL_PER_TIER;
    }

    public static int cost(ChartRun run) {
        return cost(run.tier());
    }

    public static boolean canAfford(ChartRun run, CampaignFleetAPI fleet) {
        CargoAPI cargo = fleet == null ? null : fleet.getCargo();
        return cargo != null && cargo.getFuel() >= cost(run);
    }

    static void charge(ChartRun run, CampaignFleetAPI fleet) {
        CargoAPI cargo = fleet == null ? null : fleet.getCargo();
        if (cargo != null) {
            cargo.removeFuel(Math.min(cost(run), cargo.getFuel()));
        }
    }
}
