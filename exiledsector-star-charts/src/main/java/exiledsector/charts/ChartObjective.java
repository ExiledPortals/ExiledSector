package exiledsector.charts;

import java.util.Locale;

public enum ChartObjective {
    CLEAR,
    BOSS;

    public static ChartObjective parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String wanted = text.trim().toUpperCase(Locale.ROOT);
        for (ChartObjective objective : values()) {
            if (objective.name().equals(wanted)) {
                return objective;
            }
        }
        return null;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
