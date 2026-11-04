package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeflowAtTopSpeedTest {

    private static ShipAPI ship(float topSpeed, float speed) {
        return ship(topSpeed, topSpeed, speed);
    }

    private static ShipAPI ship(float unboostedTopSpeed, float boostedTopSpeed, float speed) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getMaxSpeedWithoutBoost()).thenReturn(unboostedTopSpeed);
        when(ship.getMaxSpeed()).thenReturn(boostedTopSpeed);
        when(ship.getVelocity()).thenReturn(new Vector2f(speed, 0f));
        return ship;
    }

    @Test
    void aShipCountsAsAtTopSpeedFromNinetyFivePercentOfIt() {
        assertTrue(MovementSkillEffect.isAtTopSpeed(ship(100f, 100f)));
        assertTrue(MovementSkillEffect.isAtTopSpeed(ship(100f, 95f)));
        assertFalse(MovementSkillEffect.isAtTopSpeed(ship(100f, 90f)));
        assertFalse(MovementSkillEffect.isAtTopSpeed(ship(0f, 0f)));
    }

    @Test
    void theZeroFluxBoostDoesNotRaiseTheTopSpeedToReach() {
        assertTrue(MovementSkillEffect.isAtTopSpeed(ship(100f, 150f, 95f)));
        assertFalse(MovementSkillEffect.isAtTopSpeed(ship(100f, 150f, 90f)));
    }

    @Test
    void timeflowRisesOnlyWhileAtTopSpeed() {
        ShipAPI cruising = ship(100f, 100f);
        ShipAPI turning = ship(100f, 40f);
        MutableStat cruisingTime = cruising.getMutableStats().getTimeMult();
        MutableStat turningTime = turning.getMutableStats().getTimeMult();

        MovementSkillEffect.TIMEFLOW_AT_TOP_SPEED_PERCENT.advanceInCombat(cruising, "frame", 4f);
        MovementSkillEffect.TIMEFLOW_AT_TOP_SPEED_PERCENT.advanceInCombat(turning, "frame", 4f);

        verify(cruisingTime).modifyPercent("frame", 4f);
        verify(turningTime).modifyPercent("frame", 0f);
    }
}
