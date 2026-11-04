package exiledsector.skills.skilleffect.defense;

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

class HoldTheLineArmorTest {

    private static final float EPSILON = 1e-4f;

    private static ShipAPI ship(float speed, MutableStat armorDamageTaken) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getVelocity()).thenReturn(new Vector2f(speed, 0f));
        when(ship.getMaxSpeedWithoutBoost()).thenReturn(200f);
        when(ship.getMutableStats().getArmorDamageTakenMult()).thenReturn(armorDamageTaken);
        return ship;
    }

    @Test
    void armorDamageTakenIsReducedBelowAQuarterOfTopSpeedAndNormalAboveIt() {
        MutableStat armorDamageTaken = new MutableStat(1f);
        DefenseSkillEffect effect = DefenseSkillEffect.ARMOR_DAMAGE_TAKEN_AT_LOW_SPEED_PERCENT;

        assertTrue(effect.advancesInCombat());
        assertTrue(effect.lowerIsBetter());
        assertFalse(effect.invertsDescriptionColours());
        effect.advanceInCombat(ship(45f, armorDamageTaken), "mod_id", -15f);
        assertEquals(0.85f, armorDamageTaken.getModifiedValue(), EPSILON);

        effect.advanceInCombat(ship(55f, armorDamageTaken), "mod_id", -15f);
        assertEquals(1f, armorDamageTaken.getModifiedValue(), EPSILON);
    }
}
