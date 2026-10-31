package exiledsector.charts.anomaly.skirmish;

import exiledsector.data.EnumKeys;
import exiledsector.charts.atlas.AtlasRunChoice;

public enum SkirmishSide implements AtlasRunChoice {
    DEFENDER,
    ATTACKER,
    NEITHER;

    public String key() {
        return EnumKeys.key(this);
    }

    public static SkirmishSide parse(String text) {
        return EnumKeys.parse(SkirmishSide.class, text);
    }
}
