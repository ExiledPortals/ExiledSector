package exiledsector.charts.content;

import exiledsector.data.EnumKeys;

public enum ChartBossType {
    FLEET,
    STATION;

    public static ChartBossType parse(String text) {
        return EnumKeys.parseOr(ChartBossType.class, text, FLEET);
    }

    public static ChartBossType byKey(String key) {
        return EnumKeys.parse(ChartBossType.class, key);
    }

    public String key() {
        return EnumKeys.key(this);
    }
}
