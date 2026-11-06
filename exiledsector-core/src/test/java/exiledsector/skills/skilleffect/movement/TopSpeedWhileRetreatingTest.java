package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopSpeedWhileRetreatingTest {

    private static ShipAPI ship(boolean retreating) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.isRetreating()).thenReturn(retreating);
        return ship;
    }

    @Test
    void topSpeedRisesOnlyWhileRetreating() {
        ShipAPI retreating = ship(true);
        ShipAPI fighting = ship(false);
        MutableStat retreatingSpeed = retreating.getMutableStats().getMaxSpeed();
        MutableStat fightingSpeed = fighting.getMutableStats().getMaxSpeed();

        MovementSkillEffect.TOP_SPEED_WHILE_RETREATING_PERCENT.advanceInCombat(retreating, "retreat", 30f);
        MovementSkillEffect.TOP_SPEED_WHILE_RETREATING_PERCENT.advanceInCombat(fighting, "retreat", 30f);

        verify(retreatingSpeed).modifyPercent("retreat", 30f);
        verify(fightingSpeed).modifyPercent("retreat", 0f);
    }
}
