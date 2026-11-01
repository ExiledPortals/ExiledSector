package exiledsector.charts.anomaly;

public interface LayoutShaper extends ChartAnomaly {

    default int minPlanets() {
        return 0;
    }

    default void shape(AnomalyLayoutSite site) {
    }
}
