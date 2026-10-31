package exiledsector.charts.content;

import exiledsector.charts.objective.ChartObjective;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets,
                               List<String> anomalies, ChartBossType bossType, PlannedStation bossStation, boolean alliedDefenders,
                               Map<String, Object> anomalyPlans) {

    public enum FleetRole {
        GUARD,
        PATROL,
        BOSS,
        DEFENDER;

        public boolean bossFleet() {
            return this == BOSS || this == DEFENDER;
        }
    }

    public record PlannedPoi(ChartPoi poi, boolean guarded) {
    }

    public record PlannedFleet(FleetRole role, int poiIndex, float combatPoints, float quality, int officerLevelBonus, int officerCountBonus,
                               int strengthBonus, int averageSMods) {

        public boolean guardsPoi() {
            return poiIndex >= 0;
        }
    }

    public record PlannedStation(String variantId, int officerLevel, PlannedFleet fallbackBoss) {
    }

    public ChartContentPlan {
        pois = List.copyOf(pois);
        fleets = List.copyOf(fleets);
        anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
        bossType = bossType == null ? ChartBossType.FLEET : bossType;
        anomalyPlans = anomalyPlans == null ? Map.of() : Map.copyOf(anomalyPlans);
    }

    public ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets,
                            List<String> anomalies, ChartBossType bossType, PlannedStation bossStation) {
        this(tier, faction, objective, pois, fleets, anomalies, bossType, bossStation, false, Map.of());
    }

    public ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets,
                            List<String> anomalies) {
        this(tier, faction, objective, pois, fleets, anomalies, ChartBossType.FLEET, null);
    }

    public ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets) {
        this(tier, faction, objective, pois, fleets, List.of());
    }

    public ChartContentPlan withObjective(ChartObjective changed) {
        return new ChartContentPlan(tier, faction, changed, pois, fleets, anomalies, bossType, bossStation, alliedDefenders, anomalyPlans);
    }

    public ChartContentPlan withDefenders(List<PlannedFleet> changedFleets, PlannedStation changedStation, boolean allied) {
        return new ChartContentPlan(tier, faction, objective, pois, changedFleets, anomalies, bossType, changedStation, allied, anomalyPlans);
    }

    public ChartContentPlan withAnomalyPlan(String anomalyId, Object anomalyPlan) {
        Map<String, Object> changed = new LinkedHashMap<>(anomalyPlans);
        changed.put(anomalyId, anomalyPlan);
        return new ChartContentPlan(tier, faction, objective, pois, fleets, anomalies, bossType, bossStation, alliedDefenders, changed);
    }

    public Object anomalyPlan(String anomalyId) {
        return anomalyPlans.get(anomalyId);
    }

    public float lootSpent() {
        float spent = 0f;
        for (PlannedPoi planned : pois) {
            spent += planned.poi().cost();
        }
        return spent;
    }

    public float fleetPoints(FleetRole role) {
        float points = 0f;
        for (PlannedFleet fleet : fleets) {
            if (fleet.role() == role) {
                points += fleet.combatPoints();
            }
        }
        return points;
    }

    public long count(FleetRole role) {
        return fleets.stream().filter(fleet -> fleet.role() == role).count();
    }
}
