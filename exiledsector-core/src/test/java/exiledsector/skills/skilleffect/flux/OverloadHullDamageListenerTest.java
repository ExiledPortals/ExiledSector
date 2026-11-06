package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.skills.skilleffect.HullLoss;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OverloadHullDamageListenerTest {

    private static final int OWN_SIDE = 0;
    private static final int ENEMY_SIDE = 1;

    private final float[] hitpoints = {800f};
    private final float[] fluxLevels = {1000f, 400f};
    private FluxTrackerAPI flux;
    private DynamicStatsAPI dynamic;
    private ShipAPI ship;
    private CombatEngineAPI engine;
    private OverloadHullDamageListener listener;

    @BeforeEach
    void setUp() {
        flux = mock(FluxTrackerAPI.class);
        when(flux.getCurrFlux()).thenAnswer(call -> fluxLevels[0]);
        when(flux.getHardFlux()).thenAnswer(call -> fluxLevels[1]);
        doAnswer(call -> {
            fluxLevels[0] = call.getArgument(0);
            return null;
        }).when(flux).setCurrFlux(anyFloat());
        doAnswer(call -> {
            fluxLevels[1] = call.getArgument(0);
            return null;
        }).when(flux).setHardFlux(anyFloat());
        dynamic = mock(DynamicStatsAPI.class);
        ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getLocation()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
        when(ship.getOwner()).thenReturn(OWN_SIDE);
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
        engine = mock(CombatEngineAPI.class);
        listener = new OverloadHullDamageListener(ship);
    }

    private void advanceOneFrame() {
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            listener.advance(0.016f);
        }
    }

    private static ShipAPI shipOnSide(int owner) {
        ShipAPI other = mock(ShipAPI.class);
        when(other.getOwner()).thenReturn(owner);
        return other;
    }

    private void hitBy(Object source) {
        listener.reportDamageApplied(source, ship, mock(ApplyDamageResultAPI.class));
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
        when(dynamic.getValue(OverloadHullDamageListener.HARD_FLUX_DISSIPATION_PERCENT_KEY, 0f)).thenReturn(50f);

        advanceOneFrame();

        verify(flux, never()).stopOverload();
        assertEquals(800f, hitpoints[0], 0.001f);
        assertEquals(400f, fluxLevels[1], 0.001f);
    }

    @Test
    void aReplacedOverloadDissipatesAShareOfHardFluxOnly() {
        when(flux.isOverloaded()).thenReturn(true);
        when(dynamic.getValue(OverloadHullDamageListener.HARD_FLUX_DISSIPATION_PERCENT_KEY, 0f)).thenReturn(50f);

        advanceOneFrame();

        assertEquals(200f, fluxLevels[1], 0.001f);
        assertEquals(800f, fluxLevels[0], 0.001f);
    }

    @Test
    void withoutTheDissipationShareFluxIsLeftAlone() {
        when(flux.isOverloaded()).thenReturn(true);

        advanceOneFrame();

        assertEquals(400f, fluxLevels[1], 0.001f);
        assertEquals(1000f, fluxLevels[0], 0.001f);
    }

    @Test
    void anOverloadThatWouldEmptyTheHullEndsInAKillingBlowFromTheLastEnemyToHitIt() {
        ShipAPI firstEnemy = shipOnSide(ENEMY_SIDE);
        ShipAPI lastEnemy = shipOnSide(ENEMY_SIDE);
        hitBy(firstEnemy);
        hitBy(lastEnemy);
        hitBy(shipOnSide(OWN_SIDE));
        hitpoints[0] = 50f;
        when(flux.isOverloaded()).thenReturn(true);

        advanceOneFrame();

        assertEquals(HullLoss.KILLING_BLOW_HULL_LEFT, hitpoints[0], 0.001f);
        verify(engine).applyDamage(same(ship), any(Vector2f.class), anyFloat(), any(DamageType.class), anyFloat(), anyBoolean(), anyBoolean(),
                same(lastEnemy));
    }

    @Test
    void withNoEnemyDamageTheKillingBlowHasNoSource() {
        hitBy(shipOnSide(OWN_SIDE));
        hitpoints[0] = 50f;
        when(flux.isOverloaded()).thenReturn(true);

        advanceOneFrame();

        verify(engine).applyDamage(same(ship), any(Vector2f.class), anyFloat(), any(DamageType.class), anyFloat(), anyBoolean(), anyBoolean(),
                isNull());
    }

    @Test
    void strikeCraftDamageIsCreditedToTheirCarrier() {
        ShipAPI carrier = shipOnSide(ENEMY_SIDE);
        ShipAPI fighter = shipOnSide(ENEMY_SIDE);
        FighterWingAPI wing = mock(FighterWingAPI.class);
        when(fighter.isFighter()).thenReturn(true);
        when(fighter.getWing()).thenReturn(wing);
        when(wing.getSourceShip()).thenReturn(carrier);

        hitBy(fighter);

        assertSame(carrier, listener.lastEnemyAttacker());
    }

    @Test
    void aProjectileHitIsCreditedToTheShipThatFiredIt() {
        ShipAPI shooter = shipOnSide(ENEMY_SIDE);
        DamagingProjectileAPI missile = mock(DamagingProjectileAPI.class);
        when(missile.getSource()).thenReturn(shooter);
        when(ship.getParamAboutToApplyDamage()).thenReturn(missile);

        hitBy(null);

        assertSame(shooter, listener.lastEnemyAttacker());
    }

    @Test
    void damageToAnotherShipIsIgnored() {
        listener.reportDamageApplied(shipOnSide(ENEMY_SIDE), mock(CombatEntityAPI.class), mock(ApplyDamageResultAPI.class));

        assertNull(listener.lastEnemyAttacker());
    }
}
