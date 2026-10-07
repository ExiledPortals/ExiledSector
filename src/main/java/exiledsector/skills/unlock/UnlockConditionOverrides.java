package exiledsector.skills.unlock;

import exiledsector.ModSettings;

public final class UnlockConditionOverrides {

    public static final String DISABLE_BLUEPRINT_FIELD_ID = "exiledSector_disableBlueprintUnlock";
    public static final String DISABLE_CHARACTER_STAT_FIELD_ID = "exiledSector_disableCharacterStatUnlock";
    public static final String DISABLE_MIN_SHIP_LEVEL_FIELD_ID = "exiledSector_disableMinShipLevelUnlock";
    public static final String DISABLE_MEMORY_FLAG_FIELD_ID = "exiledSector_disableMemoryFlagUnlock";

    public static final boolean DEFAULT_DISABLED = false;

    private UnlockConditionOverrides() {
    }

    public static boolean isDisabled(UnlockConditionType conditionType) {
        String fieldId = fieldIdFor(conditionType);
        if (fieldId == null) return false;

        return ModSettings.booleanOr(fieldId, DEFAULT_DISABLED);
    }

    private static String fieldIdFor(UnlockConditionType conditionType) {
        return switch (conditionType) {
            case BLUEPRINT -> DISABLE_BLUEPRINT_FIELD_ID;
            case CHARACTER_STAT -> DISABLE_CHARACTER_STAT_FIELD_ID;
            case MIN_SHIP_LEVEL -> DISABLE_MIN_SHIP_LEVEL_FIELD_ID;
            case MEMORY_FLAG -> DISABLE_MEMORY_FLAG_FIELD_ID;
            default -> null;
        };
    }
}
