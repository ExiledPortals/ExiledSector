package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.defense.DefenseSkillEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BallisticAsEnergyTest {

    private static ShipAPI ship(boolean ballisticCountsAsEnergy) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        StatBonus flag = new StatBonus();
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(BallisticAsEnergy.KEY)).thenReturn(flag);
        if (ballisticCountsAsEnergy) {
            WeaponSkillEffect.BALLISTIC_COUNTS_AS_ENERGY.apply(stats, "fafnir", 0f);
        }
        return ship;
    }

    private static WeaponAPI weapon(WeaponAPI.WeaponType type, ShipAPI ship) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(type);
        when(weapon.getShip()).thenReturn(ship);
        return weapon;
    }

    @Test
    void ballisticWeaponsCountAsEnergyOnlyOnAShipWithTheFlag() {
        ShipAPI flagged = ship(true);
        ShipAPI plain = ship(false);

        assertTrue(BallisticAsEnergy.active(flagged.getMutableStats()));
        assertFalse(BallisticAsEnergy.active(plain.getMutableStats()));
        assertTrue(BallisticAsEnergy.countsAsEnergy(weapon(WeaponAPI.WeaponType.BALLISTIC, flagged)));
        assertFalse(BallisticAsEnergy.countsAsEnergy(weapon(WeaponAPI.WeaponType.BALLISTIC, plain)));
        assertFalse(BallisticAsEnergy.countsAsEnergy(weapon(WeaponAPI.WeaponType.BALLISTIC, null)));
        assertTrue(BallisticAsEnergy.countsAsEnergy(weapon(WeaponAPI.WeaponType.ENERGY, plain)));
        assertFalse(BallisticAsEnergy.countsAsEnergy(weapon(WeaponAPI.WeaponType.MISSILE, flagged)));
        assertFalse(BallisticAsEnergy.countsAsEnergy(null));
    }

    @Test
    void energyAndNonBeamEnergyEffectsHaveABallisticCounterpartButBeamAndAllWeaponEffectsDont() {
        assertEquals(ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.BALLISTIC, StatMode.PERCENT),
                BallisticAsEnergy.ballisticCounterpart(ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.ENERGY, StatMode.PERCENT)));
        assertEquals(ScopedWeaponEffect.find(WeaponStatFamily.RANGE, WeaponScope.BALLISTIC, StatMode.FLAT),
                BallisticAsEnergy.ballisticCounterpart(ScopedWeaponEffect.find(WeaponStatFamily.RANGE, WeaponScope.NON_BEAM_ENERGY, StatMode.FLAT)));
        assertEquals(ScopedWeaponEffect.find(WeaponStatFamily.FLUX_COST, WeaponScope.BALLISTIC, StatMode.MULT),
                BallisticAsEnergy.ballisticCounterpart(ScopedWeaponEffect.find(WeaponStatFamily.FLUX_COST, WeaponScope.NON_BEAM_ENERGY, StatMode.MULT)));
        assertNull(BallisticAsEnergy.ballisticCounterpart(ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.BEAM, StatMode.PERCENT)));
        assertNull(BallisticAsEnergy.ballisticCounterpart(ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.ALL, StatMode.PERCENT)));
        assertNull(BallisticAsEnergy.ballisticCounterpart(ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.BALLISTIC, StatMode.PERCENT)));
        assertNull(BallisticAsEnergy.ballisticCounterpart(DefenseSkillEffect.ARMOR_PERCENT));
    }

    @Test
    void everyEnergyScopedWeaponEffectHasABallisticCounterpart() {
        for (ScopedWeaponEffect effect : ScopedWeaponEffect.all()) {
            if (effect.scope() == WeaponScope.ENERGY || effect.scope() == WeaponScope.NON_BEAM_ENERGY) {
                ScopedWeaponEffect counterpart = BallisticAsEnergy.ballisticCounterpart(effect);
                assertEquals(WeaponScope.BALLISTIC, counterpart == null ? null : counterpart.scope(), effect.name());
                assertEquals(effect.family(), counterpart.family(), effect.name());
                assertEquals(effect.mode(), counterpart.mode(), effect.name());
            }
        }
    }
}
