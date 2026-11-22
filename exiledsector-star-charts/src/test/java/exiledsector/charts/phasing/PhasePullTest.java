package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import exiledsector.MockSector;
import exiledsector.charts.StarChartSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhasePullTest {

    private MockSector mockSector;
    private SettingsAPI settings;
    private MockedStatic<Misc> miscMock;
    private MockedStatic<HypershuntPower> hypershuntMock;
    private MockedStatic<PhaseGateRules> rulesMock;
    private final Map<String, Object> gateMemory = new HashMap<>();
    private PhaseDisplacementGate gate;
    private MarketAPI market;
    private StarSystemAPI system;
    private PlanetAPI star;
    private CustomCampaignEntityAPI phaseGate;
    private final PhaseGateEntityPlugin gatePlugin = new PhaseGateEntityPlugin();
    private final Map<String, Object> phaseGateMemory = new HashMap<>();
    private PlanetAPI arrived;
    private final PhasedPlanetRecord job = new PhasedPlanetRecord("run1", "planet_3", "Charted Space III", "frozen", 140f, null,
            List.of("cold"), "Charted Space", null, List.of(), 6, List.of());

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        settings = mock(SettingsAPI.class);
        when(settings.getMarketConditionSpec(anyString())).thenReturn(mock(MarketConditionSpecAPI.class));
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        miscMock = Mockito.mockStatic(Misc.class, Mockito.CALLS_REAL_METHODS);
        miscMock.when(() -> Misc.initConditionMarket(any())).thenAnswer(invocation -> null);
        miscMock.when(() -> Misc.fadeAndExpire(any())).thenAnswer(invocation -> null);
        hypershuntMock = Mockito.mockStatic(HypershuntPower.class, Mockito.CALLS_REAL_METHODS);
        hypershuntMock.when(() -> HypershuntPower.reaches(any())).thenReturn(true);
        rulesMock = Mockito.mockStatic(PhaseGateRules.class, Mockito.CALLS_REAL_METHODS);
        rulesMock.when(() -> PhaseGateRules.holdsRequiredGateway(any())).thenReturn(true);

        MemoryAPI memory = PhaseLockTest.mapMemory(gateMemory);
        market = mock(MarketAPI.class);
        when(market.getId()).thenReturn("home");
        when(market.isPlayerOwned()).thenReturn(true);
        when(market.getMemoryWithoutUpdate()).thenReturn(memory);
        EconomyAPI economy = mock(EconomyAPI.class);
        when(economy.getMarket("home")).thenReturn(market);
        when(mockSector.sector.getEconomy()).thenReturn(economy);
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.getRadius()).thenReturn(500f);
        when(star.getContainingLocation()).thenReturn(system);
        when(star.getLocation()).thenReturn(new Vector2f(0f, 0f));
        when(system.getStar()).thenReturn(star);
        when(market.getStarSystem()).thenReturn(system);
        gate = mock(PhaseDisplacementGate.class);
        when(gate.getMarket()).thenReturn(market);
        when(gate.isFunctional()).thenReturn(true);
        when(gate.getMaxDeficit(any(String[].class))).thenReturn(deficit(0));

        phaseGate = mock(CustomCampaignEntityAPI.class);
        when(phaseGate.getLocation()).thenReturn(new Vector2f(0f, 2000f));
        when(phaseGate.getCustomPlugin()).thenReturn(gatePlugin);
        when(phaseGate.getCustomEntityType()).thenReturn(PhasePull.GATE_TYPE);
        when(phaseGate.getContainingLocation()).thenReturn(system);
        MemoryAPI phaseGateMemoryApi = PhaseLockTest.mapMemory(phaseGateMemory);
        when(phaseGate.getMemoryWithoutUpdate()).thenReturn(phaseGateMemoryApi);
        when(system.addCustomEntity(anyString(), anyString(), eq(PhasePull.GATE_TYPE), anyString())).thenAnswer(invocation -> {
            when(mockSector.sector.getEntityById(PhasePull.gateId(market))).thenReturn(phaseGate);
            return phaseGate;
        });
        arrived = mock(PlanetAPI.class);
        when(arrived.getMarket()).thenReturn(mock(MarketAPI.class));
        when(arrived.getMemoryWithoutUpdate()).thenReturn(mock(MemoryAPI.class));
        when(system.addPlanet(anyString(), any(), anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(arrived);
        PhaseGateState.startJob(market, job);
    }

    @AfterEach
    void tearDown() {
        StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        rulesMock.close();
        hypershuntMock.close();
        miscMock.close();
        mockSector.close();
    }

    private static Pair<String, Integer> deficit(int amount) {
        Pair<String, Integer> deficit = new Pair<>();
        deficit.two = amount;
        return deficit;
    }

    @Test
    void thePullPlacesOnePhaseGateAtTheDestinationThatShowsItsProgress() {
        assertNull(PhasePull.advance(mockSector.sector, gate, 30f));
        assertNull(PhasePull.advance(mockSector.sector, gate, 30f));

        verify(system, times(1)).addCustomEntity(anyString(), anyString(), eq(PhasePull.GATE_TYPE), anyString());
        float orbit = PhaseGateState.jobOrbit(market);
        assertTrue(orbit > 0f);
        verify(phaseGate).setCircularOrbit(eq(star), anyFloat(), eq(orbit), eq(PhaseDestination.orbitDays(orbit)));
        assertEquals(60f, PhaseGateState.jobDays(market));
        assertEquals(0.5f, gatePlugin.progressShare(), 0.001f);
        assertTrue(PhasePull.isPhaseGate(phaseGate));
        assertEquals("home", phaseGateMemory.get(PhasePull.GATE_MARKET_KEY));
    }

    @Test
    void startingThePullPlacesThePhaseGateAtOnce() {
        PhasePull.started(mockSector.sector, market);

        verify(system, times(1)).addCustomEntity(anyString(), anyString(), eq(PhasePull.GATE_TYPE), anyString());
        assertEquals(0f, gatePlugin.progressShare());
    }

    @Test
    void theGateLeavesRoomForItselfAroundASmallPlanet() {
        PhasedPlanetRecord small = new PhasedPlanetRecord("run1", "planet_4", "Charted Space IV", "barren", 40f, null, List.of(),
                "Charted Space", null, List.of(), 6, List.of());

        float smallOrbit = PhasePull.orbitFor(market, system, star, small);

        assertEquals(PhaseDestination.orbitRadiusNear(system, star, null, PhaseGateEntityPlugin.frameRadiusFor(small.radius())), smallOrbit);
    }

    @Test
    void theGateIsSizedToFitTheIncomingPlanetInsideItsRing() {
        PhasePull.advance(mockSector.sector, gate, 10f);

        float gateRadius = PhaseGateEntityPlugin.frameRadiusFor(job.radius());
        verify(phaseGate).setRadius(gateRadius);
        assertTrue(gateRadius * PhaseGateEntityPlugin.INNER_OPENING_SHARE > job.radius());
        assertEquals(PhaseGateEntityPlugin.MIN_FRAME_RADIUS, PhaseGateEntityPlugin.frameRadiusFor(10f));
    }

    @Test
    void theGhostUsesThePlanetsTextureOrItsTypesDefault() {
        PhasedPlanetRecord textured = new PhasedPlanetRecord("run1", "planet_5", "Charted Space V", "frozen", 140f, "graphics/planets/ice.jpg",
                List.of(), "Charted Space", null, List.of(), 6, List.of());
        PlanetSpecAPI frozen = mock(PlanetSpecAPI.class);
        when(frozen.getPlanetType()).thenReturn("frozen");
        when(frozen.getTexture()).thenReturn("graphics/planets/frozen.jpg");
        when(settings.getAllPlanetSpecs()).thenReturn(List.of(frozen));

        assertEquals("graphics/planets/ice.jpg", PhasePull.textureFor(textured));
        assertEquals("graphics/planets/frozen.jpg", PhasePull.textureFor(job));
    }

    @Test
    void aWatchedGatePlaysItsArrivalBeforeThePlanetAppears() {
        when(phaseGate.isInCurrentLocation()).thenReturn(true);
        PhasePull.advance(mockSector.sector, gate, 100f);

        assertNull(PhasePull.advance(mockSector.sector, gate, 20f));

        assertTrue(gatePlugin.isArriving());
        assertTrue(PhaseGateState.hasJob(market));
        assertEquals(1f, gatePlugin.progressShare());
        verify(system, times(0)).addPlanet(anyString(), any(), anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat());

        assertNull(PhasePull.advance(mockSector.sector, gate, 1f));
        assertSame(arrived, PhasePull.finishArrival(mockSector.sector, phaseGate));
        assertFalse(PhaseGateState.hasJob(market));
        verify(system, times(0)).removeEntity(phaseGate);
        assertNull(PhasePull.finishArrival(mockSector.sector, phaseGate));
        verify(system, times(1)).addPlanet(anyString(), any(), anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    void theStatusLineCountsTheDaysLeftOrNamesThePause() {
        when(market.getIndustry(PhaseDisplacementGate.INDUSTRY_ID)).thenReturn(gate);
        PhaseGateState.setJobDays(market, 30.5f);

        assertEquals(90, PhasePull.daysLeft(market));
        assertTrue(PhasePull.statusLine(market).contains("Charted Space III"));
        assertNull(PhasePull.pauseFor(market));

        when(gate.isFunctional()).thenReturn(false);
        assertEquals(PhasePull.Pause.NOT_FUNCTIONAL, PhasePull.pauseFor(market));
        when(market.getIndustry(PhaseDisplacementGate.INDUSTRY_ID)).thenReturn(null);
        assertEquals(PhasePull.Pause.NOT_FUNCTIONAL, PhasePull.pauseFor(market));
    }

    @Test
    void cancellingDropsTheJobAndFadesTheGate() {
        PhasePull.advance(mockSector.sector, gate, 10f);

        assertTrue(PhasePull.cancel(mockSector.sector, market));

        assertFalse(PhaseGateState.hasJobEntry(market));
        miscMock.verify(() -> Misc.fadeAndExpire(phaseGate));
        verify(market).reapplyIndustries();
        assertNull(PhasePull.statusLine(market));
        assertFalse(PhasePull.cancel(mockSector.sector, market));
    }

    @Test
    void anOldSavesWormholeIsReplacedByThePhaseGate() {
        CustomCampaignEntityAPI legacyWormhole = mock(CustomCampaignEntityAPI.class);
        when(legacyWormhole.getContainingLocation()).thenReturn(system);
        when(mockSector.sector.getEntityById(PhasePull.LEGACY_WORMHOLE_ID_PREFIX + "home")).thenReturn(legacyWormhole);

        PhasePull.advance(mockSector.sector, gate, 10f);

        verify(system).removeEntity(legacyWormhole);
        verify(system, times(1)).addCustomEntity(anyString(), anyString(), eq(PhasePull.GATE_TYPE), anyString());
    }

    @Test
    void aShortageOrMissingPowerPausesThePull() {
        when(gate.getMaxDeficit(any(String[].class))).thenReturn(deficit(3));
        PhasePull.advance(mockSector.sector, gate, 30f);
        assertEquals(PhasePull.Pause.SHORTAGE, PhasePull.pauseFor(gate));
        assertEquals(0f, PhaseGateState.jobDays(market));

        when(gate.getMaxDeficit(any(String[].class))).thenReturn(deficit(0));
        hypershuntMock.when(() -> HypershuntPower.reaches(any())).thenReturn(false);
        assertEquals(PhasePull.Pause.NO_HYPERSHUNT, PhasePull.pauseFor(gate));

        StarChartSettings.setEnabledForTests(false);
        assertEquals(PhasePull.Pause.DOMAIN_OFF, PhasePull.pauseFor(gate));
    }

    @Test
    void aDisruptedGateOrAMissingGatewayPausesThePull() {
        when(gate.isFunctional()).thenReturn(false);
        PhasePull.advance(mockSector.sector, gate, 30f);
        assertEquals(PhasePull.Pause.NOT_FUNCTIONAL, PhasePull.pauseFor(gate));
        assertEquals(0f, PhaseGateState.jobDays(market));

        when(gate.isFunctional()).thenReturn(true);
        rulesMock.when(() -> PhaseGateRules.holdsRequiredGateway(any())).thenReturn(false);
        PhasePull.advance(mockSector.sector, gate, 30f);
        assertEquals(PhasePull.Pause.NO_GATEWAY, PhasePull.pauseFor(gate));
        assertEquals(0f, PhaseGateState.jobDays(market));
    }

    @Test
    void aCapturedGatePausesThePullAndPlacesNothing() {
        when(market.isPlayerOwned()).thenReturn(false);
        when(market.getFactionId()).thenReturn("hegemony");

        assertNull(PhasePull.advance(mockSector.sector, gate, PhasePull.PULL_DAYS + 1f));

        assertEquals(PhasePull.Pause.NOT_PLAYER_OWNED, PhasePull.pauseFor(gate));
        assertEquals(0f, PhaseGateState.jobDays(market));
        assertTrue(PhaseGateState.hasJob(market));
        verify(system, times(0)).addPlanet(anyString(), any(), anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    void theFullPullReplacesThePhaseGateWithThePlanet() {
        PhasePull.advance(mockSector.sector, gate, 100f);
        float orbit = PhaseGateState.jobOrbit(market);

        PlanetAPI placed = PhasePull.advance(mockSector.sector, gate, 20f);

        assertSame(arrived, placed);
        verify(system).removeEntity(phaseGate);
        verify(system).addPlanet(anyString(), eq(star), eq("Charted Space III"), eq("frozen"), eq(90f), eq(140f), eq(orbit), anyFloat());
        assertFalse(PhaseGateState.hasJob(market));
        assertEquals(0f, PhaseGateState.jobOrbit(market));
        verify(market).reapplyIndustries();
    }

    @Test
    void abandoningTheGateDropsTheJobAndThePhaseGate() {
        PhasePull.advance(mockSector.sector, gate, 10f);

        PhasePull.abandon(mockSector.sector, market);

        assertFalse(PhaseGateState.hasJobEntry(market));
        verify(system).removeEntity(phaseGate);
    }
}
