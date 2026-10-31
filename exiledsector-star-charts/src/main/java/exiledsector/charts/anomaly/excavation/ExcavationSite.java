package exiledsector.charts.anomaly.excavation;

import exiledsector.charts.loot.DropType;

import java.util.Locale;

public record ExcavationSite(int index, String planetId, Access access, DropType lean, long seed, boolean cleared) {

    static final String SEPARATOR = ",";
    static final int SEED_RADIX = Character.MAX_RADIX;
    static final String CLEARED_MARK = "cleared";
    static final String STANDING_MARK = "standing";

    public enum Access {
        OPEN,
        COLLAPSED;

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Access parse(String text) {
            for (Access access : values()) {
                if (access.key().equals(text)) {
                    return access;
                }
            }
            return null;
        }
    }

    public ExcavationSite asCleared() {
        return new ExcavationSite(index, planetId, access, lean, seed, true);
    }

    String encode() {
        return String.join(SEPARATOR, planetId, access.key(), lean.name(), Long.toString(seed, SEED_RADIX), cleared ? CLEARED_MARK : STANDING_MARK);
    }

    static boolean encodable(String planetId) {
        return planetId != null && !planetId.isEmpty() && !planetId.contains(SEPARATOR) && !planetId.contains(";") && !planetId.contains("=");
    }

    static ExcavationSite decode(int index, String encoded) {
        String[] parts = encoded == null ? new String[0] : encoded.split(SEPARATOR, -1);
        if (parts.length != 5) {
            return null;
        }
        Access access = Access.parse(parts[1]);
        DropType lean = parseLean(parts[2]);
        if (parts[0].isEmpty() || access == null || lean == null) {
            return null;
        }
        try {
            return new ExcavationSite(index, parts[0], access, lean, Long.parseLong(parts[3], SEED_RADIX), CLEARED_MARK.equals(parts[4]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static DropType parseLean(String text) {
        try {
            return DropType.valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
