package exiledsector.items;

import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HullUpgradesSettingTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
        HullUpgradesSetting.setEnabledForTests(HullUpgradesSetting.DEFAULT_ENABLED);
        HullUpgradesSetting.setUnlockLevelForTests(HullUpgradesSetting.DEFAULT_UNLOCK_LEVEL);
        UniqueSubroutinesSetting.setUnlockLevelForTests(UniqueSubroutinesSetting.DEFAULT_UNLOCK_LEVEL);
    }

    private void configure(Integer hullUpgradesLevel, Integer uniqueSubroutinesLevel) {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", HullUpgradesSetting.ENABLED_FIELD_ID)).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", HullUpgradesSetting.UNLOCK_LEVEL_FIELD_ID)).thenReturn(hullUpgradesLevel);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", UniqueSubroutinesSetting.UNLOCK_LEVEL_FIELD_ID))
                .thenReturn(uniqueSubroutinesLevel);
        HullUpgradesSetting.reloadSettings();
        UniqueSubroutinesSetting.reloadSettings();
    }

    @Test
    void bothUnlockLevelsDefaultToFifteen() {
        configure(null, null);

        assertEquals(15, HullUpgradesSetting.unlockLevel());
        assertEquals(15, UniqueSubroutinesSetting.unlockLevel());
    }

    @Test
    void readsEachLevelOnItsOwn() {
        configure(3, 9);

        assertEquals(3, HullUpgradesSetting.unlockLevel());
        assertEquals(9, UniqueSubroutinesSetting.unlockLevel());
    }

    @Test
    void keepsTheLevelsInsideThePlayerLevelRange() {
        configure(0, 99);

        assertEquals(HullUpgradesSetting.MIN_UNLOCK_LEVEL, HullUpgradesSetting.unlockLevel());
        assertEquals(UniqueSubroutinesSetting.MAX_UNLOCK_LEVEL, UniqueSubroutinesSetting.unlockLevel());
    }
}
