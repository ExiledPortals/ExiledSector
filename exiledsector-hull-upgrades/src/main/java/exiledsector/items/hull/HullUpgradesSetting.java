package exiledsector.items.hull;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import exiledsector.ModSettings;
import exiledsector.domain.DomainSwitch;
import exiledsector.items.drop.LevelGateSetting;
import exiledsector.items.schematic.MasterSchematicActivation;

import java.util.List;

public final class HullUpgradesSetting {

    public static final String ENABLED_FIELD_ID = "exiledSector_hullUpgradesEnabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final String UNLOCK_LEVEL_FIELD_ID = "exiledSector_hullUpgradesUnlockLevel";
    public static final int DEFAULT_UNLOCK_LEVEL = LevelGateSetting.DEFAULT_LEVEL;
    public static final LevelGateSetting UNLOCK_LEVEL = new LevelGateSetting(UNLOCK_LEVEL_FIELD_ID);
    public static final String POINTS_FIELD_ID = "exiledSector_hullUpgradePoints";
    public static final String REFIT_SCHEMATICS = "Refit Schematics";
    public static final String STAR_CHART_PROGRESS = "Star Chart Progress";
    public static final List<String> POINTS_OPTIONS = List.of(REFIT_SCHEMATICS, STAR_CHART_PROGRESS);
    public static final String MASTER_SCHEMATICS_FIELD_ID = "exiledSector_masterSchematicsRequired";
    public static final boolean DEFAULT_MASTER_SCHEMATICS_REQUIRED = false;

    public enum PointsMode {REFIT_SCHEMATICS, STAR_CHART_PROGRESS}

    public static final DomainSwitch SWITCH = new DomainSwitch(ENABLED_FIELD_ID, DEFAULT_ENABLED, MasterSchematicActivation::settingChanged);
    private static volatile PointsMode pointsModeSetting = PointsMode.REFIT_SCHEMATICS;
    private static volatile boolean masterSchematicsRequiredSetting = DEFAULT_MASTER_SCHEMATICS_REQUIRED;

    private HullUpgradesSetting() {
    }

    public static void reloadSettings() {
        UNLOCK_LEVEL.reload();
        PointsMode pointsMode = parsePointsMode(ModSettings.stringOr(POINTS_FIELD_ID, REFIT_SCHEMATICS));
        if (pointsMode != pointsModeSetting) {
            pointsModeSetting = pointsMode;
            HullUpgradePoints.markChanged();
        }
        boolean masterSchematicsRequired = ModSettings.booleanOr(MASTER_SCHEMATICS_FIELD_ID, DEFAULT_MASTER_SCHEMATICS_REQUIRED);
        boolean masterSchematicsChanged = masterSchematicsRequired != masterSchematicsRequiredSetting;
        masterSchematicsRequiredSetting = masterSchematicsRequired;
        SWITCH.reload();
        if (masterSchematicsChanged) {
            MasterSchematicActivation.settingChanged();
        }
    }

    public static boolean enabled() {
        return SWITCH.on();
    }

    public static boolean masterSchematicsRequired() {
        return enabled() && masterSchematicsRequiredSetting;
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
        return UNLOCK_LEVEL.level();
    }

    public static boolean hullItemDropsOpen(SectorAPI sector) {
        MutableCharacterStatsAPI playerStats = sector == null ? null : sector.getPlayerStats();
        return playerStats != null && hullItemDropsOpen(playerStats.getLevel());
    }

    public static boolean hullItemDropsOpen(int playerLevel) {
        return enabled() && playerLevel >= unlockLevel();
    }

    public static void setEnabledForTests(boolean enabled) {
        SWITCH.setForTests(enabled);
    }

    public static void setMasterSchematicsRequiredForTests(boolean masterSchematicsRequired) {
        masterSchematicsRequiredSetting = masterSchematicsRequired;
    }

    public static void setUnlockLevelForTests(int unlockLevel) {
        UNLOCK_LEVEL.setForTests(unlockLevel);
    }
}
