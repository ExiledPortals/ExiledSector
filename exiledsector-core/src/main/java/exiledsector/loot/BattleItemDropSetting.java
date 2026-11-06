package exiledsector.loot;

import exiledsector.ModSettings;

public final class BattleItemDropSetting {

    public static final String FIELD_ID = "exiledSector_battleItemDropPercent";
    public static final int DEFAULT_PERCENT = 100;
    public static final int MAX_PERCENT = 1000;

    private BattleItemDropSetting() {
    }

    public static float multiplier() {
        return Math.max(0, ModSettings.intOr(FIELD_ID, DEFAULT_PERCENT)) / 100f;
    }
}
