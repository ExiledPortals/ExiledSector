package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class FleetLossTally {

    private static final class FleetTotals {
        private float deploymentPoints;
        private float difficultyWeighted;
        private float shipLevelWeighted;

        FleetLoss toLoss(FleetLootRules rules) {
            return new FleetLoss(deploymentPoints, difficultyWeighted / deploymentPoints, shipLevelWeighted / deploymentPoints, rules);
        }
    }

    private final Map<String, Float> deploymentPointsByFaction = new LinkedHashMap<>();
    private final Map<CampaignFleetAPI, FleetTotals> totalsByFleet = new LinkedHashMap<>();
    private final List<FleetLoss> engagementLossesAtRawDifficulty = new ArrayList<>();

    public void record(BattleEngagement engagement, Function<CampaignFleetAPI, FleetLootRules> rulesAtRecordTime) {
        engagement.deploymentPointsLostByFaction().forEach((factionId, deploymentPoints) ->
                deploymentPointsByFaction.merge(factionId, deploymentPoints, Float::sum));
        float cappedDifficulty = BattleLootRates.difficulty(engagement.difficultyMultiplier());
        engagement.lostEnemiesBySourceFleet().forEach((sourceFleet, members) -> {
            float engagementShipLevelWeighted = 0f;
            for (FleetMemberAPI member : members) {
                float deploymentPoints = member.getDeploymentPointsCost();
                if (deploymentPoints > 0f) {
                    int shipLevel = FleetDifficulties.shipLevel(member);
                    FleetTotals totals = totalsByFleet.computeIfAbsent(sourceFleet, fleet -> new FleetTotals());
                    totals.deploymentPoints += deploymentPoints;
                    totals.difficultyWeighted += deploymentPoints * cappedDifficulty;
                    totals.shipLevelWeighted += deploymentPoints * shipLevel;
                    engagementShipLevelWeighted += deploymentPoints * shipLevel;
                }
            }
            float engagementDeploymentPoints = BattleEngagement.deploymentPoints(members);
            FleetLootRules rules = rulesAtRecordTime.apply(sourceFleet);
            if (engagementDeploymentPoints > 0f) {
                engagementLossesAtRawDifficulty.add(new FleetLoss(engagementDeploymentPoints, engagement.difficultyMultiplier(),
                        engagementShipLevelWeighted / engagementDeploymentPoints, rules));
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

    public List<FleetLoss> fleetLosses(Function<CampaignFleetAPI, FleetLootRules> currentRules) {
        List<FleetLoss> fleetLosses = new ArrayList<>(totalsByFleet.size());
        totalsByFleet.forEach((fleet, totals) -> fleetLosses.add(totals.toLoss(currentRules.apply(fleet))));
        return fleetLosses;
    }

    public List<FleetLoss> engagementLossesAtRawDifficulty() {
        return List.copyOf(engagementLossesAtRawDifficulty);
    }
}
