package exiledsector.charts.atlas;

import java.util.Locale;

public record AtlasAnomalyRecord(int recordId, AtlasCell cell, String anomalyId, Source source, Chain chain) {

    static final String FIELD_SEPARATOR = ",";
    private static final int FIELDS = 3;
    private static final int FIELDS_WITH_CHAIN = 6;

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

    public record Chain(int stage, int stages, AtlasCell from) {

        public Chain {
            stage = Math.max(1, stage);
            stages = Math.max(stage, stages);
        }

        public boolean isLast() {
            return stage >= stages;
        }
    }

    public AtlasAnomalyRecord(int recordId, AtlasCell cell, String anomalyId, Source source) {
        this(recordId, cell, anomalyId, source, null);
    }

    String encode() {
        String encoded = cell.pack() + FIELD_SEPARATOR + anomalyId + FIELD_SEPARATOR + source.name();
        if (chain == null) {
            return encoded;
        }
        return encoded + FIELD_SEPARATOR + chain.stage() + FIELD_SEPARATOR + chain.stages() + FIELD_SEPARATOR
                + (chain.from() == null ? "" : Long.toString(chain.from().pack()));
    }

    static AtlasAnomalyRecord decode(int recordId, String text) {
        if (text == null) {
            return null;
        }
        String[] fields = text.split(FIELD_SEPARATOR, -1);
        if (fields.length != FIELDS && fields.length != FIELDS_WITH_CHAIN || fields[1].isBlank()) {
            return null;
        }
        Source source = Source.parse(fields[2]);
        try {
            Chain chain = fields.length == FIELDS_WITH_CHAIN ? new Chain(Integer.parseInt(fields[3]), Integer.parseInt(fields[4]),
                    fields[5].isBlank() ? null : AtlasCell.unpack(Long.parseLong(fields[5]))) : null;
            return source == null ? null : new AtlasAnomalyRecord(recordId, AtlasCell.unpack(Long.parseLong(fields[0])), fields[1], source, chain);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    AtlasAnomalyRecord movedTo(AtlasCell target) {
        return new AtlasAnomalyRecord(recordId, target, anomalyId, source, chain);
    }

    AtlasAnomalyRecord movedTo(AtlasCell target, Chain nextChain) {
        return new AtlasAnomalyRecord(recordId, target, anomalyId, source, nextChain);
    }
}
