package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.StarChartSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
    private MockedStatic<Misc> miscMock;
    private MockedStatic<HypershuntPower> hypershuntMock;
    private MockedStatic<PhaseGateRules> rulesMock;
    private final Map<String, Object> gateMemory = new HashMap<>();
    private PhaseDisplacementGate gate;
    private MarketAPI market;
    private StarSystemAPI system;
    private PlanetAPI star;
    private CustomCampaignEntityAPI wormhole;
    private PlanetAPI arrived;
    private final PhasedPlanetRecord job = new PhasedPlanetRecord("run1", "planet_3", "Charted Space III", "frozen", 140f, null,
            List.of("cold"), "Charted Space", null, List.of(), 6, List.of());

    @BeforeAll
    static void loadThemeGenerator() throws ClassNotFoundException {
        ThemeGeneratorLoading.ensureLoaded();
    }

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getMarketConditionSpec(anyString())).thenReturn(mock(MarketConditionSpecAPI.class));
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        miscMock = Mockito.mockStatic(Misc.class, Mockito.CALLS_REAL_METHODS);
        miscMock.when(() -> Misc.initConditionMarket(any())).thenAnswer(invocation -> null);
        hypershuntMock = Mockito.mockStatic(HypershuntPower.class, Mockito.CALLS_REAL_METHODS);
        hypershuntMock.when(() -> HypershuntPower.reaches(any())).thenReturn(true);
        rulesMock = Mockito.mockStatic(PhaseGateRules.class, Mockito.CALLS_REAL_METHODS);
        rulesMock.when(() -> PhaseGateRules.holdsRequiredGateway(any())).thenReturn(true);

        MemoryAPI memory = PhaseLockTest.mapMemory(gateMemory);
        market = mock(MarketAPI.class);
        when(market.getId()).thenReturn("home");
        when(market.getMemoryWithoutUpdate()).thenReturn(memory);
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

        wormhole = mock(CustomCampaignEntityAPI.class);
        when(wormhole.getLocation()).thenReturn(new Vector2f(0f, 2000f));
        when(system.addCustomEntity(anyString(), anyString(), eq(PhasePull.WORMHOLE_TYPE), anyString())).thenAnswer(invocation -> {
            when(mockSector.sector.getEntityById(PhasePull.wormholeId(market))).thenReturn(wormhole);
            return wormhole;
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
    void thePullOpensOneHeldOpenWormholeThatGrowsWithProgress() {
        assertNull(PhasePull.advance(mockSector.sector, gate, 30f));
        assertNull(PhasePull.advance(mockSector.sector, gate, 30f));

        verify(system, times(1)).addCustomEntity(anyString(), anyString(), eq(PhasePull.WORMHOLE_TYPE), anyString());
        verify(wormhole).addTag(ChartIds.HELD_OPEN_WORMHOLE_TAG);
        float orbit = PhaseGateState.jobOrbit(market);
        assertTrue(orbit > 0f);
        verify(wormhole).setCircularOrbit(eq(star), anyFloat(), eq(orbit), eq(PhaseDestination.orbitDays(orbit)));
        assertEquals(60f, PhaseGateState.jobDays(market));
        verify(wormhole).setRadius(PhasePull.wormholeRadius(job, 60f));
        assertTrue(PhasePull.wormholeRadius(job, 60f) > PhasePull.wormholeRadius(job, 30f));
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
    void theFullPullReplacesTheWormholeWithThePlanet() {
        PhasePull.advance(mockSector.sector, gate, 100f);
        float orbit = PhaseGateState.jobOrbit(market);

        PlanetAPI placed = PhasePull.advance(mockSector.sector, gate, 20f);

        assertSame(arrived, placed);
        verify(system).removeEntity(wormhole);
        verify(system).addPlanet(anyString(), eq(star), eq("Charted Space III"), eq("frozen"), eq(90f), eq(140f), eq(orbit), anyFloat());
        assertFalse(PhaseGateState.hasJob(market));
        assertEquals(0f, PhaseGateState.jobOrbit(market));
        verify(market).reapplyIndustries();
    }

    @Test
    void abandoningTheGateDropsTheJobAndTheWormhole() {
        PhasePull.advance(mockSector.sector, gate, 10f);
        when(wormhole.getContainingLocation()).thenReturn(system);

        PhasePull.abandon(mockSector.sector, market);

        assertFalse(PhaseGateState.hasJobEntry(market));
        verify(system).removeEntity(wormhole);
    }
}
