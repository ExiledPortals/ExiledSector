package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.charts.objective.ChartObjective;

import java.util.List;

public record SettledObjective(ChartObjective objective, List<String> targetIds, CampaignFleetAPI boss, ChartBossType bossType, String protectedFleetId) {

    public SettledObjective {
        targetIds = objective == null || targetIds == null ? List.of() : List.copyOf(targetIds);
        bossType = bossType == null ? ChartBossType.FLEET : bossType;
    }
}
