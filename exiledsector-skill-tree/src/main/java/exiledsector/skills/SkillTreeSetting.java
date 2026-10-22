package exiledsector.skills;

import exiledlib.ModSettings;
import exiledlib.ModSettingsListener;
import exiledsector.effects.ShipTreeSync;

public final class SkillTreeSetting {

    public static final String ENABLED_FIELD_ID = "exiledSector_skillTreeEnabled";
    public static final boolean DEFAULT_ENABLED = true;

    private static volatile boolean enabledSetting = DEFAULT_ENABLED;

    public static final class SettingsListener extends ModSettingsListener {

        @Override
        protected void reload() {
            reloadSettings();
        }
    }

    private SkillTreeSetting() {
    }

    public static void reloadSettings() {
        boolean enabled = ModSettings.booleanOr(ENABLED_FIELD_ID, DEFAULT_ENABLED);
        boolean switched = enabled != enabledSetting;
        enabledSetting = enabled;
        if (switched) {
            ShipTreeSync.treeSwitched();
        }
    }

    public static boolean enabled() {
        return enabledSetting;
    }

    public static void setEnabledForTests(boolean enabled) {
        enabledSetting = enabled;
    }
}
