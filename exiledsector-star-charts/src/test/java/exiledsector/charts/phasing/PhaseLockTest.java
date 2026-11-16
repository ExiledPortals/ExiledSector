package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhaseLockTest {

    private static final String RUN_ID = "run1";
    private static final int TIER = 12;

    private MockSector mockSector;
    private final Map<String, Object> gateMemory = new HashMap<>();
    private MarketAPI gateMarket;
    private Industry gate;
    private StarSystemAPI chartSystem;
    private PlanetAPI planet;
    private MarketAPI planetMarket;
    private ChartRun run;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        gate = mock(Industry.class);
        gateMarket = mock(MarketAPI.class);
        when(gateMarket.isPlayerOwned()).thenReturn(true);
        when(gateMarket.getName()).thenReturn("Home");
        when(gateMarket.hasIndustry(PhaseDisplacementGate.INDUSTRY_ID)).thenReturn(true);
        when(gateMarket.getIndustry(PhaseDisplacementGate.INDUSTRY_ID)).thenReturn(gate);
        MemoryAPI memory = mapMemory(gateMemory);
        when(gateMarket.getMemoryWithoutUpdate()).thenReturn(memory);
        EconomyAPI economy = mock(EconomyAPI.class);
        when(economy.getMarketsCopy()).thenAnswer(invocation -> new ArrayList<>(List.of(gateMarket)));
        when(mockSector.sector.getEconomy()).thenReturn(economy);

        chartSystem = mock(StarSystemAPI.class);
        when(chartSystem.getId()).thenReturn(ChartIds.systemId(RUN_ID));
        when(chartSystem.getBaseName()).thenReturn("Charted Space");
        planet = chartPlanet("planet_3", SurveyLevel.FULL);
        planetMarket = planet.getMarket();

        run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(TIER, "pirates", "boss", 3, 2, List.of("black_hole")));
        ChartRuns.store(run);
        PhasedPlanetTraits.use(List.of(
                new PhasedPlanetTrait("crust", "exiledSector_time_slipped_crust", 1f, Map.of(), Map.of(), Set.of()),
                new PhasedPlanetTrait("throat", "exiledSector_residual_wormhole_throat", 1f, Map.of(), Map.of(), Set.of()),
                new PhasedPlanetTrait("husks", "exiledSector_fabricator_husks", 1f, Map.of(), Map.of(), Set.of())));
    }

    @AfterEach
    void tearDown() {
        PhasedPlanetTraits.use(List.of());
        StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        ChartRuns.delete(RUN_ID);
        mockSector.close();
    }

    static MemoryAPI mapMemory(Map<String, Object> values) {
        MemoryAPI memory = mock(MemoryAPI.class);
        doAnswer(invocation -> values.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        doAnswer(invocation -> values.remove(invocation.<String>getArgument(0))).when(memory).unset(anyString());
        when(memory.getString(anyString())).thenAnswer(invocation -> values.get(invocation.<String>getArgument(0)) instanceof String text ? text : null);
        when(memory.getFloat(anyString())).thenAnswer(invocation -> values.get(invocation.<String>getArgument(0)) instanceof Number number
                ? number.floatValue() : 0f);
        when(memory.contains(anyString())).thenAnswer(invocation -> values.containsKey(invocation.<String>getArgument(0)));
        return memory;
    }

    private PlanetAPI chartPlanet(String id, SurveyLevel surveyLevel) {
        PlanetAPI chartPlanet = mock(PlanetAPI.class);
        MarketAPI market = mock(MarketAPI.class);
        when(market.getSurveyLevel()).thenReturn(surveyLevel);
        List<MarketConditionAPI> conditions = List.of(condition("cold", false), condition("ore_moderate", false),
                condition("population_1", true));
        when(market.getConditions()).thenReturn(conditions);
        PlanetSpecAPI spec = mock(PlanetSpecAPI.class);
        when(spec.getTexture()).thenReturn("graphics/planets/frozen01.jpg");
        when(chartPlanet.getId()).thenReturn(id);
        when(chartPlanet.getName()).thenReturn("Charted Space III");
        when(chartPlanet.getTypeId()).thenReturn("frozen");
        when(chartPlanet.getRadius()).thenReturn(140f);
        when(chartPlanet.getSpec()).thenReturn(spec);
        when(chartPlanet.getMarket()).thenReturn(market);
        when(chartPlanet.getContainingLocation()).thenReturn(chartSystem);
        when(chartPlanet.getStarSystem()).thenReturn(chartSystem);
        return chartPlanet;
    }

    private static MarketConditionAPI condition(String id, boolean decivRemove) {
        MarketConditionAPI condition = mock(MarketConditionAPI.class);
        MarketConditionSpecAPI spec = mock(MarketConditionSpecAPI.class);
        when(spec.isDecivRemove()).thenReturn(decivRemove);
        when(condition.getId()).thenReturn(id);
        when(condition.getSpec()).thenReturn(spec);
        return condition;
    }

    @Test
    void aSurveyedRunPlanetIsOfferedWhileTheGateIsIdle() {
        assertEquals(PhaseLock.Offer.AVAILABLE, PhaseLock.offerFor(mockSector.sector, planet));
        assertEquals(PhaseLock.Offer.NOT_SURVEYED, PhaseLock.offerFor(mockSector.sector, chartPlanet("planet_4", SurveyLevel.SEEN)));
    }

    @Test
    void starsSitesAndPlanetsOutsideTheRunAreNotOffered() {
        PlanetAPI star = chartPlanet("star", SurveyLevel.FULL);
        when(star.isStar()).thenReturn(true);
        PlanetAPI site = chartPlanet("site", SurveyLevel.FULL);
        when(site.hasTag(ChartIds.CONTENT_TAG)).thenReturn(true);
        PlanetAPI elsewhere = chartPlanet("elsewhere", SurveyLevel.FULL);
        StarSystemAPI homeSystem = mock(StarSystemAPI.class);
        when(homeSystem.getId()).thenReturn("home");
        when(elsewhere.getContainingLocation()).thenReturn(homeSystem);

        assertNull(PhaseLock.offerFor(mockSector.sector, star));
        assertNull(PhaseLock.offerFor(mockSector.sector, site));
        assertNull(PhaseLock.offerFor(mockSector.sector, elsewhere));
        assertNull(PhaseLock.offerFor(mockSector.sector, null));
    }

    @Test
    void nothingIsOfferedWithoutAnIdleGateOrWithTheDomainOff() {
        when(gate.isBuilding()).thenReturn(true);
        assertNull(PhaseLock.offerFor(mockSector.sector, planet));

        when(gate.isBuilding()).thenReturn(false);
        PhaseGateState.startJob(gateMarket, PhaseLock.recordOf(planet, run, null));
        assertNull(PhaseLock.offerFor(mockSector.sector, planet));

        PhaseGateState.clearJob(gateMarket);
        StarChartSettings.setEnabledForTests(false);
        assertNull(PhaseLock.offerFor(mockSector.sector, planet));
    }

    @Test
    void lockingRecordsThePlanetWithItsKeptConditionsAndRolledTraits() {
        PhasedPlanetRecord lock = PhaseLock.lock(mockSector.sector, planet);

        assertNotNull(lock);
        assertEquals(lock, PhaseGateState.lock(gateMarket));
        assertEquals(RUN_ID, lock.runId());
        assertEquals("planet_3", lock.planetId());
        assertEquals("frozen", lock.typeId());
        assertEquals(140f, lock.radius());
        assertEquals("graphics/planets/frozen01.jpg", lock.texture());
        assertEquals(List.of("cold", "ore_moderate"), lock.conditionIds());
        assertEquals("Charted Space", lock.originName());
        assertEquals(List.of("black_hole"), lock.anomalyIds());
        assertEquals(TIER, lock.tier());
        int traits = lock.traitConditionIds().size();
        assertTrue(traits >= 2 && traits <= 3, String.valueOf(traits));
        assertEquals(PhaseLock.Offer.LOCKED, PhaseLock.offerFor(mockSector.sector, planet));
    }

    @Test
    void theTraitRollIsSeededByTheRunAndPlanet() {
        PhasedPlanetRecord first = PhaseLock.lock(mockSector.sector, planet);
        PhaseGateState.clearLock(gateMarket);
        PhasedPlanetRecord again = PhaseLock.lock(mockSector.sector, planet);

        assertEquals(first.traitConditionIds(), again.traitConditionIds());
    }

    @Test
    void lockingAnotherPlanetReplacesTheLock() {
        PhaseLock.lock(mockSector.sector, planet);
        PhasedPlanetRecord replaced = PhaseLock.lock(mockSector.sector, chartPlanet("planet_5", SurveyLevel.FULL));

        assertEquals("planet_5", PhaseGateState.lock(gateMarket).planetId());
        assertEquals(replaced, PhaseLock.currentLock(gateMarket));
        assertEquals(PhaseLock.Offer.AVAILABLE, PhaseLock.offerFor(mockSector.sector, planet));
    }

    @Test
    void aCompletedRunTurnsTheLockIntoAPullJob() {
        PhasedPlanetRecord lock = PhaseLock.lock(mockSector.sector, planet);
        assertTrue(run.markObjectiveComplete());

        assertTrue(PhaseLock.runClosing(mockSector.sector, run));

        assertEquals(lock, PhaseGateState.job(gateMarket));
        assertEquals(0f, PhaseGateState.jobDays(gateMarket));
        assertNull(PhaseGateState.lock(gateMarket));
    }

    @Test
    void aFailedRunClearsTheLock() {
        PhaseLock.lock(mockSector.sector, planet);

        assertFalse(PhaseLock.runClosing(mockSector.sector, run));

        assertNull(PhaseGateState.lock(gateMarket));
        assertFalse(PhaseGateState.hasJob(gateMarket));
    }

    @Test
    void anotherRunsCloseLeavesTheLockAlone() {
        PhaseLock.lock(mockSector.sector, planet);
        ChartRun other = new ChartRun("run2", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);

        assertFalse(PhaseLock.runClosing(mockSector.sector, other));

        assertNotNull(PhaseGateState.lock(gateMarket));
    }

    @Test
    void aLockWhoseRunIsGoneIsForgotten() {
        PhaseLock.lock(mockSector.sector, planet);
        ChartRuns.delete(RUN_ID);

        assertNull(PhaseLock.currentLock(gateMarket));
        assertFalse(gateMemory.containsKey(PhaseGateState.LOCK_KEY));
    }
}
