package exiledsector.effects;

import exiledsector.ModSettings;

public final class ModuleShareSetting {

    public static final String FIELD_ID = "exiledSector_moduleTreeShare";
    public static final String STATION_FIELD_ID = "exiledSector_stationModuleTreeShare";
    public static final int DEFAULT_PERCENT = 50;
    public static final int DEFAULT_STATION_PERCENT = 100;
    public static final int MAX_PERCENT = 100;

    private ModuleShareSetting() {
    }

    public static int percent() {
        return clamped(ModSettings.intOr(FIELD_ID, DEFAULT_PERCENT));
    }

    public static int stationPercent() {
        return clamped(ModSettings.intOr(STATION_FIELD_ID, DEFAULT_STATION_PERCENT));
    }

    public static int percentFor(boolean station) {
        return station ? stationPercent() : percent();
    }

    public static float share() {
        return percent() / 100f;
    }

    public static float shareFor(boolean station) {
        return percentFor(station) / 100f;
    }

    private static int clamped(int sharePercent) {
        return Math.max(0, Math.min(MAX_PERCENT, sharePercent));
    }
}
