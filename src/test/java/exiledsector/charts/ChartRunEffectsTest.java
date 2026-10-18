package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import exiledsector.items.ItemData;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartRunEffectsTest {

    private static final String RUN_ID = "sensors";

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private StarSystemAPI system;
    private LocationAPI hyperspace;
    private CampaignFleetAPI playerFleet;
    private MutableFleetStatsAPI stats;
    private StatBonus sensorRange;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        Map<String, Object> persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        system = mock(StarSystemAPI.class);
        hyperspace = mock(LocationAPI.class);
        playerFleet = mock(CampaignFleetAPI.class);
        stats = mock(MutableFleetStatsAPI.class);
        sensorRange = new StatBonus();
        when(playerFleet.getStats()).thenReturn(stats);
        when(stats.getSensorRangeMod()).thenReturn(sensorRange);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(sector.getHyperspace()).thenReturn(hyperspace);
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(system);
        when(sector.getStarSystems()).thenReturn(List.of(system));
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        StarChartFixtures.clear();
    }

    private ChartRun storeRun(StarChart chart) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        if (chart != null) {
            run.recordChart(ItemData.of(chart).encode());
        }
        ChartRuns.store(run);
        return run;
    }

    @Test
    void theRunReadsItsChartBackFromTheStoredData() {
        StarChart chart = StarChartFixtures.chart("unique_chart_silent_reach", 9L, 6);
        ChartRun run = storeRun(chart);

        StarChart restored = ChartRunEffects.chartOf(run);
        assertEquals(chart.definitionId(), restored.definitionId());
        assertEquals(chart.seed(), restored.seed());
        assertEquals(6, restored.tier());
        assertEquals(chart.explicits(), restored.explicits());
        assertEquals(0.5f, ChartRunEffects.contextOf(run).sensorRangeMult(), 0.001f);
        assertNull(ChartRunEffects.chartOf(new ChartRun("plain", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1)));
        assertTrue(ChartRunEffects.contextOf(new ChartRun("plain", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1)).isEmpty());
    }

    @Test
    void theSensorPenaltyHoldsOnlyWhileTheFleetIsInsideAndLiftsOnLeaving() {
        ChartRun run = storeRun(StarChartFixtures.chart("unique_chart_silent_reach", 9L, 6));
        run.recordArrival();
        ChartRunScript watchdog = new ChartRunScript();
        when(playerFleet.getContainingLocation()).thenReturn(system);

        watchdog.advance(0f);
        watchdog.advance(0f);

        verify(stats, atLeastOnce()).addTemporaryModMult(eq(ChartRunEffects.SENSOR_MOD_DAYS), eq(ChartIds.SENSOR_MOD_ID), anyString(), eq(0.5f),
                eq(sensorRange));
        verify(stats, never()).removeTemporaryMod(ChartIds.SENSOR_MOD_ID);

        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        when(playerFleet.isInHyperspaceTransition()).thenReturn(true);
        watchdog.advance(0f);

        verify(stats).removeTemporaryMod(ChartIds.SENSOR_MOD_ID);
    }

    @Test
    void aRunWithoutASensorModifierNeverTouchesTheFleetsSensors() {
        storeRun(null).recordArrival();
        ChartRunScript watchdog = new ChartRunScript();
        when(playerFleet.getContainingLocation()).thenReturn(system);

        watchdog.advance(0f);
        when(playerFleet.getContainingLocation()).thenReturn(hyperspace);
        watchdog.advance(0f);

        verify(stats, never()).addTemporaryModMult(anyFloat(), anyString(), anyString(), anyFloat(), Mockito.any(StatBonus.class));
        verify(stats, never()).removeTemporaryMod(anyString());
    }

    @Test
    void theRunEndingLiftsThePenaltyEvenIfTheWatchdogMissedTheExit() {
        ChartRun run = storeRun(StarChartFixtures.chart("unique_chart_silent_reach", 9L, 6));
        run.recordArrival();
        ChartRunScript watchdog = new ChartRunScript();
        when(playerFleet.getContainingLocation()).thenReturn(system);
        watchdog.advance(0f);

        ChartRuns.delete(RUN_ID);
        watchdog.advance(0f);

        verify(stats).removeTemporaryMod(ChartIds.SENSOR_MOD_ID);
    }

    @Test
    void closingARunAndLoadingASaveBothLiftThePenalty() {
        ChartRun run = storeRun(StarChartFixtures.chart("unique_chart_silent_reach", 9L, 6));
        sensorRange.modifyMult(ChartIds.SENSOR_MOD_ID, 0.5f);

        ChartRunCloser.close(sector, run.runId(), null);
        verify(stats).removeTemporaryMod(ChartIds.SENSOR_MOD_ID);
        assertNull(sensorRange.getMultBonus(ChartIds.SENSOR_MOD_ID));

        sensorRange.modifyMult(ChartIds.SENSOR_MOD_ID, 0.5f);
        ChartLoadSweep.liftEffectsOutsideSystem(sector);
        assertNull(sensorRange.getMultBonus(ChartIds.SENSOR_MOD_ID));
    }

    @Test
    void loadingASaveMadeInsideKeepsThePenaltyForTheWatchdogToRenew() {
        ChartRun run = storeRun(StarChartFixtures.chart("unique_chart_silent_reach", 9L, 6));
        run.recordArrival();
        when(playerFleet.getContainingLocation()).thenReturn(system);
        sensorRange.modifyMult(ChartIds.SENSOR_MOD_ID, 0.5f);

        ChartLoadSweep.liftEffectsOutsideSystem(sector);

        assertFalse(sensorRange.getMultBonus(ChartIds.SENSOR_MOD_ID) == null);
    }

    @Test
    void aClosingRunKeepsThePenaltyInsideForBothTheWatchdogAndTheLoadSweep() {
        ChartRun run = storeRun(StarChartFixtures.chart("unique_chart_silent_reach", 9L, 6));
        run.recordArrival();
        run.beginClosing();
        when(playerFleet.getContainingLocation()).thenReturn(system);
        sensorRange.modifyMult(ChartIds.SENSOR_MOD_ID, 0.5f);

        ChartLoadSweep.liftEffectsOutsideSystem(sector);
        assertFalse(sensorRange.getMultBonus(ChartIds.SENSOR_MOD_ID) == null, "the load sweep keeps the penalty of a closing run");
        new ChartRunScript().watch(sector, run);
        verify(stats).addTemporaryModMult(eq(ChartRunEffects.SENSOR_MOD_DAYS), eq(ChartIds.SENSOR_MOD_ID), anyString(), eq(0.5f), eq(sensorRange));
        verify(stats, never()).removeTemporaryMod(ChartIds.SENSOR_MOD_ID);

        assertTrue(ChartRunState.CLOSING.keepsRunEffects());
        assertTrue(ChartRunState.INSIDE.keepsRunEffects());
        assertFalse(ChartRunState.CLOSED.keepsRunEffects());
    }

    @Test
    void applyingAMultOfOneRemovesInsteadOfAdding() {
        assertFalse(ChartRunEffects.applySensorRange(playerFleet, 1f, "src"));
        verify(stats).removeTemporaryMod(ChartIds.SENSOR_MOD_ID);
        assertTrue(ChartRunEffects.applySensorRange(playerFleet, 0.6f, "src"));
        assertFalse(ChartRunEffects.applySensorRange(null, 0.6f, "src"));
    }
}
