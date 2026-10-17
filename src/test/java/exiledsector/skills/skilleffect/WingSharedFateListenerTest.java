package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WingSharedFateListenerTest {

    private static ShipAPI fighter(float hitpoints) {
        ShipAPI fighter = mock(ShipAPI.class);
        when(fighter.isAlive()).thenReturn(true);
        when(fighter.getHitpoints()).thenReturn(hitpoints);
        when(fighter.getMaxHitpoints()).thenReturn(hitpoints);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(flux.getMaxFlux()).thenReturn(FLUX_CAPACITY);
        when(fighter.getFluxTracker()).thenReturn(flux);
        when(fighter.getLocation()).thenReturn(new Vector2f());
        return fighter;
    }

    private static final float FLUX_CAPACITY = 200f;

    private record Hit(WingSharedFateListener listener, ShipAPI hitFighter, Object projectile) {
        void lands(float shieldFlux, float hullDamage) {
            DamageAPI damage = mock(DamageAPI.class);
            when(damage.getModifier()).thenReturn(mock(MutableStat.class));
            when(hitFighter.getParamAboutToApplyDamage()).thenReturn(projectile);
            ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
            when(result.getDamageToShields()).thenReturn(shieldFlux);
            when(result.getDamageToHull()).thenReturn(hullDamage);
            listener.modifyDamageTaken(projectile, hitFighter, damage, new Vector2f(), shieldFlux > 0f);
            listener.reportDamageApplied(projectile, hitFighter, result);
        }
    }

    @Test
    void aHullHitAtLeastTheStruckFightersMaxHullIsNotShared() {
        ShipAPI hit = fighter(100f);
        ShipAPI wingmate = fighter(100f);
        ShipAPI otherWingmate = fighter(100f);
        wingOf(hit, wingmate, otherWingmate);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(0f, 40f);

        verify(wingmate, never()).setHitpoints(anyFloat());
        verify(otherWingmate, never()).setHitpoints(anyFloat());
    }

    @Test
    void aShieldHitAtLeastTheStruckFightersFluxCapacityIsNotShared() {
        ShipAPI hit = fighter(100f);
        ShipAPI wingmate = fighter(100f);
        wingOf(hit, wingmate);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(100f, 0f);

        verify(wingmate.getFluxTracker(), never()).increaseFlux(anyFloat(), eq(true));
        verify(hit.getFluxTracker()).increaseFlux(about(100f, 0.001f), eq(true));
    }

    @Test
    void aHitSmallerThanTheStruckFightersMaxHullIsShared() {
        ShipAPI hit = fighter(100f);
        ShipAPI wingmate = fighter(100f);
        wingOf(hit, wingmate);

        new Hit(attachedTo(hit, 100f), hit, new Object()).lands(0f, 45f);

        verify(wingmate).setHitpoints(about(55f, 0.001f));
    }

    private static List<ShipAPI> wingOf(ShipAPI... members) {
        List<ShipAPI> wingMembers = new ArrayList<>(List.of(members));
        FighterWingAPI wing = mock(FighterWingAPI.class);
        when(wing.getWingMembers()).thenReturn(wingMembers);
        for (ShipAPI member : members) {
            when(member.getWing()).thenReturn(wing);
        }
        return wingMembers;
    }

    private static WingSharedFateListener attachedTo(ShipAPI fighter, float sharedPercent) {
        ArgumentCaptor<Object> listener = ArgumentCaptor.forClass(Object.class);
        WingSharedFateListener.attach(fighter, sharedPercent);
        verify(fighter).addListener(listener.capture());
        return (WingSharedFateListener) listener.getValue();
    }

    @Test
    void sharingEverythingSplitsEachHitEvenlyAcrossTheWing() {
        WingSharedFateListener.Split split = WingSharedFateListener.split(100f, 2);

        assertEquals(1f / 3f, split.keptFraction(), 0.0001f);
        assertEquals(1f, split.perWingmateRatio(), 0.0001f, "each wingmate takes exactly what the hit fighter takes");
    }

    @Test
    void sharingHalfLeavesTheHitFighterWithMostOfTheDamage() {
        WingSharedFateListener.Split split = WingSharedFateListener.split(50f, 1);

        assertEquals(0.75f, split.keptFraction(), 0.0001f);
        assertEquals(1f / 3f, split.perWingmateRatio(), 0.0001f);
    }

    @Test
    void aLoneFighterKeepsTheWholeHit() {
        assertEquals(new WingSharedFateListener.Split(1f, 0f), WingSharedFateListener.split(100f, 0));
    }

    @Test
    void aHitIsCutDownAndItsShieldAndHullDamagePassedToEveryWingmate() {
        ShipAPI hit = fighter(300f);
        ShipAPI near = fighter(300f);
        ShipAPI far = fighter(300f);
        wingOf(hit, near, far);
        WingSharedFateListener listener = attachedTo(hit, 100f);
        DamageAPI damage = mock(DamageAPI.class);
        MutableStat modifier = mock(MutableStat.class);
        when(damage.getModifier()).thenReturn(modifier);
        Object projectile = new Object();
        when(hit.getParamAboutToApplyDamage()).thenReturn(projectile);
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getDamageToShields()).thenReturn(40f);
        when(result.getDamageToHull()).thenReturn(30f);

        assertEquals(WingSharedFateListener.SHARE_MOD_ID, listener.modifyDamageTaken(projectile, hit, damage, new Vector2f(), true));
        listener.reportDamageApplied(projectile, hit, result);

        verify(modifier).modifyMult(eq(WingSharedFateListener.SHARE_MOD_ID), about(1f / 3f, 0.0001f));
        for (ShipAPI wingmate : List.of(near, far)) {
            verify(wingmate.getFluxTracker()).increaseFlux(about(40f, 0.001f), eq(true));
            verify(wingmate).setHitpoints(about(270f, 0.001f));
        }
    }

    @Test
    void aFighterWhoseWingmatesAreGoneTakesTheHitAlone() {
        ShipAPI hit = fighter(300f);
        ShipAPI destroyed = fighter(0f);
        when(destroyed.isAlive()).thenReturn(false);
        wingOf(hit, destroyed);
        WingSharedFateListener listener = attachedTo(hit, 100f);
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));

        assertNull(listener.modifyDamageTaken(new Object(), hit, damage, new Vector2f(), false));
        verify(damage.getModifier(), never()).modifyMult(any(), anyFloat());
    }

    private static float about(float value, float tolerance) {
        return org.mockito.AdditionalMatchers.eq(value, tolerance);
    }
}
