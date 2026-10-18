package exiledsector.charts;

import com.fs.starfarer.api.campaign.StarSystemAPI;

public record ChartGenerated(StarSystemAPI pocket, String runId, ChartContent content, ChartGenContext context) {
}
