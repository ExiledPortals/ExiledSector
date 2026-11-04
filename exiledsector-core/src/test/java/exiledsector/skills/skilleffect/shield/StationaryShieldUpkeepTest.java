package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StationaryShieldUpkeepTest {

    private static final float EPSILON = 1e-4f;

    private static ShipAPI ship(float speed, MutableStat shieldUpkeep) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getVelocity()).thenReturn(new Vector2f(speed, 0f));
        when(ship.getMaxSpeedWithoutBoost()).thenReturn(200f);
        when(ship.getMutableStats().getShieldUpkeepMult()).thenReturn(shieldUpkeep);
        return ship;
    }

    @Test
    void shieldUpkeepIsReducedBelowTenPercentOfTopSpeedAndNormalAboveIt() {
        MutableStat shieldUpkeep = new MutableStat(1f);
        ShieldSkillEffect effect = ShieldSkillEffect.SHIELD_UPKEEP_AT_LOW_SPEED_PERCENT;

        assertTrue(effect.advancesInCombat());
        assertTrue(effect.lowerIsBetter());
        effect.advanceInCombat(ship(15f, shieldUpkeep), "mod_id", -35f);
        assertEquals(0.65f, shieldUpkeep.getModifiedValue(), EPSILON);

        effect.advanceInCombat(ship(25f, shieldUpkeep), "mod_id", -35f);
        assertEquals(1f, shieldUpkeep.getModifiedValue(), EPSILON);
    }

    @Test
    void speedDependentShieldReductionsKeepTheirWrittenColours() {
        for (ShieldSkillEffect effect : new ShieldSkillEffect[]{ShieldSkillEffect.SHIELD_UPKEEP_AT_LOW_SPEED_PERCENT,
                ShieldSkillEffect.SHIELD_DAMAGE_TAKEN_AT_SPEED_PERCENT}) {
            assertTrue(effect.lowerIsBetter(), effect.name());
            assertFalse(effect.invertsDescriptionColours(), effect.name());
        }
    }
}
