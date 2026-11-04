package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopSpeedShareTest {

    private static ShipAPI moving(float velocityX, float velocityY) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getVelocity()).thenReturn(new Vector2f(velocityX, velocityY));
        return ship;
    }

    @Test
    void theShareOfTopSpeedItselfIsAtLeastButNeitherAboveNorBelow() {
        ShipAPI atHalfSpeed = moving(0f, -50f);

        assertTrue(TopSpeedShare.isAtLeast(atHalfSpeed, 100f, 0.5f));
        assertFalse(TopSpeedShare.isAbove(atHalfSpeed, 100f, 0.5f));
        assertFalse(TopSpeedShare.isBelow(atHalfSpeed, 100f, 0.5f));
    }

    @Test
    void speedCountsInAnyDirection() {
        assertTrue(TopSpeedShare.isAbove(moving(-40f, 40f), 100f, 0.5f));
        assertTrue(TopSpeedShare.isBelow(moving(30f, -30f), 100f, 0.5f));
    }

    @Test
    void noTopSpeedNeverCounts() {
        ShipAPI stopped = moving(0f, 0f);

        assertFalse(TopSpeedShare.isAtLeast(stopped, 0f, 0.5f));
        assertFalse(TopSpeedShare.isBelow(stopped, 0f, 0.5f));
        assertFalse(TopSpeedShare.isAbove(moving(10f, 0f), 0f, 0.5f));
    }
}
