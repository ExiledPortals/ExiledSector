package exiledsector.charts;

import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarChartSettingsTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
    }

    private void configure(Integer entries) {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", StarChartSettings.ENTRIES_FIELD_ID)).thenReturn(entries);
    }

    @Test
    void starChartsAreOnUnlessSwitchedOff() {
        try {
            lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.ENABLED_FIELD_ID)).thenReturn(null);
            StarChartSettings.reloadSettings();
            assertTrue(StarChartSettings.enabled());

            lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.ENABLED_FIELD_ID)).thenReturn(false);
            StarChartSettings.reloadSettings();
            assertFalse(StarChartSettings.enabled());
        } finally {
            StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        }
    }

    @Test
    void defaultsToOneEntry() {
        configure(null);

        assertEquals(1, StarChartSettings.DEFAULT_ENTRIES);
        assertEquals(1, StarChartSettings.wormholeEntries());
    }

    @Test
    void readsTheConfiguredEntries() {
        configure(4);

        assertEquals(4, StarChartSettings.wormholeEntries());
    }

    @Test
    void keepsTheEntriesInsideTheSettingsRange() {
        configure(0);
        assertEquals(StarChartSettings.MIN_ENTRIES, StarChartSettings.wormholeEntries());

        configure(99);
        assertEquals(StarChartSettings.MAX_ENTRIES, StarChartSettings.wormholeEntries());
    }

    @Test
    void readsTheFleetLimitWithinItsRangeAndDefaultsToSeven() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", StarChartSettings.FLEET_LIMIT_FIELD_ID)).thenReturn(null);
        assertEquals(7, StarChartSettings.fleetLimit());

        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", StarChartSettings.FLEET_LIMIT_FIELD_ID)).thenReturn(4);
        assertEquals(4, StarChartSettings.fleetLimit());

        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", StarChartSettings.FLEET_LIMIT_FIELD_ID)).thenReturn(0);
        assertEquals(StarChartSettings.MIN_FLEET_LIMIT, StarChartSettings.fleetLimit());

        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", StarChartSettings.FLEET_LIMIT_FIELD_ID)).thenReturn(99);
        assertEquals(StarChartSettings.MAX_FLEET_LIMIT, StarChartSettings.fleetLimit());
    }

    @Test
    void opensWormholesAnywhereOnlyWhenTheDevSwitchIsOn() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.OPEN_ANYWHERE_FIELD_ID)).thenReturn(null);
        assertFalse(StarChartSettings.openAnywhere());

        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.OPEN_ANYWHERE_FIELD_ID)).thenReturn(true);
        assertTrue(StarChartSettings.openAnywhere());
    }
}
