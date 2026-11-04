package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HighSpeedTest {

    private static ShipAPI moving(float velocityX, float velocityY, float topSpeed) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getVelocity()).thenReturn(new Vector2f(velocityX, velocityY));
        when(ship.getMaxSpeedWithoutBoost()).thenReturn(topSpeed);
        return ship;
    }

    @Test
    void movingFasterThanTheShareOfTopSpeedCountsInAnyDirection() {
        assertTrue(HighSpeed.isMovingFast(moving(80f, 0f, 100f)));
        assertTrue(HighSpeed.isMovingFast(moving(-60f, -60f, 100f)));
        assertFalse(HighSpeed.isMovingFast(moving(75f, 0f, 100f)));
        assertFalse(HighSpeed.isMovingFast(moving(10f, 10f, 100f)));
    }

    @Test
    void aShipThatCannotMoveNeverCounts() {
        assertFalse(HighSpeed.isMovingFast(moving(5f, 0f, 0f)));
    }
}
