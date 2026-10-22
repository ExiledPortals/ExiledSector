package exiledsector.skills.npc;

import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcTreeConfigTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
    }

    @Test
    void fallsBackToTheDefaultsWhenNothingIsConfigured() {
        assertTrue(NpcTreeConfig.isEnabled());
        assertTrue(NpcTreeConfig.levelsOfficeredShips());
        assertTrue(NpcTreeConfig.levelsFlagship());
        assertEquals(0.3f, NpcTreeConfig.otherShipChance(), 0.0001f);
    }

    @Test
    void usesTheConfiguredSettings() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", NpcTreeConfig.ENABLED_FIELD_ID)).thenReturn(false);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", NpcTreeConfig.OFFICERED_SHIPS_FIELD_ID)).thenReturn(false);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", NpcTreeConfig.FLAGSHIP_FIELD_ID)).thenReturn(false);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", NpcTreeConfig.OTHER_SHIP_CHANCE_FIELD_ID)).thenReturn(75);

        assertFalse(NpcTreeConfig.isEnabled());
        assertFalse(NpcTreeConfig.levelsOfficeredShips());
        assertFalse(NpcTreeConfig.levelsFlagship());
        assertEquals(0.75f, NpcTreeConfig.otherShipChance(), 0.0001f);
    }

    @Test
    void theOtherShipChanceIsClampedToAValidProbability() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", NpcTreeConfig.OTHER_SHIP_CHANCE_FIELD_ID)).thenReturn(250);
        assertEquals(1f, NpcTreeConfig.otherShipChance(), 0.0001f);

        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", NpcTreeConfig.OTHER_SHIP_CHANCE_FIELD_ID)).thenReturn(-10);
        assertEquals(0f, NpcTreeConfig.otherShipChance(), 0.0001f);
    }
}
