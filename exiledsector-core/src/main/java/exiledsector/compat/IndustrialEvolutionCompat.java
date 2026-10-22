package exiledsector.compat;

import exiledlib.ModSettings;

public final class IndustrialEvolutionCompat {

    public static final String MOD_ID = "IndEvo";
    static final String DERELICTS_FIELD_ID = "IndEvo_Enable_Derelicts";
    static final String DERELICT_STATIONS_FIELD_ID = "IndEvo_enable_derelict_stations";
    static final boolean DEFAULT_ENABLED = true;

    private IndustrialEvolutionCompat() {
    }

    public static boolean derelictStationsEnabled() {
        return ModSettings.otherModBooleanOr(MOD_ID, DERELICTS_FIELD_ID, DEFAULT_ENABLED)
                && ModSettings.otherModBooleanOr(MOD_ID, DERELICT_STATIONS_FIELD_ID, DEFAULT_ENABLED);
    }
}
