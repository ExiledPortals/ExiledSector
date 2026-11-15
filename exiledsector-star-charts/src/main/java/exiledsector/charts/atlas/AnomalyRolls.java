package exiledsector.charts.atlas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class AnomalyRolls<T> {

    private record Passed<T>(T anomaly, String id, double roll) {
    }

    private final List<Passed<T>> passed = new ArrayList<>();

    void offer(T anomaly, String id, double roll, double chance) {
        if (roll < chance) {
            passed.add(new Passed<>(anomaly, id, roll));
        }
    }

    List<T> lowestFirst() {
        return passed.stream().sorted(Comparator.<Passed<T>>comparingDouble(Passed::roll).thenComparing(Passed::id)).map(Passed::anomaly).toList();
    }
}
