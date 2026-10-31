package exiledsector.charts.anomaly;

import exiledsector.charts.run.ChartRun;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class AnomalyState {

    static final String FIELD_SEPARATOR = ".";
    private static final AnomalyState NONE = new AnomalyState("", key -> null, (key, value) -> {
    });

    private final String anomalyId;
    private final Function<String, String> reader;
    private final BiConsumer<String, String> writer;

    private AnomalyState(String anomalyId, Function<String, String> reader, BiConsumer<String, String> writer) {
        this.anomalyId = anomalyId;
        this.reader = reader;
        this.writer = writer;
    }

    public static AnomalyState of(ChartRun run, String anomalyId) {
        return run == null ? NONE : new AnomalyState(anomalyId, run::anomalyState, run::recordAnomalyState);
    }

    static AnomalyState staged(String anomalyId, Map<String, String> staged) {
        return new AnomalyState(anomalyId, staged::get, (key, value) -> {
            if (value == null || value.isEmpty()) {
                staged.remove(key);
            } else {
                staged.put(key, value);
            }
        });
    }

    public String text(String field) {
        return reader.apply(key(field));
    }

    public boolean has(String field) {
        return text(field) != null;
    }

    public int integer(String field) {
        String stored = text(field);
        try {
            return stored == null ? 0 : Integer.parseInt(stored);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public <E extends Enum<E>> E enumValue(String field, Class<E> type) {
        String stored = text(field);
        try {
            return stored == null ? null : Enum.valueOf(type, stored);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void put(String field, String value) {
        writer.accept(key(field), value);
    }

    public void put(String field, int value) {
        put(field, Integer.toString(value));
    }

    public void put(String field, Enum<?> value) {
        put(field, value == null ? null : value.name());
    }

    String key(String field) {
        return anomalyId + FIELD_SEPARATOR + field;
    }
}
