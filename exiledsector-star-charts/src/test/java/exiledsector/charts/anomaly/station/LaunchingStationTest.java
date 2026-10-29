package exiledsector.charts.anomaly.station;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantAssignmentAI;
import exiledlib.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunBound;
import exiledsector.charts.run.ChartRuns;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LaunchingStationTest {

    private static final String RUN_ID = "run1";

    private MockSector mockSector;
    private FakeChartContentEngine engine;
    private StarSystemAPI system;
    private PlanetAPI star;
    private final List<EveryFrameScript> systemScripts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.getLocation()).thenReturn(new Vector2f());
        when(system.getCenter()).thenReturn(star);
        when(system.getId()).thenReturn(ChartIds.systemId(RUN_ID));
        doAnswer(invocation -> systemScripts.add(invocation.getArgument(0))).when(system).addScript(any());
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        ChartTables.resetAnomalies();
        AtlasCatalysts.resetForTests();
        mockSector.close();
    }

    private AnomalyContentSite site(int tier) {
        return new AnomalyContentSite(system, RUN_ID, tier, "remnant", ChartGenContext.none(), List.of(), new Vector2f(), spot -> true,
                salt -> new Random(salt));
    }

    private static ChartRun run(int tier) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(tier, "remnant", ChartObjective.CLEAR, List.of(), null, 3, 2));
        ChartRuns.store(run);
        return run;
    }

    private CampaignFleetAPI placed(LaunchingStation anomaly, int tier) {
        CampaignFleetAPI station = anomaly.place(site(tier));
        when(station.getContainingLocation()).thenReturn(system);
        when(station.getLocationInHyperspace()).thenReturn(new Vector2f());
        return station;
    }

    private static float launchPoints(int tier, int lost) {
        ChartTier chartTier = ChartAnomalyFleets.tier(tier, ChartGenContext.none());
        return ChartAnomalyFleets.combat(Factions.REMNANTS, "patrol", chartTier, chartTier.fleetSize() * StationRules.fleetShare(lost)).combatPoints();
    }

    @Test
    void theNexusIsADamagedStationAtLowTiersAndAFullOneAfterMarkedWithTheRunTier() {
        CampaignFleetAPI low = placed(RemnantNexusStation.INSTANCE, StationRules.LAST_DAMAGED_NEXUS_TIER);
        CampaignFleetAPI high = placed(RemnantNexusStation.INSTANCE, StationRules.LAST_DAMAGED_NEXUS_TIER + 1);

        assertEquals(List.of(StationRules.NEXUS_DAMAGED_VARIANT, StationRules.NEXUS_VARIANT), engine.stationVariants);
        assertEquals(StationRules.LAST_DAMAGED_NEXUS_TIER, low.getMemoryWithoutUpdate().get(ChartIds.TIER_MEMORY_KEY));
        assertTrue(low.getMemoryWithoutUpdate().getBoolean(LaunchingStation.DAMAGED_STATION_KEY));
        assertFalse(high.getMemoryWithoutUpdate().getBoolean(LaunchingStation.DAMAGED_STATION_KEY));
        assertTrue(low.hasTag(ChartIds.runTag(RUN_ID)));
        assertTrue(low.hasTag(RemnantNexusStation.INSTANCE.stationTag()));
        assertTrue(low.getMemoryWithoutUpdate().getBoolean(ChartIds.fleetRunKey(RUN_ID)));
        verify(system).addEntity(low);
        verify(low).setAI(null);
        verify(low).setCircularOrbitWithSpin(eq(star), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    void eachStationAddsItsOwnRunBoundFleetManagerToTheSystem() {
        placed(RemnantNexusStation.INSTANCE, 3);
        placed(RemnantNexusStation.INSTANCE, 12);
        placed(DomainMothership.INSTANCE, 5);

        NexusFleetManager damaged = assertInstanceOf(NexusFleetManager.class, systemScripts.get(0));
        NexusFleetManager full = assertInstanceOf(NexusFleetManager.class, systemScripts.get(1));
        MothershipFleetManager mothership = assertInstanceOf(MothershipFleetManager.class, systemScripts.get(2));
        assertEquals(StationRules.DAMAGED_NEXUS_MAX_FLEETS, damaged.maxFleets());
        assertEquals(StationRules.NEXUS_MAX_FLEETS, full.maxFleets());
        assertEquals(StationRules.MOTHERSHIP_MAX_FLEETS, mothership.maxFleets());
        assertTrue(damaged.respawnDays() > full.respawnDays());
        assertEquals(StationRules.MOTHERSHIP_VARIANT, engine.stationVariants.get(2));
        for (EveryFrameScript script : systemScripts) {
            assertTrue(((ChartRunBound) script).belongsToRun(RUN_ID));
            assertFalse(((ChartRunBound) script).belongsToRun("other"));
        }
    }

    @Test
    void aStationWithoutAClearOrbitFailsSoTheAnomalyIsDropped() {
        AnomalyContentSite blocked = new AnomalyContentSite(system, RUN_ID, 4, "remnant", ChartGenContext.none(), List.of(), new Vector2f(),
                spot -> false, salt -> new Random(salt));

        assertThrows(IllegalStateException.class, () -> RemnantNexusStation.INSTANCE.generate(blocked));
        assertTrue(engine.stationVariants.isEmpty());
        assertTrue(systemScripts.isEmpty());
    }

    @Test
    void theNexusLaunchesChartLevelRemnantFleetsThatDropNoChartItemsAndGrowWithEachLoss() {
        run(6);
        CampaignFleetAPI nexus = placed(RemnantNexusStation.INSTANCE, 6);
        NexusFleetManager manager = (NexusFleetManager) systemScripts.get(0);

        CampaignFleetAPI first = manager.spawnFleet();

        assertNotNull(first);
        FleetParamsV3 params = engine.fleetParams.get(0);
        assertEquals(Factions.REMNANTS, params.factionId);
        assertEquals(launchPoints(6, 0), params.combatPts, 0.001f);
        assertEquals(6, first.getMemoryWithoutUpdate().get(ChartIds.TIER_MEMORY_KEY));
        assertTrue(first.getMemoryWithoutUpdate().getBoolean(ChartIds.fleetRunKey(RUN_ID)));
        assertFalse(first.getMemoryWithoutUpdate().contains(ChartIds.START_DP_MEMORY_KEY));
        assertEquals(nexus.getId(), first.getMemoryWithoutUpdate().getString(StationFleets.SOURCE_ID_KEY));
        assertTrue(first.hasTag(ChartIds.runTag(RUN_ID)));
        verify(system).addEntity(first);
        verify(first).addScript(any(RemnantAssignmentAI.class));

        manager.reportFleetDespawnedToListener(first, FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        manager.spawnFleet();

        assertEquals(1, manager.getTotalLost());
        assertEquals(launchPoints(6, 1), engine.fleetParams.get(1).combatPts, 0.001f);
        assertTrue(engine.fleetParams.get(1).combatPts > params.combatPts);
    }

    @Test
    void theMothershipLaunchesDerelictDronesUpToCruisersAndCountsItsLosses() {
        run(9);
        placed(DomainMothership.INSTANCE, 9);
        MothershipFleetManager manager = (MothershipFleetManager) systemScripts.get(0);

        CampaignFleetAPI first = manager.spawnFleet();
        manager.reportFleetDespawnedToListener(first, FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        manager.reportFleetDespawnedToListener(manager.spawnFleet(), FleetDespawnReason.PLAYER_FAR_AWAY, null);

        assertEquals(Factions.DERELICT, engine.fleetParams.get(0).factionId);
        assertEquals(StationRules.DRONE_MAX_SHIP_SIZE, engine.fleetParams.get(0).maxShipSize);
        assertEquals(1, manager.fleetsLost());
        assertTrue(engine.fleetParams.get(1).combatPts > engine.fleetParams.get(0).combatPts);
        assertFalse(first.getMemoryWithoutUpdate().contains(ChartIds.START_DP_MEMORY_KEY));
    }

    @Test
    void nothingLaunchesOnceTheRunIsOverOrTheStationIsDead() {
        ChartRun run = run(6);
        CampaignFleetAPI nexus = placed(RemnantNexusStation.INSTANCE, 6);
        NexusFleetManager manager = (NexusFleetManager) systemScripts.get(0);

        when(nexus.isAlive()).thenReturn(false);
        assertNull(manager.spawnFleet());
        when(nexus.isAlive()).thenReturn(true);
        run.markClosed();
        assertNull(manager.spawnFleet());
        ChartRuns.delete(RUN_ID);
        assertNull(manager.spawnFleet());
        assertTrue(engine.fleetParams.isEmpty());
    }

    @Test
    void theNexusFillsItsFleetsLikeVanillaButNeverPutsAnEncounterPointIntoTheMainHyperspace() {
        run(10);
        placed(RemnantNexusStation.INSTANCE, 10);
        NexusFleetManager manager = (NexusFleetManager) systemScripts.get(0);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getFloat("unitsPerLightYear")).thenReturn(2000f);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        CampaignClockAPI clock = mock(CampaignClockAPI.class);
        when(mockSector.sector.getClock()).thenReturn(clock);
        ListenerManagerAPI listeners = mock(ListenerManagerAPI.class);
        when(mockSector.sector.getListenerManager()).thenReturn(listeners);
        when(mockSector.playerFleet().getLocationInHyperspace()).thenReturn(new Vector2f());

        for (int frame = 0; frame < 10; frame++) {
            manager.advance(0.1f);
        }

        assertEquals(StationRules.NEXUS_MAX_FLEETS, engine.fleetParams.size());
        verify(listeners, never()).addListener(any(), anyBoolean());
        verify(listeners, never()).addListener(any());
    }

    @Test
    void neitherStationAnomalyIsInsertableNorCanACatalystCarryIt() throws Exception {
        ChartTables.loadAnomalies();

        for (String id : List.of(RemnantNexusStation.ID, DomainMothership.ID)) {
            ChartAnomalies.Definition definition = ChartAnomalies.definition(id);
            assertNotNull(definition, id);
            assertFalse(definition.insertable(), id);
            assertTrue(definition.perBiome(), id);
            assertTrue(definition.keptWhenSpent(), id);
            assertFalse(definition.icon().isEmpty(), id);
        }
        assertEquals(Set.of("remnant_expanse"), ChartAnomalies.definition(RemnantNexusStation.ID).biomes());
        assertEquals(Set.of("derelict_graveyard"), ChartAnomalies.definition(DomainMothership.ID).biomes());
        assertSame(RemnantNexusStation.INSTANCE, ChartAnomalies.byId(RemnantNexusStation.ID));
        assertSame(DomainMothership.INSTANCE, ChartAnomalies.byId(DomainMothership.ID));

        List<String> problems = AtlasCatalysts.register(new JSONArray()
                .put(new JSONObject().put("id", "nexus_orders").put("anomaly", RemnantNexusStation.ID).put("family", "orders").put("effect", "anomaly"))
                .put(new JSONObject().put("id", "mothership_survey").put("anomaly", DomainMothership.ID).put("family", "survey").put("effect", "anomaly")));

        assertEquals(2, problems.size());
        assertNull(AtlasCatalysts.byItemId("nexus_orders"));
        assertNull(AtlasCatalysts.byItemId("mothership_survey"));
        assertEquals(1, ChartAnomalies.register(new JSONArray().put(new JSONObject().put("id", RemnantNexusStation.ID).put("insertable", "yes")
                .put("biomes", "remnant_expanse"))).size());
        assertFalse(ChartAnomalies.definition(RemnantNexusStation.ID).insertable());
    }
}
