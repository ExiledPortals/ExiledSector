package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InjunctionTest {

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    @Test
    void eachStackSlowsShipSystemRechargeByItsPercent() {
        ShipAPI target = ship(1);
        Injunction injunction = new Injunction(target);
        MutableShipStatsAPI stats = target.getMutableStats();

        injunction.gain(5f);
        verify(stats.getSystemCooldownBonus()).modifyMult(Injunction.MOD_ID, 1.05f);
        verify(stats.getSystemRegenBonus()).modifyMult(Injunction.MOD_ID, 1f / 1.05f);

        injunction.advance(Injunction.SECONDS_BETWEEN_STACKS);
        injunction.gain(5f);
        assertEquals(2, injunction.stacks());
        verify(stats.getSystemCooldownBonus()).modifyMult(Injunction.MOD_ID, 1.1f);
    }

    @Test
    void aShipGainsAtMostOneStackEachSecond() {
        Injunction injunction = new Injunction(ship(1));

        injunction.gain(5f);
        injunction.advance(Injunction.SECONDS_BETWEEN_STACKS / 2f);
        injunction.gain(5f);
        assertEquals(1, injunction.stacks());

        injunction.advance(Injunction.SECONDS_BETWEEN_STACKS / 2f);
        injunction.gain(5f);
        assertEquals(2, injunction.stacks());
    }

    @Test
    void steadyHitsBuildUpToTheMaximumStacks() {
        Injunction injunction = new Injunction(ship(1));
        for (int stack = 0; stack < Injunction.MAX_STACKS + 3; stack++) {
            injunction.gain(5f);
            injunction.advance(Injunction.SECONDS_BETWEEN_STACKS);
        }
        assertEquals(Injunction.MAX_STACKS - 1, injunction.stacks());
        injunction.gain(5f);
        assertEquals(Injunction.MAX_STACKS, injunction.stacks());
        assertEquals(5f * Injunction.MAX_STACKS, injunction.slowdownPercent(), 1e-4f);
    }

    @Test
    void expiredStacksLiftTheSlowdownAndTheDebuffRemovesItself() {
        ShipAPI target = ship(1);
        Injunction injunction = new Injunction(target);

        injunction.gain(5f);
        injunction.advance(Injunction.STACK_SECONDS);
        assertEquals(0, injunction.stacks());
        verify(target.getMutableStats().getSystemCooldownBonus()).unmodify(Injunction.MOD_ID);
        verify(target.getMutableStats().getSystemRegenBonus()).unmodify(Injunction.MOD_ID);
        verify(target).removeListener(injunction);
    }

    @Test
    void ownEnergyWeaponHitsOnEnemyShipsInflictInjunction() {
        ShipAPI owner = ship(0);
        when(owner.getMutableStats().getDynamic().getValue(InjunctionListener.SLOWDOWN_PERCENT_KEY, 0f)).thenReturn(5f);
        ShipAPI enemy = ship(1);
        when(enemy.getListeners(Injunction.class)).thenReturn(List.of());
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(WeaponAPI.WeaponType.ENERGY);
        when(weapon.getShip()).thenReturn(owner);
        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWeapon()).thenReturn(weapon);

        new InjunctionListener(owner).modifyDamageDealt(beam, enemy, mock(DamageAPI.class), new Vector2f(), true);

        verify(enemy).addListener(any(Injunction.class));
        verify(enemy.getMutableStats().getSystemCooldownBonus()).modifyMult(Injunction.MOD_ID, 1.05f);
    }

    @Test
    void otherWeaponTypesStrikeCraftAndAlliesInflictNothing() {
        ShipAPI owner = ship(0);
        when(owner.getMutableStats().getDynamic().getValue(InjunctionListener.SLOWDOWN_PERCENT_KEY, 0f)).thenReturn(5f);
        InjunctionListener listener = new InjunctionListener(owner);
        WeaponAPI ballistic = mock(WeaponAPI.class);
        when(ballistic.getType()).thenReturn(WeaponAPI.WeaponType.BALLISTIC);
        when(ballistic.getShip()).thenReturn(owner);
        DamagingProjectileAPI shell = mock(DamagingProjectileAPI.class);
        when(shell.getWeapon()).thenReturn(ballistic);
        WeaponAPI energy = mock(WeaponAPI.class);
        when(energy.getType()).thenReturn(WeaponAPI.WeaponType.ENERGY);
        when(energy.getShip()).thenReturn(owner);
        DamagingProjectileAPI bolt = mock(DamagingProjectileAPI.class);
        when(bolt.getWeapon()).thenReturn(energy);
        ShipAPI enemy = ship(1);
        ShipAPI enemyFighter = ship(1);
        when(enemyFighter.isFighter()).thenReturn(true);
        ShipAPI ally = ship(0);

        listener.modifyDamageDealt(shell, enemy, mock(DamageAPI.class), new Vector2f(), false);
        listener.modifyDamageDealt(bolt, enemyFighter, mock(DamageAPI.class), new Vector2f(), false);
        listener.modifyDamageDealt(bolt, ally, mock(DamageAPI.class), new Vector2f(), false);

        verify(enemy, never()).addListener(any());
        verify(enemyFighter, never()).addListener(any());
        verify(ally, never()).addListener(any());
    }
}
