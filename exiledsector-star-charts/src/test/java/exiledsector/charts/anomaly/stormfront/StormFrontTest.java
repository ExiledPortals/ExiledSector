package exiledsector.charts.anomaly.stormfront;

import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.StarAnomalies;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StormFrontTest {

    private MockSector mockSector;
    private StarSystemAPI system;
    private float elapsedDays;
    private final List<CampaignTerrainAPI> terrain = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        CampaignClockAPI clock = mock(CampaignClockAPI.class);
        when(clock.getTimestamp()).thenReturn(100L);
        when(clock.getElapsedDaysSince(anyLong())).thenAnswer(invocation -> elapsedDays);
        when(mockSector.sector.getClock()).thenReturn(clock);
        system = mock(StarSystemAPI.class);
        when(system.getTerrainCopy()).thenReturn(terrain);
    }

    @AfterEach
    void tearDown() {
        ChartTables.resetAnomalies();
        mockSector.close();
    }

    private PlanetAPI planet(String id, float x, float y) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getId()).thenReturn(id);
        when(planet.getName()).thenReturn(id);
        when(planet.getLocation()).thenReturn(new Vector2f(x, y));
        when(planet.getRadius()).thenReturn(100f);
        return planet;
    }

    private StormFrontTerrainPlugin front(StormCourse path) {
        return front(new StormFrontTerrainPlugin.Params("run1", path, List.of("inner"), 100L, 3L));
    }

    private StormFrontTerrainPlugin front(StormFrontTerrainPlugin.Params params) {
        CampaignTerrainAPI entity = mock(CampaignTerrainAPI.class);
        Vector2f location = new Vector2f();
        when(entity.getLocation()).thenReturn(location);
        when(entity.getContainingLocation()).thenReturn(system);
        doAnswer(invocation -> {
            location.set(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(entity).setFixedLocation(anyFloat(), anyFloat());
        StormFrontTerrainPlugin front = new StormFrontTerrainPlugin();
        front.init(StormFront.TERRAIN_ID, entity, params);
        when(entity.getPlugin()).thenReturn(front);
        terrain.add(entity);
        return front;
    }

    @Test
    void theAnomalyIsBuiltIn() {
        assertSame(StormFront.INSTANCE, ChartAnomalies.byId(StormFront.ANOMALY_ID));
    }

    @Test
    void generationAddsOneTaggedFrontReachingPastTheOutermostWorld() {
        PlanetAPI star = planet("star", 0f, 0f);
        when(star.isStar()).thenReturn(true);
        PlanetAPI inner = planet("inner", 3000f, 0f);
        PlanetAPI outer = planet("outer", 0f, 9000f);
        when(system.getPlanets()).thenReturn(List.of(star, inner, outer));
        CampaignTerrainAPI added = mock(CampaignTerrainAPI.class);
        when(system.addTerrain(anyString(), any())).thenReturn(added);
        AnomalyContentSite site = new AnomalyContentSite(system, "run1", 4, "pirates", ChartGenContext.none(), List.of(), new Vector2f(),
                spot -> true, salt -> new Random(salt));

        StormFront.INSTANCE.generate(site);

        ArgumentCaptor<Object> params = ArgumentCaptor.forClass(Object.class);
        verify(system).addTerrain(eq(StormFront.TERRAIN_ID), params.capture());
        StormFrontTerrainPlugin.Params front = (StormFrontTerrainPlugin.Params) params.getValue();
        assertEquals("run1", front.runId());
        assertEquals(List.of("inner", "outer"), front.markIds());
        StormPath path = assertInstanceOf(StormPath.class, front.course());
        assertEquals(9100f + StormRules.EDGE_MARGIN, path.edgeRadius(), 0.01f);
        assertEquals(StormRules.BAND_WIDTH, path.bandWidth());
        assertEquals(StormRules.CROSSING_DAYS, path.crossingDays());
        assertEquals(100L, front.startTimestamp());
        assertFalse(front.solar());
        assertEquals(0f, front.heldSlowShare());
        verify(added).addTag(StormFront.FRONT_TAG);
        assertTrue(site.placed().contains(added));
    }

    private StormFrontTerrainPlugin.Params generated(ChartGenContext context) {
        CampaignTerrainAPI added = mock(CampaignTerrainAPI.class);
        when(system.addTerrain(anyString(), any())).thenReturn(added);
        StormFront.INSTANCE.generate(new AnomalyContentSite(system, "run1", 4, "pirates", context, List.of(), new Vector2f(), spot -> true,
                salt -> new Random(salt)));
        ArgumentCaptor<Object> params = ArgumentCaptor.forClass(Object.class);
        verify(system).addTerrain(eq(StormFront.TERRAIN_ID), params.capture());
        return (StormFrontTerrainPlugin.Params) params.getValue();
    }

    private static ChartGenContext withLine(ChartModifier line, float value, String... anomalyIds) {
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(line, value))).withAnomalies(List.of(anomalyIds));
    }

    @Test
    void aCapturedFrontCirclesTheBlackHoleThroughItsDerelicts() throws Exception {
        ChartTables.loadAnomalies();
        PlanetAPI hole = planet("hole", 0f, 0f);
        when(hole.isStar()).thenReturn(true);
        when(system.getPlanets()).thenReturn(List.of(hole));
        when(system.getStar()).thenReturn(hole);

        StormFrontTerrainPlugin.Params front = generated(withLine(ChartModifier.CHART_STORM_FRONT_CAPTURED, 1f, StarAnomalies.BLACK_HOLE.id(),
                StormFront.ANOMALY_ID));

        StormOrbit orbit = assertInstanceOf(StormOrbit.class, front.course());
        assertEquals(100f + StormRules.CAPTURED_INNER_GAP, orbit.innerRadius());
        assertEquals(StarAnomalies.DERELICT_REACH + StormRules.CAPTURED_OUTER_MARGIN, orbit.outerRadius());
        assertEquals(StormRules.BAND_WIDTH, orbit.bandWidth());
    }

    @Test
    void aCapturedFrontWithoutABlackHoleStillCrossesTheSystem() throws Exception {
        ChartTables.loadAnomalies();
        PlanetAPI inner = planet("inner", 3000f, 0f);
        when(system.getPlanets()).thenReturn(List.of(inner));

        StormFrontTerrainPlugin.Params front = generated(withLine(ChartModifier.CHART_STORM_FRONT_CAPTURED, 1f, StormFront.ANOMALY_ID));

        assertInstanceOf(StormPath.class, front.course());
    }

    @Test
    void aSolarFrontNeedsStellarFlaresInTheSystem() throws Exception {
        ChartTables.loadAnomalies();
        PlanetAPI inner = planet("inner", 3000f, 0f);
        when(system.getPlanets()).thenReturn(List.of(inner));

        assertTrue(generated(withLine(ChartModifier.CHART_STORM_FRONT_LUCKY_BIOME_MODIFIERS, 1f, StellarFlares.ANOMALY_ID, StormFront.ANOMALY_ID))
                .solar());
    }

    @Test
    void aFrontWithoutStellarFlaresKeepsItsIonColours() throws Exception {
        ChartTables.loadAnomalies();
        PlanetAPI inner = planet("inner", 3000f, 0f);
        when(system.getPlanets()).thenReturn(List.of(inner));

        assertFalse(generated(withLine(ChartModifier.CHART_STORM_FRONT_LUCKY_BIOME_MODIFIERS, 1f, StormFront.ANOMALY_ID)).solar());
    }

    @Test
    void theEyeOfTheStormLineSetsTheHeldSlowShare() {
        PlanetAPI inner = planet("inner", 3000f, 0f);
        when(system.getPlanets()).thenReturn(List.of(inner));

        assertEquals(0.8f, generated(withLine(ChartModifier.CHART_STORM_FRONT_HELD_SPEED_PERCENT, 80f)).heldSlowShare(), 1e-6f);
    }

    @Test
    void theFrontSlowsWhileThePlayerFleetIsInside() {
        StormPath path = new StormPath(0f, 10000f, 3000f, 30f);
        StormFrontTerrainPlugin front = front(new StormFrontTerrainPlugin.Params("run1", path, List.of(), 100L, 3L, false, 0.8f));
        CampaignFleetAPI player = mock(CampaignFleetAPI.class);
        when(player.isPlayerFleet()).thenReturn(true);
        MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class);
        when(stats.getSensorRangeMod()).thenReturn(new StatBonus());
        when(player.getStats()).thenReturn(stats);
        CampaignFleetAPI other = mock(CampaignFleetAPI.class);
        when(other.getStats()).thenReturn(stats);

        front.applyEffect(player, 10f);
        front.applyEffect(other, 10f);
        elapsedDays = 15f;
        front.advance(0.1f);

        assertEquals(8f, front.heldDays(), 1e-4f);
        assertEquals(path.centre(7f).x, front.getEntity().getLocation().x, 0.01f);
        elapsedDays = 30f;
        assertFalse(front.isDone());
        elapsedDays = 38f;
        assertTrue(front.isDone());
    }

    @Test
    void aCapturedFrontNeverLeaves() {
        StormFrontTerrainPlugin front = front(new StormOrbit(0f, 0f, 600f, 8000f, 3000f, 0f, 30f));

        elapsedDays = 1000f;
        front.advance(0.1f);

        assertFalse(front.isDone());
        assertTrue(front.captured());
        assertNotNull(StormFront.INSTANCE.intelText(new AnomalyRun(null, system)));
    }

    @Test
    void theFrontFollowsItsPathThenDissipates() {
        StormFrontTerrainPlugin front = front(new StormPath(0f, 10000f, 3000f, 30f));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getLocation()).thenReturn(new Vector2f(0f, 0f));
        when(fleet.getContainingLocation()).thenReturn(system);

        elapsedDays = 15f;
        front.advance(0.1f);
        assertEquals(0f, front.getEntity().getLocation().x, 0.01f);
        assertSame(front, StormFront.frontHolding(fleet));
        AnomalyRun anomalyRun = new AnomalyRun(null, system);
        assertNotNull(StormFront.INSTANCE.intelText(anomalyRun));

        elapsedDays = 30f;
        assertTrue(front.isDone());
        assertNull(StormFront.frontHolding(fleet));
        assertNull(StormFront.INSTANCE.intelText(anomalyRun));
    }

    @Test
    void fleetsInsideLoseSensorRange() {
        StormFrontTerrainPlugin front = front(new StormPath(0f, 10000f, 3000f, 30f));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class);
        StatBonus sensorRange = new StatBonus();
        when(stats.getSensorRangeMod()).thenReturn(sensorRange);
        when(fleet.getStats()).thenReturn(stats);

        front.applyEffect(fleet, 0.1f);

        verify(stats).addTemporaryModMult(eq(StormFrontTerrainPlugin.SENSOR_MOD_DAYS), eq(StormFrontTerrainPlugin.SENSOR_MOD_ID), any(),
                eq(StormRules.SENSOR_RANGE_MULT), eq(sensorRange));
    }

    @Test
    void leavingTheChartLiftsTheSensorPenaltyAndTheTerrainKey() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class);
        StatBonus sensorRange = new StatBonus();
        sensorRange.modifyMult(StormFrontTerrainPlugin.SENSOR_MOD_ID, StormRules.SENSOR_RANGE_MULT);
        when(stats.getSensorRangeMod()).thenReturn(sensorRange);
        when(fleet.getStats()).thenReturn(stats);
        MemoryAPI fleetMemory = mock(MemoryAPI.class);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(fleetMemory);

        StormFront.INSTANCE.clearFrom(fleet);
        StormFront.INSTANCE.clearFrom(null);

        verify(stats).removeTemporaryMod(StormFrontTerrainPlugin.SENSOR_MOD_ID);
        assertNull(sensorRange.getMultBonus(StormFrontTerrainPlugin.SENSOR_MOD_ID));
        verify(fleetMemory).unset("$terrain_" + StormFront.TERRAIN_ID);
    }

    @Test
    void puffsFillTheBandWithinTheirCaps() {
        StormPath path = new StormPath(0f, 10000f, 3000f, 30f);
        List<StormFrontTerrainPlugin.Puff> puffs = StormFrontTerrainPlugin.puffs(path, 1L);

        assertTrue(puffs.size() >= StormFrontTerrainPlugin.MIN_PUFFS && puffs.size() <= StormFrontTerrainPlugin.MAX_PUFFS);
        for (StormFrontTerrainPlugin.Puff puff : puffs) {
            assertTrue(Math.abs(puff.along()) <= path.bandWidth() / 2f);
            assertTrue(Math.abs(puff.across()) <= path.bandLength() / 2f);
        }
        assertEquals(1f, StormFrontTerrainPlugin.endFade(0f, path.bandLength()));
        assertEquals(0f, StormFrontTerrainPlugin.endFade(path.bandLength() / 2f, path.bandLength()));
    }
}
