package exiledsector.charts;

import exiledsector.charts.anomaly.skirmish.SkirmishSide;

import java.util.List;

public record ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets,
                               List<String> anomalies, ChartBossType bossType, PlannedStation bossStation, PlannedSkirmish skirmish) {

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

    public record PlannedSkirmish(String attackerFactionId, SkirmishSide side, List<PlannedFleet> attackerFleets) {

        public PlannedSkirmish {
            side = side == null ? SkirmishSide.NEITHER : side;
            attackerFleets = List.copyOf(attackerFleets);
        }

        public boolean alliedWith(boolean defending) {
            return side == (defending ? SkirmishSide.DEFENDER : SkirmishSide.ATTACKER);
        }
    }

    public ChartContentPlan {
        pois = List.copyOf(pois);
        fleets = List.copyOf(fleets);
        anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
        bossType = bossType == null ? ChartBossType.FLEET : bossType;
    }

    public ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets,
                            List<String> anomalies, ChartBossType bossType, PlannedStation bossStation) {
        this(tier, faction, objective, pois, fleets, anomalies, bossType, bossStation, null);
    }

    public ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets,
                            List<String> anomalies) {
        this(tier, faction, objective, pois, fleets, anomalies, ChartBossType.FLEET, null);
    }

    public ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets) {
        this(tier, faction, objective, pois, fleets, List.of());
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
