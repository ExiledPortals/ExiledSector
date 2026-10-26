package exiledsector.charts.anomaly.skirmish;

import java.util.Locale;

public enum SkirmishSide {
    DEFENDER,
    ATTACKER,
    NEITHER;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SkirmishSide parse(String text) {
        if (text == null) {
            return null;
        }
        for (SkirmishSide side : values()) {
            if (side.key().equals(text.trim().toLowerCase(Locale.ROOT))) {
                return side;
            }
        }
        return null;
    }
}
