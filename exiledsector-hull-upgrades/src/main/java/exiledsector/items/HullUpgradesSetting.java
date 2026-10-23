package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledlib.ModSettings;
import exiledlib.ModSettingsListener;
import exiledsector.effects.PlayerFleetStats;

import java.util.List;

public final class HullUpgradesSetting {

    public static final String ENABLED_FIELD_ID = "exiledSector_hullUpgradesEnabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String UNLOCK_LEVEL_FIELD_ID = "exiledSector_hullUpgradesUnlockLevel";
    public static final int DEFAULT_UNLOCK_LEVEL = 15;
    public static final int MIN_UNLOCK_LEVEL = 1;
    public static final int MAX_UNLOCK_LEVEL = 15;
    public static final String POINTS_FIELD_ID = "exiledSector_hullUpgradePoints";
    public static final String REFIT_SCHEMATICS = "Refit Schematics";
    public static final String STAR_CHART_PROGRESS = "Star Chart Progress";
    public static final List<String> POINTS_OPTIONS = List.of(REFIT_SCHEMATICS, STAR_CHART_PROGRESS);

    public enum PointsMode {REFIT_SCHEMATICS, STAR_CHART_PROGRESS}

    private static volatile boolean enabledSetting = DEFAULT_ENABLED;
    private static volatile int unlockLevelSetting = DEFAULT_UNLOCK_LEVEL;
    private static volatile PointsMode pointsModeSetting = PointsMode.REFIT_SCHEMATICS;

    public static final class SettingsListener extends ModSettingsListener {

        @Override
        protected void reload() {
            reloadSettings();
        }
    }

    private HullUpgradesSetting() {
    }

    public static void reloadSettings() {
        boolean enabled = ModSettings.booleanOr(ENABLED_FIELD_ID, DEFAULT_ENABLED);
        boolean switched = enabled != enabledSetting;
        enabledSetting = enabled;
        unlockLevelSetting = clampLevel(ModSettings.intOr(UNLOCK_LEVEL_FIELD_ID, DEFAULT_UNLOCK_LEVEL));
        PointsMode pointsMode = parsePointsMode(ModSettings.stringOr(POINTS_FIELD_ID, REFIT_SCHEMATICS));
        if (pointsMode != pointsModeSetting) {
            pointsModeSetting = pointsMode;
            HullUpgradePoints.markChanged();
        }
        if (switched) {
            PlayerFleetStats.markForStatUpdate();
        }
    }

    public static boolean enabled() {
        return enabledSetting;
    }

    public static PointsMode pointsMode() {
        return pointsModeSetting;
    }

    static PointsMode parsePointsMode(String value) {
        return STAR_CHART_PROGRESS.equalsIgnoreCase(value == null ? null : value.trim()) ? PointsMode.STAR_CHART_PROGRESS
                : PointsMode.REFIT_SCHEMATICS;
    }

    public static void setPointsModeForTests(PointsMode pointsMode) {
        pointsModeSetting = pointsMode;
        HullUpgradePoints.markChanged();
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
