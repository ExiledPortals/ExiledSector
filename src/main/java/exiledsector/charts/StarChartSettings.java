package exiledsector.charts;

import exiledsector.ModSettings;

public final class StarChartSettings {

    public static final String ENTRIES_FIELD_ID = "exiledSector_starChartEntries";
    public static final int DEFAULT_ENTRIES = 1;
    public static final int MIN_ENTRIES = 1;
    public static final int MAX_ENTRIES = 10;
    public static final String OPEN_ANYWHERE_FIELD_ID = "exiledSector_starChartOpenAnywhere";
    public static final boolean DEFAULT_OPEN_ANYWHERE = true;

    private StarChartSettings() {
    }

    public static int wormholeEntries() {
        int configured = ModSettings.intOr(ENTRIES_FIELD_ID, DEFAULT_ENTRIES);
        return Math.max(MIN_ENTRIES, Math.min(MAX_ENTRIES, configured));
    }

    public static boolean openAnywhere() {
        return ModSettings.booleanOr(OPEN_ANYWHERE_FIELD_ID, DEFAULT_OPEN_ANYWHERE);
    }
}
