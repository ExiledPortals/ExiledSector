package exiledsector.charts;

import java.util.List;

public record ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets,
                               List<String> expansions, ChartBossType bossType, PlannedStation bossStation) {

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
        expansions = expansions == null ? List.of() : List.copyOf(expansions);
        bossType = bossType == null ? ChartBossType.FLEET : bossType;
    }

    public ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets,
                            List<String> expansions) {
        this(tier, faction, objective, pois, fleets, expansions, ChartBossType.FLEET, null);
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
