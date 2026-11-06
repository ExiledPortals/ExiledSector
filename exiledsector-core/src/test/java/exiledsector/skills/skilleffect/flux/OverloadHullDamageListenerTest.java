package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.skills.skilleffect.HullLoss;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OverloadHullDamageListenerTest {

    private final float[] hitpoints = {800f};
    private FluxTrackerAPI flux;
    private DynamicStatsAPI dynamic;
    private ShipAPI ship;

    @BeforeEach
    void setUp() {
        flux = mock(FluxTrackerAPI.class);
        dynamic = mock(DynamicStatsAPI.class);
        ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getLocation()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(OverloadHullDamageListener.HULL_DAMAGE_PERCENT_KEY, 0f)).thenReturn(10f);
        when(ship.getFluxTracker()).thenReturn(flux);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenAnswer(call -> hitpoints[0]);
        doAnswer(call -> {
            hitpoints[0] = call.getArgument(0);
            return null;
        }).when(ship).setHitpoints(anyFloat());
    }

    private void advanceOneFrame() {
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(mock(CombatEngineAPI.class));
            new OverloadHullDamageListener(ship).advance(0.016f);
        }
    }

    @Test
    void anOverloadIsStoppedAndCostsAShareOfMaximumHull() {
        when(flux.isOverloaded()).thenReturn(true);

        advanceOneFrame();

        verify(flux).stopOverload();
        assertEquals(700f, hitpoints[0], 0.001f);
    }

    @Test
    void nothingHappensWhileTheShipIsNotOverloaded() {
        advanceOneFrame();

        verify(flux, never()).stopOverload();
        assertEquals(800f, hitpoints[0], 0.001f);
    }

    @Test
    void withoutTheEffectAnOverloadRunsItsCourse() {
        when(flux.isOverloaded()).thenReturn(true);
        when(dynamic.getValue(OverloadHullDamageListener.HULL_DAMAGE_PERCENT_KEY, 0f)).thenReturn(0f);

        advanceOneFrame();

        verify(flux, never()).stopOverload();
        assertEquals(800f, hitpoints[0], 0.001f);
    }

    @Test
    void anOverloadThatWouldEmptyTheHullEndsInAKillingBlow() {
        hitpoints[0] = 50f;
        when(flux.isOverloaded()).thenReturn(true);

        advanceOneFrame();

        verify(flux).stopOverload();
        assertEquals(HullLoss.KILLING_BLOW_HULL_LEFT, hitpoints[0], 0.001f);
    }
}
