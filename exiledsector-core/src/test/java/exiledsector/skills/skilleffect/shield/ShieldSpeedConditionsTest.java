package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShieldSpeedConditionsTest {

    private static ShipAPI moving(float velocityX, float velocityY, float topSpeed) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getVelocity()).thenReturn(new Vector2f(velocityX, velocityY));
        when(ship.getMaxSpeedWithoutBoost()).thenReturn(topSpeed);
        when(ship.getMaxSpeed()).thenReturn(topSpeed * 2f);
        return ship;
    }

    @Test
    void movingFasterThanTheShareOfUnboostedTopSpeedCountsInAnyDirection() {
        assertTrue(ShieldSkillEffect.isMovingFast(moving(80f, 0f, 100f)));
        assertTrue(ShieldSkillEffect.isMovingFast(moving(-60f, -60f, 100f)));
        assertFalse(ShieldSkillEffect.isMovingFast(moving(75f, 0f, 100f)));
        assertFalse(ShieldSkillEffect.isMovingFast(moving(10f, 10f, 100f)));
    }

    @Test
    void movingSlowerThanTheShareOfUnboostedTopSpeedCountsInAnyDirection() {
        assertTrue(ShieldSkillEffect.isMovingSlowly(moving(5f, 0f, 100f)));
        assertTrue(ShieldSkillEffect.isMovingSlowly(moving(-6f, 6f, 100f)));
        assertFalse(ShieldSkillEffect.isMovingSlowly(moving(10f, 0f, 100f)));
        assertFalse(ShieldSkillEffect.isMovingSlowly(moving(0f, -40f, 100f)));
    }

    @Test
    void aShipThatCannotMoveNeverCounts() {
        assertFalse(ShieldSkillEffect.isMovingFast(moving(5f, 0f, 0f)));
        assertFalse(ShieldSkillEffect.isMovingSlowly(moving(0f, 0f, 0f)));
    }
}
