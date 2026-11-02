package exiledsector.charts;

import exiledsector.items.StarChart;
import exiledsector.modifiers.TierRange;
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

    private void configure(StarChartSettings.WormholeEntries setting, Integer entries) {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", setting.fieldId())).thenReturn(entries);
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
    void eachTierGroupHasItsOwnEntriesDefaultingToSixThreeAndOne() {
        StarChartSettings.ENTRIES.forEach(setting -> configure(setting, null));

        assertEquals(6, StarChartSettings.wormholeEntries(1));
        assertEquals(6, StarChartSettings.wormholeEntries(5));
        assertEquals(3, StarChartSettings.wormholeEntries(6));
        assertEquals(3, StarChartSettings.wormholeEntries(10));
        assertEquals(1, StarChartSettings.wormholeEntries(11));
        assertEquals(1, StarChartSettings.wormholeEntries(StarChart.CORRUPTED_TIER));
        assertEquals(6, StarChartSettings.wormholeEntries(TierRange.NO_TIER), "a tierless open counts as the lowest tier");
    }

    @Test
    void readsTheConfiguredEntriesOfTheChartsGroup() {
        StarChartSettings.ENTRIES.forEach(setting -> configure(setting, null));
        configure(StarChartSettings.entriesFor(7), 4);

        assertEquals(4, StarChartSettings.wormholeEntries(7));
        assertEquals(6, StarChartSettings.wormholeEntries(2));
    }

    @Test
    void keepsTheEntriesInsideTheSettingsRange() {
        StarChartSettings.WormholeEntries high = StarChartSettings.entriesFor(12);
        configure(high, 0);
        assertEquals(StarChartSettings.MIN_ENTRIES, StarChartSettings.wormholeEntries(12));

        configure(high, 99);
        assertEquals(StarChartSettings.MAX_ENTRIES, StarChartSettings.wormholeEntries(12));
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
    void showsTheDebugToolsOnlyWhenTheirSwitchIsOn() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.DEBUG_TOOLS_FIELD_ID)).thenReturn(null);
        assertFalse(StarChartSettings.debugTools());

        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", StarChartSettings.DEBUG_TOOLS_FIELD_ID)).thenReturn(true);
        assertTrue(StarChartSettings.debugTools());
    }
}
