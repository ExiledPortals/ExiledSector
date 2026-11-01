package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public record SettledObjective(CampaignFleetAPI boss, ChartBossType bossType, String protectedFleetId) {

    public SettledObjective {
        bossType = bossType == null ? ChartBossType.FLEET : bossType;
    }
}
