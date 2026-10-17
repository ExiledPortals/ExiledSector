package exiledsector.hullitems;

import exiledsector.ModSettings;
import exiledsector.ModSettingsListener;

public final class HullUpgradesSetting {

    public static final String ENABLED_FIELD_ID = "exiledSector_hullUpgradesEnabled";
    public static final boolean DEFAULT_ENABLED = true;

    private static volatile boolean enabledSetting = DEFAULT_ENABLED;

    public static final class SettingsListener extends ModSettingsListener {

        @Override
        protected void reload() {
            reloadSettings();
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
