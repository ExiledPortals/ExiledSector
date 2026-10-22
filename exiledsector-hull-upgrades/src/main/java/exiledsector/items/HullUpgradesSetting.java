package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.ModSettings;
import exiledsector.ModSettingsListener;

public final class HullUpgradesSetting {

    public static final String ENABLED_FIELD_ID = "exiledSector_hullUpgradesEnabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String UNLOCK_LEVEL_FIELD_ID = "exiledSector_hullUpgradesUnlockLevel";
    public static final int DEFAULT_UNLOCK_LEVEL = 15;
    public static final int MIN_UNLOCK_LEVEL = 1;
    public static final int MAX_UNLOCK_LEVEL = 15;

    private static volatile boolean enabledSetting = DEFAULT_ENABLED;
    private static volatile int unlockLevelSetting = DEFAULT_UNLOCK_LEVEL;

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
        unlockLevelSetting = clampLevel(ModSettings.intOr(UNLOCK_LEVEL_FIELD_ID, DEFAULT_UNLOCK_LEVEL));
    }

    public static boolean enabled() {
        return enabledSetting;
    }

    public static int unlockLevel() {
        return unlockLevelSetting;
    }

    public static boolean sectionsOpen(SectorAPI sector) {
        MutableCharacterStatsAPI playerStats = sector == null ? null : sector.getPlayerStats();
        return playerStats != null && sectionsOpen(playerStats.getLevel());
    }

    public static boolean sectionsOpen(int playerLevel) {
        return enabled() && playerLevel >= unlockLevel();
    }

    static int clampLevel(int level) {
        return Math.max(MIN_UNLOCK_LEVEL, Math.min(MAX_UNLOCK_LEVEL, level));
    }

    public static void setEnabledForTests(boolean enabled) {
        enabledSetting = enabled;
    }

    public static void setUnlockLevelForTests(int unlockLevel) {
        unlockLevelSetting = unlockLevel;
    }
}
