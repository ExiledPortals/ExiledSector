package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DamageTakenWhileRetreatingTest {

    private static final float EPSILON = 1e-4f;

    private record DamageTaken(MutableStat hull, MutableStat armor, MutableStat shield) {
    }

    private static DamageTaken damageTakenOf(ShipAPI ship) {
        DamageTaken damageTaken = new DamageTaken(new MutableStat(1f), new MutableStat(1f), new MutableStat(1f));
        when(ship.getMutableStats().getHullDamageTakenMult()).thenReturn(damageTaken.hull());
        when(ship.getMutableStats().getArmorDamageTakenMult()).thenReturn(damageTaken.armor());
        when(ship.getMutableStats().getShieldDamageTakenMult()).thenReturn(damageTaken.shield());
        return damageTaken;
    }

    @Test
    void hullArmorAndShieldDamageTakenAreReducedOnlyWhileRetreating() {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        DamageTaken damageTaken = damageTakenOf(ship);
        DefenseSkillEffect effect = DefenseSkillEffect.DAMAGE_TAKEN_WHILE_RETREATING_PERCENT;
        assertTrue(effect.lowerIsBetter());

        when(ship.isRetreating()).thenReturn(true);
        effect.advanceInCombat(ship, "retreat", -30f);
        assertEquals(0.7f, damageTaken.hull().getModifiedValue(), EPSILON);
        assertEquals(0.7f, damageTaken.armor().getModifiedValue(), EPSILON);
        assertEquals(0.7f, damageTaken.shield().getModifiedValue(), EPSILON);

        when(ship.isRetreating()).thenReturn(false);
        effect.advanceInCombat(ship, "retreat", -30f);
        assertEquals(1f, damageTaken.hull().getModifiedValue(), EPSILON);
        assertEquals(1f, damageTaken.shield().getModifiedValue(), EPSILON);
    }
}
