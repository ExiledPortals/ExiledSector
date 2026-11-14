package exiledsector.items;

public final class UniqueSubroutinesSetting {

    public static final String UNLOCK_LEVEL_FIELD_ID = "exiledSector_uniqueSubroutinesUnlockLevel";
    public static final int DEFAULT_UNLOCK_LEVEL = LevelGateSetting.DEFAULT_LEVEL;
    public static final LevelGateSetting UNLOCK_LEVEL = new LevelGateSetting(UNLOCK_LEVEL_FIELD_ID);

    private UniqueSubroutinesSetting() {
    }

    public static void reloadSettings() {
        UNLOCK_LEVEL.reload();
    }

    public static int unlockLevel() {
        return UNLOCK_LEVEL.level();
    }

    public static void setUnlockLevelForTests(int unlockLevel) {
        UNLOCK_LEVEL.setForTests(unlockLevel);
    }
}
