package exiledsector.effects;

import exiledlib.ModSettings;

public final class ModuleShareSetting {

    public static final String FIELD_ID = "exiledSector_moduleTreeShare";
    public static final int DEFAULT_PERCENT = 50;
    public static final int MAX_PERCENT = 100;

    private ModuleShareSetting() {
    }

    public static int percent() {
        return Math.max(0, Math.min(MAX_PERCENT, ModSettings.intOr(FIELD_ID, DEFAULT_PERCENT)));
    }

    public static float share() {
        return percent() / 100f;
    }
}
