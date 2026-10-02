package exiledsector.skills.skilleffect;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryGatingSupportTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "BALLISTIC_WEAPON_DAMAGE_PER_BURN_LEVEL_PERCENT",
            "ENERGY_WEAPON_RANGE_PER_SENSOR_STRENGTH_FLAT",
            "FIGHTER_BAYS_FLAT",
            "ARMOR_FLAT_FOR_LOW_BASE_ARMOR",
            "HULL_PERCENT",
            "ARMOR_PERCENT",
            "SHIELD_ARC_PERCENT",
            "CARGO_CAPACITY_FLAT",
            "SHIP_RECOVERY_CHANCE_BONUS"
    })
    void effectsTheGameCannotUndoMidBattleDoNotClaimTemporaryGating(String name) {
        SkillEffect effect = SkillEffect.byName(name);

        assertNotNull(effect, name);
        assertFalse(effect.supportsTemporaryGating(), name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "TOP_SPEED_PERCENT",
            "ACCELERATION_PERCENT",
            "FLUX_CAPACITY_PERCENT",
            "FLUX_DISSIPATION_FLAT",
            "PEAK_CR_DURATION_PERCENT",
            "SHIELD_DAMAGE_TAKEN_MULT",
            "ARMOR_DAMAGE_TAKEN_MULT_PER_DMOD",
            "WEAPON_DAMAGE_MULT_PER_DMOD",
            "FIGHTER_REFIT_TIME_MULT"
    })
    void effectsOnStatsTheGameKeepsReadingInCombatOptIn(String name) {
        SkillEffect effect = SkillEffect.byName(name);

        assertNotNull(effect, name);
        assertTrue(effect.supportsTemporaryGating(), name);
    }

    @ParameterizedTest
    @EnumSource(WeaponScope.class)
    void weaponAmmoIsSetWhenTheShipIsBuiltSoItCannotBeGatedAtAnyScope(WeaponScope scope) {
        ScopedWeaponEffect effect = ScopedWeaponEffect.find(WeaponStatFamily.AMMO, scope, StatMode.PERCENT);

        if (effect != null) {
            assertFalse(effect.supportsTemporaryGating(), effect.name());
        }
    }

    @ParameterizedTest
    @EnumSource(value = WeaponScope.class, names = {"ALL", "BALLISTIC", "ENERGY", "MISSILE"})
    void weaponDamageCanBeGatedAtEveryTypedScope(WeaponScope scope) {
        ScopedWeaponEffect effect = ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, scope, StatMode.PERCENT);

        assertNotNull(effect, scope.name());
        assertTrue(effect.supportsTemporaryGating(), effect.name());
    }
}
