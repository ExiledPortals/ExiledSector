package exiledsector.socketables;

import exiledsector.ExiledSectorModPlugin;
import exiledsector.ModSettings;
import lunalib.lunaSettings.LunaSettingsListener;

public final class HullUpgradesSetting {

    public static final String ENABLED_FIELD_ID = "exiledSector_hullUpgradesEnabled";
    public static final boolean DEFAULT_ENABLED = true;

    private static volatile boolean enabledSetting = DEFAULT_ENABLED;

    public static final class SettingsListener implements LunaSettingsListener {

        @Override
        public void settingsChanged(String modId) {
            if (ExiledSectorModPlugin.MOD_ID.equals(modId)) {
                reloadSettings();
            }
        }
    }

    private HullUpgradesSetting() {
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
}
