package exiledsector.ui.inspect;

import exiledsector.ModSettings;
import org.lwjgl.input.Keyboard;

public final class NpcInspectConfig {

    public static final String KEYBIND_FIELD_ID = "exiledSector_inspectNpcTreesKey";
    public static final int DEFAULT_KEY = Keyboard.KEY_X;

    private NpcInspectConfig() {
    }

    public static int key() {
        return ModSettings.intOr(KEYBIND_FIELD_ID, DEFAULT_KEY);
    }
}
