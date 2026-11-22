package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.RealityDisruptorChargeGlow.RDRepairRateDebuff;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HullHitRepairDisruptionListenerTest {

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("ship" + owner);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.isHulk()).thenReturn(false);
        when(ship.isFighter()).thenReturn(false);
        when(ship.getListeners(RDRepairRateDebuff.class)).thenReturn(List.of());
        return ship;
    }

    private static DamagingProjectileAPI shotFrom(ShipAPI ownerShip) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getShip()).thenReturn(ownerShip);
        DamagingProjectileAPI shot = mock(DamagingProjectileAPI.class);
        when(shot.getWeapon()).thenReturn(weapon);
        return shot;
    }

    private static HullHitRepairDisruptionListener listener(ShipAPI ownerShip, float chancePercent) {
        when(ownerShip.getMutableStats().getDynamic().getValue(HullHitRepairDisruptionListener.CHANCE_PERCENT_KEY, 0f)).thenReturn(chancePercent);
        return new HullHitRepairDisruptionListener(ownerShip, new Random(1));
    }

    @Test
    void aHullHitAtFullChanceDoublesTheTargetsRepairTime() {
        ShipAPI ownerShip = ship(0);
        ShipAPI target = ship(1);

        listener(ownerShip, 100f).modifyDamageDealt(shotFrom(ownerShip), target, mock(DamageAPI.class), new Vector2f(), false);

        verify(target).addListener(any(RDRepairRateDebuff.class));
        verify(target.getMutableStats().getCombatWeaponRepairTimeMult()).modifyMult(anyString(), anyFloat());
    }

    @Test
    void shieldHitsAndHitsOnAlliesDoNothing() {
        ShipAPI ownerShip = ship(0);
        ShipAPI enemy = ship(1);
        ShipAPI ally = ship(0);
        HullHitRepairDisruptionListener listener = listener(ownerShip, 100f);

        listener.modifyDamageDealt(shotFrom(ownerShip), enemy, mock(DamageAPI.class), new Vector2f(), true);
        listener.modifyDamageDealt(shotFrom(ownerShip), ally, mock(DamageAPI.class), new Vector2f(), false);

        verify(enemy, never()).addListener(any());
        verify(ally, never()).addListener(any());
    }

    @Test
    void aBeamRollsItsChanceForEachSecondOfContact() {
        DamageAPI beamTick = mock(DamageAPI.class);
        when(beamTick.isDps()).thenReturn(true);
        when(beamTick.getDpsDuration()).thenReturn(0.1f);

        assertEquals(2f, HullHitRepairDisruptionListener.chancePercentFor(beamTick, 20f), 0.0001f);
        assertEquals(20f, HullHitRepairDisruptionListener.chancePercentFor(mock(DamageAPI.class), 20f));
    }
}
