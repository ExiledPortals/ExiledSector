package exiledsector.charts.objective;

import exiledsector.data.EnumKeys;

public enum ChartObjective {
    CLEAR,
    BOSS;

    public static ChartObjective parse(String text) {
        return EnumKeys.parse(ChartObjective.class, text);
    }

    public String key() {
        return EnumKeys.key(this);
    }
}
