package exiledsector.charts.generation;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.content.ChartContent;

public record ChartGenerated(StarSystemAPI system, String runId, ChartContent content, ChartGenContext context) {
}
