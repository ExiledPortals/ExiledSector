package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FleetLossTally {

    @FunctionalInterface
    public interface ShipWeight {

        ShipWeight NONE = member -> 0f;

        float of(FleetMemberAPI member);
    }

    public record FleetLoss(CampaignFleetAPI fleet, float deploymentPoints, float averageDifficulty, float averageShipWeight) {
    }

    private static final class FleetTotals {
        private float deploymentPoints;
        private float difficultyWeighted;
        private float shipWeighted;
    }

    private final ShipWeight shipWeight;
    private final Map<String, Float> deploymentPointsByFaction = new LinkedHashMap<>();
    private final Map<CampaignFleetAPI, FleetTotals> totalsByFleet = new LinkedHashMap<>();

    public FleetLossTally() {
        this(ShipWeight.NONE);
    }

    public FleetLossTally(ShipWeight shipWeight) {
        this.shipWeight = shipWeight;
    }

    public void record(BattleEngagement engagement) {
        engagement.deploymentPointsLostByFaction().forEach((factionId, deploymentPoints) ->
                deploymentPointsByFaction.merge(factionId, deploymentPoints, Float::sum));
        float difficulty = BattleLootRates.difficulty(engagement.difficultyMultiplier());
        engagement.lostEnemiesBySourceFleet().forEach((sourceFleet, members) -> {
            for (FleetMemberAPI member : members) {
                float deploymentPoints = member.getDeploymentPointsCost();
                if (deploymentPoints > 0f) {
                    FleetTotals totals = totalsByFleet.computeIfAbsent(sourceFleet, fleet -> new FleetTotals());
                    totals.deploymentPoints += deploymentPoints;
                    totals.difficultyWeighted += deploymentPoints * difficulty;
                    totals.shipWeighted += deploymentPoints * shipWeight.of(member);
                }
            }
        });
    }

    public Map<String, Float> deploymentPointsByFaction() {
        return deploymentPointsByFaction;
    }

    public String mostDefeatedFaction() {
        String mostDefeated = null;
        for (Map.Entry<String, Float> defeated : deploymentPointsByFaction.entrySet()) {
            if (mostDefeated == null || defeated.getValue() > deploymentPointsByFaction.get(mostDefeated)) {
                mostDefeated = defeated.getKey();
            }
        }
        return mostDefeated;
    }

    public List<FleetLoss> fleetLosses() {
        List<FleetLoss> fleetLosses = new ArrayList<>(totalsByFleet.size());
        totalsByFleet.forEach((fleet, totals) -> fleetLosses.add(new FleetLoss(fleet, totals.deploymentPoints,
                totals.difficultyWeighted / totals.deploymentPoints, totals.shipWeighted / totals.deploymentPoints)));
        return fleetLosses;
    }
}
