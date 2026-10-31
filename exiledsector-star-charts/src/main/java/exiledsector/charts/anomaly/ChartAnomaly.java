package exiledsector.charts.anomaly;

public interface ChartAnomaly {

    String id();

    default void loadData() {
    }

    default void loadItems() {
    }

    default void preloadTextures() {
    }
}
