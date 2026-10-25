package exiledsector.charts;

import java.util.Locale;

public enum ChartBossType {
    FLEET,
    STATION;

    public static ChartBossType parse(String text) {
        if (text == null || text.isBlank()) {
            return FLEET;
        }
        String wanted = text.trim().toUpperCase(Locale.ROOT);
        for (ChartBossType type : values()) {
            if (type.name().equals(wanted)) {
                return type;
            }
        }
        return FLEET;
    }

    public static ChartBossType byKey(String key) {
        for (ChartBossType type : values()) {
            if (type.key().equals(key)) {
                return type;
            }
        }
        return null;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
