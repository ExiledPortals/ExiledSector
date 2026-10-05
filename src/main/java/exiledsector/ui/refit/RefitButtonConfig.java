package exiledsector.ui.refit;

import exiledsector.ModSettings;

public final class RefitButtonConfig {

    public static final String FIELD_ID = "exiledSector_skillTreeButtonUnderHullMods";

    public static final boolean DEFAULT = true;

    private RefitButtonConfig() {
    }

    public static boolean buttonUnderHullMods() {
        return ModSettings.booleanOr(FIELD_ID, DEFAULT);
    }
}
