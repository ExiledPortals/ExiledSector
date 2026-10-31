package exiledsector.charts.anomaly;

import exiledsector.charts.content.ChartContentPlan;

public interface ContentPlacer extends ChartAnomaly {

    default boolean forcesStationBoss() {
        return false;
    }

    default ChartContentPlan adjustPlan(ChartContentPlan plan, ContentPlanSite site) {
        return plan;
    }

    void generate(AnomalyContentSite site);
}
