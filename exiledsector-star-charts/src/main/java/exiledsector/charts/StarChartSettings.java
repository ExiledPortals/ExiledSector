package exiledsector.charts;

import exiledlib.ModSettings;
import exiledsector.domain.DomainSwitch;

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
    public static final String DEBUG_TOOLS_FIELD_ID = "exiledSector_starChartDebugTools";
    public static final boolean DEFAULT_DEBUG_TOOLS = false;

    public static final DomainSwitch SWITCH = new DomainSwitch(ENABLED_FIELD_ID, DEFAULT_ENABLED, () -> {
    });

    private StarChartSettings() {
    }

    public static void reloadSettings() {
        SWITCH.reload();
    }

    public static boolean enabled() {
        return SWITCH.on();
    }

    public static void setEnabledForTests(boolean enabled) {
        SWITCH.setForTests(enabled);
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

    public static boolean debugTools() {
        return ModSettings.booleanOr(DEBUG_TOOLS_FIELD_ID, DEFAULT_DEBUG_TOOLS);
    }
}
