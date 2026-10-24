package exiledsector.items;

import exiledlib.ModSettings;

public final class UniqueSubroutinesSetting {

    public static final String UNLOCK_LEVEL_FIELD_ID = "exiledSector_uniqueSubroutinesUnlockLevel";
    public static final int DEFAULT_UNLOCK_LEVEL = 15;
    public static final int MIN_UNLOCK_LEVEL = 1;
    public static final int MAX_UNLOCK_LEVEL = 15;

    private static volatile int unlockLevelSetting = DEFAULT_UNLOCK_LEVEL;

    private UniqueSubroutinesSetting() {
    }

    public static void reloadSettings() {
        int configured = ModSettings.intOr(UNLOCK_LEVEL_FIELD_ID, DEFAULT_UNLOCK_LEVEL);
        unlockLevelSetting = Math.max(MIN_UNLOCK_LEVEL, Math.min(MAX_UNLOCK_LEVEL, configured));
    }

    public static int unlockLevel() {
        return unlockLevelSetting;
    }

    public static void setUnlockLevelForTests(int unlockLevel) {
        unlockLevelSetting = unlockLevel;
    }
}
