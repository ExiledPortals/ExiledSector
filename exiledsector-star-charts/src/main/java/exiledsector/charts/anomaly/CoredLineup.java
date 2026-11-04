package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.i18n.Translation;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartTiers;

import java.util.List;
import java.util.Random;

public record CoredLineup(String factionId, String fleetType, CoredShip flagship, List<CoredShip> escorts, int escortsAtLowestTier,
                          int tiersPerExtraEscort) {

    public record CoredShip(String variantId, String aiCoreId) {
    }

    public CoredLineup {
        escorts = List.copyOf(escorts);
    }

    public int escortCount(int tier) {
        int extra = (ChartTiers.clamp(tier) - ChartTiers.LOWEST_TIER) / Math.max(1, tiersPerExtraEscort);
        return Math.min(escorts.size(), escortsAtLowestTier + extra);
    }

    public CampaignFleetAPI create(int tier, String nameKey, Random random) {
        CampaignFleetAPI fleet = ChartAnomalyFleets.emptyFleet(factionId, fleetType);
        if (fleet == null || !ChartAnomalyFleets.addCoreFlagship(fleet, flagship.variantId(), flagship.aiCoreId(), random)) {
            return null;
        }
        for (CoredShip escort : escorts.subList(0, escortCount(tier))) {
            ChartAnomalyFleets.addCoreShip(fleet, escort.variantId(), escort.aiCoreId(), random);
        }
        fleet.setName(Translation.gameText(nameKey));
        fleet.setNoFactionInName(true);
        return fleet;
    }
}
