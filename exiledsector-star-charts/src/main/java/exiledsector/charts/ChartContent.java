package exiledsector.charts;

import java.util.List;

public record ChartContent(int tier, String factionId, ChartObjective objective, List<String> targetIds, String bossFleetId,
                           ChartBossType bossType, int fleets, int pois, List<String> anomalies) {

    public ChartContent {
        targetIds = targetIds == null ? List.of() : List.copyOf(targetIds);
        anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
        bossType = bossType == null ? ChartBossType.FLEET : bossType;
    }

    public ChartContent(int tier, String factionId, ChartObjective objective, List<String> targetIds, String bossFleetId, int fleets, int pois,
                        List<String> anomalies) {
        this(tier, factionId, objective, targetIds, bossFleetId, ChartBossType.FLEET, fleets, pois, anomalies);
    }

    public ChartContent(int tier, String factionId, ChartObjective objective, List<String> targetIds, String bossFleetId, int fleets, int pois) {
        this(tier, factionId, objective, targetIds, bossFleetId, fleets, pois, List.of());
    }

    public static ChartContent none(int tier) {
        return new ChartContent(tier, null, null, List.of(), null, 0, 0, List.of());
    }
}
