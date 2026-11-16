package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.anomaly.excavation.ExcavationDeliveries;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class ConvoyBoltHole {

    private ConvoyBoltHole() {
    }

    static boolean isOn(ChartGenContext context) {
        return ChartGenContext.orNone(context).anomalyPercent(SupplyConvoy.EXCAVATION_DELIVERY_KEY) > 0f;
    }

    static void reroute(List<ConvoyState> convoys, List<String> sitePlanetIds) {
        if (sitePlanetIds.isEmpty()) {
            return;
        }
        Map<String, Integer> headedTo = new LinkedHashMap<>();
        sitePlanetIds.forEach(planetId -> headedTo.put(planetId, 0));
        for (ConvoyState convoy : convoys) {
            headedTo.computeIfPresent(convoy.destinationId(), (planetId, count) -> count + 1);
        }
        for (ConvoyState convoy : convoys) {
            if (convoy.phase() != ConvoyState.Phase.WAITING || headedTo.containsKey(convoy.destinationId())) {
                continue;
            }
            String planetId = leastHeadedTo(headedTo);
            convoy.routeTo(planetId);
            headedTo.merge(planetId, 1, Integer::sum);
        }
    }

    private static String leastHeadedTo(Map<String, Integer> headedTo) {
        String least = null;
        int fewest = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : headedTo.entrySet()) {
            if (entry.getValue() < fewest) {
                least = entry.getKey();
                fewest = entry.getValue();
            }
        }
        return least;
    }

    static boolean deliver(SectorAPI sector, ChartRun run, ConvoyState convoy, CampaignFleetAPI fleet, SectorEntityToken destination, Random random) {
        ChartGenContext context = ChartRunEffects.contextOf(run);
        if (!isOn(context) || !ExcavationDeliveries.accepts(run, destination)) {
            return false;
        }
        ExcavationDeliveries.joinGuardians(run, destination, fleet);
        CargoAPI cache = ConvoyCargo.deliveredCache(sector, run, convoy, fleet, FeedstockBonus.excavationShare(context), random);
        ExcavationDeliveries.deliverCache(run, destination, cache, context.anomalyPercent(SupplyConvoy.EXCAVATION_RAID_DIFFICULTY_KEY),
                context.anomalyPercent(SupplyConvoy.EXCAVATION_LOOT_KEY));
        convoy.markDeliveredToExcavation();
        return true;
    }
}
