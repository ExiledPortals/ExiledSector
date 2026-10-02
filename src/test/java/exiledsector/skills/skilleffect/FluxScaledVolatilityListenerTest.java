package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FluxScaledVolatilityListenerTest {

    private static final String MOD_ID = "exiledSector_fluxScaled_ship";

    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private FluxTrackerAPI flux;
    private MutableShipStatsAPI stats;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getElapsedInLastFrame()).thenReturn(0.016f);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);

        ship = mock(ShipAPI.class);
        when(ship.getId()).thenReturn("ship");
        when(ship.isAlive()).thenReturn(true);
        when(ship.getVelocity()).thenReturn(new Vector2f(100f, 0f));
        when(ship.getEngineController()).thenReturn(mock(ShipEngineControllerAPI.class));
        flux = mock(FluxTrackerAPI.class);
        when(ship.getFluxTracker()).thenReturn(flux);
        stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats()).thenReturn(stats);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(FluxScaledVolatilityListener.TOP_SPEED_KEY, 0f)).thenReturn(50f);
        when(dynamic.getValue(FluxScaledVolatilityListener.RATE_OF_FIRE_KEY, 0f)).thenReturn(40f);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static FluxTrackerAPI trackerAt(float level, boolean overloaded, boolean venting) {
        FluxTrackerAPI tracker = mock(FluxTrackerAPI.class);
        when(tracker.getFluxLevel()).thenReturn(level);
        when(tracker.isOverloaded()).thenReturn(overloaded);
        when(tracker.isVenting()).thenReturn(venting);
        return tracker;
    }

    @Test
    void theRatioRunsFromFullBonusAtNoFluxToFullPenaltyAtFullFlux() {
        assertEquals(1f, FluxScaledVolatilityListener.ratio(trackerAt(0f, false, false)), 1e-6f);
        assertEquals(0.5f, FluxScaledVolatilityListener.ratio(trackerAt(0.25f, false, false)), 1e-6f);
        assertEquals(0f, FluxScaledVolatilityListener.ratio(trackerAt(0.5f, false, false)), 1e-6f);
        assertEquals(-1f, FluxScaledVolatilityListener.ratio(trackerAt(1f, false, false)), 1e-6f);
    }

    @Test
    void ventingOrBeingOverloadedCountsAsFullFlux() {
        assertEquals(-1f, FluxScaledVolatilityListener.ratio(trackerAt(0.1f, true, false)), 1e-6f);
        assertEquals(-1f, FluxScaledVolatilityListener.ratio(trackerAt(0.1f, false, true)), 1e-6f);
    }

    @Test
    void atNoFluxTheShipGetsTheFullBonusAndTheLowFluxVisuals() {
        when(flux.getFluxLevel()).thenReturn(0f);
        FluxScaledVolatilityListener listener = new FluxScaledVolatilityListener(ship);

        listener.advance(0.016f);

        verify(stats.getMaxSpeed()).modifyFlat(MOD_ID, 50f);
        verify(stats.getAcceleration()).modifyPercent(MOD_ID, 50f);
        verify(stats.getTurnAcceleration()).modifyPercent(MOD_ID, 50f);
        verify(stats.getBallisticRoFMult()).modifyPercent(MOD_ID, 40f);
        verify(stats.getEnergyRoFMult()).modifyPercent(MOD_ID, 40f);
        verify(stats.getMissileRoFMult()).modifyPercent(MOD_ID, 40f);
        verify(ship.getEngineController()).fadeToOtherColor(eq(listener), any(Color.class), eq(null), eq(1f), eq(0.5f));
        verify(ship).addAfterimage(any(Color.class), anyFloat(), anyFloat(), eq(-80f), eq(-0f), anyFloat(), anyFloat(),
                anyFloat(), anyFloat(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void atFullFluxTheShipIsSlowedAndFiresSlowerButKeepsItsAgilityWithoutVisuals() {
        when(flux.getFluxLevel()).thenReturn(1f);
        FluxScaledVolatilityListener listener = new FluxScaledVolatilityListener(ship);

        listener.advance(0.016f);

        verify(stats.getMaxSpeed()).modifyFlat(MOD_ID, -50f);
        verify(stats.getAcceleration()).modifyPercent(MOD_ID, 0f);
        verify(stats.getBallisticRoFMult()).modifyPercent(MOD_ID, -40f);
        verify(ship.getEngineController(), never()).fadeToOtherColor(any(), any(), any(), anyFloat(), anyFloat());
        verify(ship, never()).addAfterimage(any(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                anyFloat(), anyFloat(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void statsAreOnlyReappliedWhenTheFluxStepChanges() {
        when(flux.getFluxLevel()).thenReturn(0.5f);
        FluxScaledVolatilityListener listener = new FluxScaledVolatilityListener(ship);
        MutableStat maxSpeed = stats.getMaxSpeed();

        listener.advance(0.016f);
        when(flux.getFluxLevel()).thenReturn(0.501f);
        listener.advance(0.016f);
        when(flux.getFluxLevel()).thenReturn(0.75f);
        listener.advance(0.016f);

        verify(maxSpeed, times(2)).modifyFlat(eq(MOD_ID), anyFloat());
        verify(maxSpeed).modifyFlat(MOD_ID, -25f);
    }

    @Test
    void thePlayerShipShowsAStatusThatIsADebuffAboveHalfFlux() {
        when(engine.getPlayerShip()).thenReturn(ship);
        when(flux.getFluxLevel()).thenReturn(0.75f);

        new FluxScaledVolatilityListener(ship).advance(0.016f);

        verify(engine).maintainStatusForPlayerShip(anyString(), anyString(), any(), any(), eq(true));
    }
}
