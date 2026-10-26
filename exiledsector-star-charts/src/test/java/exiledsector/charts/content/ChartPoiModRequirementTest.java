package exiledsector.charts.content;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import exiledsector.charts.ChartTables;
import exiledsector.charts.ChartTier;
import exiledsector.charts.ChartTiers;
import exiledsector.charts.content.ChartContentPlan.PlannedPoi;
import exiledsector.compat.IndustrialEvolutionCompat;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartPoiModRequirementTest {

    private static final String ARSENAL_ID = "arsenal_station";
    private static final String DERELICTS_FIELD_ID = "IndEvo_Enable_Derelicts";
    private static final String DERELICT_STATIONS_FIELD_ID = "IndEvo_enable_derelict_stations";
    private static final int HIGHEST_TIER = ChartTiers.HIGHEST_TIER;

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaMock;
    private final ModManagerAPI modManager = mock(ModManagerAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadShipped();
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        lunaMock = Mockito.mockStatic(LunaSettings.class);
    }

    @AfterEach
    void tearDown() {
        lunaMock.close();
        globalMock.close();
        ChartTiers.resetForTests();
        ChartPois.resetForTests();
        ChartFactions.resetForTests();
    }

    private void configure(boolean indEvoEnabled, Boolean derelicts, Boolean derelictStations) {
        when(modManager.isModEnabled(IndustrialEvolutionCompat.MOD_ID)).thenReturn(indEvoEnabled);
        lunaMock.when(() -> LunaSettings.getBoolean(IndustrialEvolutionCompat.MOD_ID, DERELICTS_FIELD_ID)).thenReturn(derelicts);
        lunaMock.when(() -> LunaSettings.getBoolean(IndustrialEvolutionCompat.MOD_ID, DERELICT_STATIONS_FIELD_ID)).thenReturn(derelictStations);
    }

    private static ChartPoi arsenal() {
        return ChartPois.all().stream().filter(poi -> poi.id().equals(ARSENAL_ID)).findFirst().orElseThrow();
    }

    private static boolean eligible() {
        return ChartPois.lootAt(ChartPois.all(), HIGHEST_TIER).contains(arsenal());
    }

    private static boolean plannedInAnyOf(int seeds) {
        ChartTier stationsOnly = new ChartTier(HIGHEST_TIER, 100f, 2, 2, 0, 0, 50f, 100, 100, 0f, 0f, 70f, 0f, false, 0f, 1f, 0f);
        for (long seed = 0; seed < seeds; seed++) {
            List<PlannedPoi> planned = ChartContentPlanner.spendLoot(stationsOnly, ChartPois.all(), new Random(seed));
            if (planned.stream().anyMatch(poi -> poi.poi().id().equals(ARSENAL_ID))) {
                return true;
            }
        }
        return false;
    }

    @Test
    void theArsenalRowNeedsIndustrialEvolution() {
        ChartPoi arsenal = arsenal();

        assertEquals(IndustrialEvolutionCompat.MOD_ID, arsenal.requiredMod());
        assertEquals("IndEvo_arsenalStation", arsenal.entityType());
        assertEquals("IndEvo_arsenalStation", arsenal.specForSalvage());
        assertEquals(ChartPoi.Kind.STATION, arsenal.kind());
        assertEquals(5, arsenal.minTier());
        assertTrue(ChartPois.all().stream().filter(poi -> !poi.id().equals(ARSENAL_ID)).allMatch(poi -> poi.requiredMod().isEmpty()));
    }

    @Test
    void theArsenalIsNeverPlacedWithoutIndustrialEvolution() {
        configure(false, true, true);

        assertFalse(eligible());
        assertFalse(plannedInAnyOf(200));
    }

    @Test
    void theArsenalIsNeverPlacedWhenIndustrialEvolutionTurnsDerelictsOff() {
        configure(true, false, true);

        assertFalse(eligible());
        assertFalse(plannedInAnyOf(200));
    }

    @Test
    void theArsenalIsNeverPlacedWhenIndustrialEvolutionTurnsDerelictStationsOff() {
        configure(true, true, false);

        assertFalse(eligible());
        assertFalse(plannedInAnyOf(200));
    }

    @Test
    void theArsenalIsPlacedWhenBothSettingsAreOn() {
        configure(true, true, true);

        assertTrue(eligible());
        assertTrue(plannedInAnyOf(200));
    }

    @Test
    void unsetSettingsCountAsOn() {
        configure(true, null, null);

        assertTrue(eligible());
        assertTrue(plannedInAnyOf(200));
    }

    @Test
    void rowsWithoutARequiredModIgnoreTheModManager() {
        configure(false, false, false);

        List<ChartPoi> eligible = ChartPois.lootAt(ChartPois.all(), HIGHEST_TIER);
        assertEquals(ChartPois.all().stream().filter(poi -> poi.requiredMod().isEmpty() && poi.allowedAt(HIGHEST_TIER)).count(), eligible.size());
    }

    @Test
    void theArsenalIsSkippedWhenTheGameSettingsAreMissing() {
        globalMock.when(Global::getSettings).thenReturn(null);

        assertFalse(eligible());
    }
}
