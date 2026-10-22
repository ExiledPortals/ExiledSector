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
        String fieldId = conditionType.overrideFieldId();
        if (fieldId == null) return false;

        return ModSettings.booleanOr(fieldId, DEFAULT_DISABLED);
    }
}
