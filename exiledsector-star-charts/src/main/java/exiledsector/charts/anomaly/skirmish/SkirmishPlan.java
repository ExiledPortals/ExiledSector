package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.content.ChartContentPlan.PlannedFleet;

public record SkirmishPlan(String attackerFactionId, int attackerOfficerId, int defenderOfficerId, PlannedFleet attackerFleet,
                           PlannedFleet defenderFleet) {
}
