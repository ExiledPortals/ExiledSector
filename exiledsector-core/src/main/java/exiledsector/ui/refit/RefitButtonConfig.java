package exiledsector.ui.refit;

import exiledsector.ModSettings;
import org.lwjgl.input.Keyboard;

public final class RefitButtonConfig {

    public static final String FIELD_ID = "exiledSector_skillTreeButtonUnderHullMods";

    public static final boolean DEFAULT = true;

    public static final String BENCH_KEYBIND_FIELD_ID = "exiledSector_craftingBenchButtonKey";

    public static final int DEFAULT_BENCH_KEY = 0;

    public static final String SKILL_TREE_KEYBIND_FIELD_ID = "exiledSector_skillTreeButtonKey";

    public static final int DEFAULT_SKILL_TREE_KEY = Keyboard.KEY_K;

    public static final String HULL_ITEMS_KEYBIND_FIELD_ID = "exiledSector_hullItemsButtonKey";

    public static final int DEFAULT_HULL_ITEMS_KEY = Keyboard.KEY_L;

    private RefitButtonConfig() {
    }

    public static int skillTreeKey() {
        return ModSettings.intOr(SKILL_TREE_KEYBIND_FIELD_ID, DEFAULT_SKILL_TREE_KEY);
    }

    public static int hullItemsKey() {
        return ModSettings.intOr(HULL_ITEMS_KEYBIND_FIELD_ID, DEFAULT_HULL_ITEMS_KEY);
    }

    public static boolean buttonUnderHullMods() {
        return ModSettings.booleanOr(FIELD_ID, DEFAULT);
    }

    public static int benchKey() {
        return ModSettings.intOr(BENCH_KEYBIND_FIELD_ID, DEFAULT_BENCH_KEY);
    }
}
