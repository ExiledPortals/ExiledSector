package exiledsector.charts;

import java.util.List;

public record ChartContentPlan(int tier, ChartFaction faction, ChartObjective objective, List<PlannedPoi> pois, List<PlannedFleet> fleets) {

    public enum FleetRole {
        GUARD,
        PATROL,
        BOSS
    }

    public record PlannedPoi(ChartPoi poi, boolean guarded) {
    }

    public record PlannedFleet(FleetRole role, int poiIndex, float combatPoints, float quality, int officerLevelBonus, int officerCountBonus,
                               int averageSMods) {

        public boolean guardsPoi() {
            return poiIndex >= 0;
        }
    }

    public ChartContentPlan {
        pois = List.copyOf(pois);
        fleets = List.copyOf(fleets);
    }

    public float lootSpent() {
        float spent = 0f;
        for (PlannedPoi planned : pois) {
            spent += planned.poi().cost();
        }
        return spent;
    }

    public float fleetPower(FleetRole role) {
        float power = 0f;
        for (PlannedFleet fleet : fleets) {
            if (fleet.role() == role) {
                power += fleet.combatPoints();
            }
        }
        return power;
    }

    public long count(FleetRole role) {
        return fleets.stream().filter(fleet -> fleet.role() == role).count();
    }
}
