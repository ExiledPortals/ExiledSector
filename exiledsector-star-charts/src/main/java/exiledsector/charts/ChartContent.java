package exiledsector.charts;

import java.util.List;

public record ChartContent(int tier, String factionId, ChartObjective objective, List<String> targetIds, String bossFleetId,
                           int fleets, int pois) {

    public ChartContent {
        targetIds = targetIds == null ? List.of() : List.copyOf(targetIds);
    }

    public static ChartContent none(int tier) {
        return new ChartContent(tier, null, null, List.of(), null, 0, 0);
    }
}
