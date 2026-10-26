package exiledsector.charts.atlas;

import java.util.Locale;

public record AtlasAnomalyRecord(int recordId, AtlasCell cell, String anomalyId, Source source) {

    static final String FIELD_SEPARATOR = ",";
    private static final int FIELDS = 3;

    public enum Source {
        PLAYER,
        WORLD;

        static Source parse(String text) {
            for (Source source : values()) {
                if (source.name().equals(text.trim().toUpperCase(Locale.ROOT))) {
                    return source;
                }
            }
            return null;
        }
    }

    String encode() {
        return cell.pack() + FIELD_SEPARATOR + anomalyId + FIELD_SEPARATOR + source.name();
    }

    static AtlasAnomalyRecord decode(int recordId, String text) {
        if (text == null) {
            return null;
        }
        String[] fields = text.split(FIELD_SEPARATOR, -1);
        if (fields.length != FIELDS || fields[1].isBlank()) {
            return null;
        }
        Source source = Source.parse(fields[2]);
        try {
            return source == null ? null : new AtlasAnomalyRecord(recordId, AtlasCell.unpack(Long.parseLong(fields[0])), fields[1], source);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    AtlasAnomalyRecord movedTo(AtlasCell target) {
        return new AtlasAnomalyRecord(recordId, target, anomalyId, source);
    }
}
