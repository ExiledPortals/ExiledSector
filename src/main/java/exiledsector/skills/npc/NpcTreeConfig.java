package exiledsector.skills.npc;

import exiledsector.ModSettings;

public final class NpcTreeConfig {

    public static final String ENABLED_FIELD_ID = "exiledSector_npcTreesEnabled";
    public static final String OFFICERED_SHIPS_FIELD_ID = "exiledSector_npcTreesOfficeredShips";
    public static final String FLAGSHIP_FIELD_ID = "exiledSector_npcTreesFlagship";
    public static final String OTHER_SHIP_CHANCE_FIELD_ID = "exiledSector_npcTreesOtherShipChance";

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_OFFICERED_SHIPS = true;
    public static final boolean DEFAULT_FLAGSHIP = true;
    public static final int DEFAULT_OTHER_SHIP_CHANCE_PERCENT = 30;

    private NpcTreeConfig() {
    }

    public static boolean isEnabled() {
        return bool(ENABLED_FIELD_ID, DEFAULT_ENABLED);
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

    private static boolean bool(String fieldId, boolean defaultValue) {
        return ModSettings.booleanOr(fieldId, defaultValue);
    }
}
