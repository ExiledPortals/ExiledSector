package exiledsector.charts.content;

import java.util.List;

public record ChartContent(int tier, String factionId, String bossFleetId, ChartBossType bossType, int fleets, int pois, List<String> anomalies,
                           String protectedFleetId, String bossFlagshipId) {

    public ChartContent {
        anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
        bossType = bossType == null ? ChartBossType.FLEET : bossType;
    }

    public ChartContent(int tier, String factionId, String bossFleetId, ChartBossType bossType, int fleets, int pois, List<String> anomalies,
                        String protectedFleetId) {
        this(tier, factionId, bossFleetId, bossType, fleets, pois, anomalies, protectedFleetId, null);
    }

    public ChartContent(int tier, String factionId, String bossFleetId, ChartBossType bossType, int fleets, int pois, List<String> anomalies) {
        this(tier, factionId, bossFleetId, bossType, fleets, pois, anomalies, null);
    }

    public ChartContent(int tier, String factionId, String bossFleetId, int fleets, int pois, List<String> anomalies) {
        this(tier, factionId, bossFleetId, ChartBossType.FLEET, fleets, pois, anomalies);
    }

    public ChartContent(int tier, String factionId, String bossFleetId, int fleets, int pois) {
        this(tier, factionId, bossFleetId, fleets, pois, List.of());
    }

    public static ChartContent none(int tier) {
        return new ChartContent(tier, null, null, 0, 0);
    }
}
