package exiledsector.ui.refit;

import exiledlib.ModSettings;

public final class RefitButtonConfig {

    public static final String FIELD_ID = "exiledSector_skillTreeButtonUnderHullMods";

    public static final boolean DEFAULT = true;

    public static final String BENCH_KEYBIND_FIELD_ID = "exiledSector_modificationBenchButtonKey";

    public static final int DEFAULT_BENCH_KEY = 0;

    private RefitButtonConfig() {
    }

    public static boolean buttonUnderHullMods() {
        return ModSettings.booleanOr(FIELD_ID, DEFAULT);
    }

    public static int benchKey() {
        return ModSettings.intOr(BENCH_KEYBIND_FIELD_ID, DEFAULT_BENCH_KEY);
    }
}
