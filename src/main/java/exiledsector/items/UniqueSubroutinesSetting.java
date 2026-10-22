package exiledsector.items;

import exiledsector.ModSettings;
import exiledsector.ModSettingsListener;

public final class UniqueSubroutinesSetting {

    public static final String UNLOCK_LEVEL_FIELD_ID = "exiledSector_uniqueSubroutinesUnlockLevel";
    public static final int DEFAULT_UNLOCK_LEVEL = 15;

    private static volatile int unlockLevelSetting = DEFAULT_UNLOCK_LEVEL;

    public static final class SettingsListener extends ModSettingsListener {

        @Override
        protected void reload() {
            reloadSettings();
        }
    }

    private UniqueSubroutinesSetting() {
    }

    public static void reloadSettings() {
        unlockLevelSetting = HullUpgradesSetting.clampLevel(ModSettings.intOr(UNLOCK_LEVEL_FIELD_ID, DEFAULT_UNLOCK_LEVEL));
    }

    public static int unlockLevel() {
        return unlockLevelSetting;
    }

    public static void setUnlockLevelForTests(int unlockLevel) {
        unlockLevelSetting = unlockLevel;
    }
}
