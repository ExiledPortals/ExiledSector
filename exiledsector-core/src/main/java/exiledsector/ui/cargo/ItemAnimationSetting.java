package exiledsector.ui.cargo;

import exiledlib.ModSettings;
import exiledlib.ModSettingsListener;

import java.util.List;

public final class ItemAnimationSetting {

    public static final String FIELD_ID = "exiledSector_itemAnimations";
    public static final String FULL = "Full";
    public static final String CALM = "Calm";
    public static final String OFF = "Off";
    public static final List<String> OPTIONS = List.of(FULL, CALM, OFF);

    public enum Mode {FULL, CALM, OFF}

    private static volatile Mode mode = Mode.FULL;

    public static final class SettingsListener extends ModSettingsListener {

        @Override
        protected void reload() {
            reloadSettings();
        }
    }

    private ItemAnimationSetting() {
    }

    public static void reloadSettings() {
        mode = parse(ModSettings.stringOr(FIELD_ID, FULL));
    }

    static Mode parse(String value) {
        if (CALM.equalsIgnoreCase(value)) {
            return Mode.CALM;
        }
        return OFF.equalsIgnoreCase(value) ? Mode.OFF : Mode.FULL;
    }

    public static Mode mode() {
        return mode;
    }

    public static void setModeForTests(Mode testMode) {
        mode = testMode;
    }
}
