package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.content.ChartContentPlan.PlannedFleet;

import java.util.List;

public record SkirmishPlan(String attackerFactionId, SkirmishSide side, List<PlannedFleet> attackerFleets) {

    public SkirmishPlan {
        side = side == null ? SkirmishSide.NEITHER : side;
        attackerFleets = List.copyOf(attackerFleets);
    }

    public boolean alliedWith(SkirmishSide fleetSide) {
        return side == fleetSide;
    }
}
