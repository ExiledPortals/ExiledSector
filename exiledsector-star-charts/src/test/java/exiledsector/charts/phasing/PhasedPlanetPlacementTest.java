package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.util.Misc;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhasedPlanetPlacementTest {

    static final List<String> KNOWN_CONDITIONS = List.of("cold", "ore_moderate", PhasedPlanetTraits.PHASE_DISPLACED_CONDITION_ID,
            "exiledSector_time_slipped_crust");

    private MockSector mockSector;
    private MockedStatic<Misc> miscMock;
    private StarSystemAPI system;
    private SectorEntityToken star;
    private PlanetAPI planet;
    private PlanetSpecAPI planetSpec;
    private MarketAPI market;
    private MemoryAPI planetMemory;
    private MarketConditionAPI addedCondition;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SettingsAPI settings = mock(SettingsAPI.class);
        for (String conditionId : KNOWN_CONDITIONS) {
            when(settings.getMarketConditionSpec(conditionId)).thenReturn(mock(MarketConditionSpecAPI.class));
        }
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        miscMock = Mockito.mockStatic(Misc.class, Mockito.CALLS_REAL_METHODS);
        miscMock.when(() -> Misc.initConditionMarket(any())).thenAnswer(invocation -> null);

        system = mock(StarSystemAPI.class);
        star = mock(SectorEntityToken.class);
        planet = mock(PlanetAPI.class);
        planetSpec = mock(PlanetSpecAPI.class);
        market = mock(MarketAPI.class);
        planetMemory = mock(MemoryAPI.class);
        addedCondition = mock(MarketConditionAPI.class);
        when(planetSpec.getTexture()).thenReturn("graphics/planets/barren01.jpg");
        when(planet.getSpec()).thenReturn(planetSpec);
        when(planet.getMarket()).thenReturn(market);
        when(planet.getMemoryWithoutUpdate()).thenReturn(planetMemory);
        when(market.getConditions()).thenReturn(List.of(addedCondition));
        when(system.addPlanet(anyString(), any(), anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(planet);
    }

    @AfterEach
    void tearDown() {
        miscMock.close();
        mockSector.close();
    }

    private static PhasedPlanetRecord record(String texture) {
        return new PhasedPlanetRecord("run1", "planet_3", "Charted Space III", "frozen", 140f, texture,
                List.of("cold", "ore_moderate", "removed_by_a_mod"), "Charted Space", "frozen_fringe", List.of(), 12,
                List.of("exiledSector_time_slipped_crust"));
    }

    @Test
    void thePlanetArrivesAtTheChosenOrbitWithItsConditionsAndTraits() {
        PlanetAPI placed = PhasedPlanetPlacement.place(mockSector.sector, system, star, record(null), 4000f, 90f);

        assertSame(planet, placed);
        verify(system).addPlanet("exiledSector_phased_run1", star, "Charted Space III", "frozen", 90f, 140f, 4000f,
                PhaseDestination.orbitDays(4000f));
        InOrder order = inOrder(market);
        order.verify(market).addCondition("cold");
        order.verify(market).addCondition("ore_moderate");
        order.verify(market).addCondition(PhasedPlanetTraits.PHASE_DISPLACED_CONDITION_ID);
        order.verify(market).addCondition("exiledSector_time_slipped_crust");
        order.verify(market).setSurveyLevel(SurveyLevel.FULL);
        order.verify(market).reapplyConditions();
        verify(market, never()).addCondition("removed_by_a_mod");
        verify(addedCondition).setSurveyed(true);
        verify(planet).addTag(PhasedPlanetPlacement.PHASED_TAG);
        verify(planetMemory).set(PhasedPlanetPlacement.ORIGIN_MEMORY_KEY, "Charted Space");
        verify(planetMemory).set(PhasedPlanetPlacement.REGION_MEMORY_KEY, "frozen_fringe");
        verify(planetMemory).set(PhasedPlanetPlacement.TIER_MEMORY_KEY, 12);
    }

    @Test
    void keptRuinsArriveAlreadyExplored() {
        MemoryAPI marketMemory = mock(MemoryAPI.class);
        when(market.getMemoryWithoutUpdate()).thenReturn(marketMemory);
        when(market.hasCondition(Conditions.RUINS_WIDESPREAD)).thenReturn(true);

        PhasedPlanetPlacement.place(mockSector.sector, system, star, record(null), 4000f, 0f);

        verify(marketMemory).set(PhasedPlanetPlacement.RUINS_EXPLORED_MEMORY_KEY, true);
    }

    @Test
    void aPlanetWithoutRuinsGetsNoRuinsFlag() {
        MemoryAPI marketMemory = mock(MemoryAPI.class);
        when(market.getMemoryWithoutUpdate()).thenReturn(marketMemory);

        PhasedPlanetPlacement.place(mockSector.sector, system, star, record(null), 4000f, 0f);

        verify(marketMemory, never()).set(eq(PhasedPlanetPlacement.RUINS_EXPLORED_MEMORY_KEY), any());
    }

    @Test
    void aRecordedTextureReplacesTheTypesOwn() {
        PhasedPlanetPlacement.place(mockSector.sector, system, star, record("graphics/planets/unknown_skies.jpg"), 4000f, 0f);

        verify(planetSpec).setTexture("graphics/planets/unknown_skies.jpg");
        verify(planet).applySpecChanges();
    }

    @Test
    void theTypesOwnTextureIsLeftAlone() {
        PhasedPlanetPlacement.place(mockSector.sector, system, star, record("graphics/planets/barren01.jpg"), 4000f, 0f);

        verify(planetSpec, never()).setTexture(anyString());
        verify(planet, never()).applySpecChanges();
    }

    @Test
    void aTakenPlanetIdGetsASuffix() {
        when(mockSector.sector.getEntityById("exiledSector_phased_run1")).thenReturn(mock(PlanetAPI.class));

        assertEquals("exiledSector_phased_run1_2", PhasedPlanetPlacement.uniquePlanetId(mockSector.sector, record(null)));
        PhasedPlanetPlacement.place(mockSector.sector, system, star, record(null), 4000f, 0f);
        verify(system).addPlanet(eq("exiledSector_phased_run1_2"), any(), anyString(), anyString(), anyFloat(), anyFloat(), anyFloat(),
                anyFloat());
    }
}
