package exiledsector.charts;

import exiledsector.ModSettings;
import exiledsector.ModSettingsListener;

public final class StarChartSettings {

    public static final String ENABLED_FIELD_ID = "exiledSector_starChartsEnabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String ENTRIES_FIELD_ID = "exiledSector_starChartEntries";
    public static final int DEFAULT_ENTRIES = 1;
    public static final int MIN_ENTRIES = 1;
    public static final int MAX_ENTRIES = 10;
    public static final String FLEET_LIMIT_FIELD_ID = "exiledSector_starChartFleetLimit";
    public static final int DEFAULT_FLEET_LIMIT = 7;
    public static final int MIN_FLEET_LIMIT = 1;
    public static final int MAX_FLEET_LIMIT = 12;
    public static final String OPEN_ANYWHERE_FIELD_ID = "exiledSector_starChartOpenAnywhereDev";
    public static final boolean DEFAULT_OPEN_ANYWHERE = false;

    private static volatile boolean enabledSetting = DEFAULT_ENABLED;

    public static final class SettingsListener extends ModSettingsListener {

        @Override
        protected void reload() {
            reloadSettings();
        }
    }

    private StarChartSettings() {
    }

    public static void reloadSettings() {
        enabledSetting = ModSettings.booleanOr(ENABLED_FIELD_ID, DEFAULT_ENABLED);
    }

    public static boolean enabled() {
        return enabledSetting;
    }

    public static void setEnabledForTests(boolean enabled) {
        enabledSetting = enabled;
    }

    public static int wormholeEntries() {
        int configured = ModSettings.intOr(ENTRIES_FIELD_ID, DEFAULT_ENTRIES);
        return Math.max(MIN_ENTRIES, Math.min(MAX_ENTRIES, configured));
    }

    public static int fleetLimit() {
        int configured;
        try {
            configured = ModSettings.intOr(FLEET_LIMIT_FIELD_ID, DEFAULT_FLEET_LIMIT);
        } catch (RuntimeException | LinkageError e) {
            configured = DEFAULT_FLEET_LIMIT;
        }
        return Math.max(MIN_FLEET_LIMIT, Math.min(MAX_FLEET_LIMIT, configured));
    }

    public static boolean openAnywhere() {
        return ModSettings.booleanOr(OPEN_ANYWHERE_FIELD_ID, DEFAULT_OPEN_ANYWHERE);
    }
}
