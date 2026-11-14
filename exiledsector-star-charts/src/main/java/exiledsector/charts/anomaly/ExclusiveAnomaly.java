package exiledsector.charts.anomaly;

import java.util.Set;

public interface ExclusiveAnomaly {

    String id();

    boolean replacesStar();

    Set<String> excludes();

    default boolean conflictsWith(ExclusiveAnomaly other) {
        return other != null && !id().equals(other.id())
                && (replacesStar() && other.replacesStar() || excludes().contains(other.id()) || other.excludes().contains(id()));
    }
}
