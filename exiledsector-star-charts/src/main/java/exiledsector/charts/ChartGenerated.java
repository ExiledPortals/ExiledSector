package exiledsector.charts;

import com.fs.starfarer.api.campaign.StarSystemAPI;

public record ChartGenerated(StarSystemAPI system, String runId, ChartContent content, ChartGenContext context) {
}
