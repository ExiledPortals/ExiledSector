package exiledsector.skills.npc;

import exiledsector.ModSettings;
import exiledsector.skills.SkillTreeSetting;

import java.util.List;

public final class NpcTreeConfig {

    public static final String ENABLED_FIELD_ID = "exiledSector_npcTreesEnabled";
    public static final String OFFICERED_SHIPS_FIELD_ID = "exiledSector_npcTreesOfficeredShips";
    public static final String FLAGSHIP_FIELD_ID = "exiledSector_npcTreesFlagship";
    public static final String OTHER_SHIP_CHANCE_FIELD_ID = "exiledSector_npcTreesOtherShipChance";
    public static final String STATIONS_FIELD_ID = "exiledSector_npcTreesStations";
    public static final String LEVEL_MODE_FIELD_ID = "exiledSector_npcLevelMode";
    public static final String FIXED_MIN_PERCENT_FIELD_ID = "exiledSector_npcFixedMinPercent";
    public static final String RECOVERED_START_BLANK_FIELD_ID = "exiledSector_npcRecoveredStartBlank";

    public static final String YOUR_SHIPS = "Your Ships";
    public static final String FIXED_RANGE = "Fixed Range";
    public static final List<String> LEVEL_MODE_OPTIONS = List.of(YOUR_SHIPS, FIXED_RANGE);

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_OFFICERED_SHIPS = true;
    public static final boolean DEFAULT_FLAGSHIP = true;
    public static final int DEFAULT_OTHER_SHIP_CHANCE_PERCENT = 30;
    public static final boolean DEFAULT_STATIONS = true;
    public static final int DEFAULT_FIXED_MIN_PERCENT = 50;
    public static final boolean DEFAULT_RECOVERED_START_BLANK = false;

    private NpcTreeConfig() {
    }

    public static boolean isEnabled() {
        return SkillTreeSetting.enabled() && bool(ENABLED_FIELD_ID, DEFAULT_ENABLED);
    }

    public static boolean levelsOfficeredShips() {
        return bool(OFFICERED_SHIPS_FIELD_ID, DEFAULT_OFFICERED_SHIPS);
    }

    public static boolean levelsFlagship() {
        return bool(FLAGSHIP_FIELD_ID, DEFAULT_FLAGSHIP);
    }

    public static float otherShipChance() {
        int chancePercent = ModSettings.intOr(OTHER_SHIP_CHANCE_FIELD_ID, DEFAULT_OTHER_SHIP_CHANCE_PERCENT);
        return Math.max(0, Math.min(100, chancePercent)) / 100f;
    }

    public static boolean levelsStations() {
        return bool(STATIONS_FIELD_ID, DEFAULT_STATIONS);
    }

    public static boolean fixedLevels() {
        String mode = ModSettings.stringOr(LEVEL_MODE_FIELD_ID, YOUR_SHIPS);
        return mode != null && FIXED_RANGE.equalsIgnoreCase(mode.trim());
    }

    public static int fixedMinPercent() {
        return Math.max(0, Math.min(100, ModSettings.intOr(FIXED_MIN_PERCENT_FIELD_ID, DEFAULT_FIXED_MIN_PERCENT)));
    }

    public static boolean recoveredShipsStartBlank() {
        return bool(RECOVERED_START_BLANK_FIELD_ID, DEFAULT_RECOVERED_START_BLANK);
    }

    private static boolean bool(String fieldId, boolean defaultValue) {
        return ModSettings.booleanOr(fieldId, defaultValue);
    }
}
