package exiledsector.skills.unlock;

import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnlockConditionOverridesTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
    }

    @Test
    void defaultsToNotDisabledWhenNoSettingIsConfigured() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableBlueprintUnlock")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableCharacterStatUnlock")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableMinShipLevelUnlock")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableMemoryFlagUnlock")).thenReturn(null);

        assertFalse(UnlockConditionOverrides.isDisabled(UnlockConditionType.BLUEPRINT));
        assertFalse(UnlockConditionOverrides.isDisabled(UnlockConditionType.CHARACTER_STAT));
        assertFalse(UnlockConditionOverrides.isDisabled(UnlockConditionType.MIN_SHIP_LEVEL));
        assertFalse(UnlockConditionOverrides.isDisabled(UnlockConditionType.MEMORY_FLAG));
    }

    @Test
    void readsEachConditionTypesOwnSettingIndependently() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableBlueprintUnlock")).thenReturn(true);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableCharacterStatUnlock")).thenReturn(false);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableMinShipLevelUnlock")).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_disableMemoryFlagUnlock")).thenReturn(true);

        assertTrue(UnlockConditionOverrides.isDisabled(UnlockConditionType.BLUEPRINT));
        assertFalse(UnlockConditionOverrides.isDisabled(UnlockConditionType.CHARACTER_STAT));
        assertFalse(UnlockConditionOverrides.isDisabled(UnlockConditionType.MIN_SHIP_LEVEL));
        assertTrue(UnlockConditionOverrides.isDisabled(UnlockConditionType.MEMORY_FLAG));
    }
}
