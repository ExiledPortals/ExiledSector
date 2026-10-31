package exiledsector.charts.anomaly;

import java.util.Set;

public interface ChartAnomaly {

    String id();

    default Set<String> runFactionIds() {
        return Set.of();
    }

    default void loadData() {
    }

    default void loadItems() {
    }

    default void preloadTextures() {
    }
}
