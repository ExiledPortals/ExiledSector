package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipFramePointTest {

    @Test
    void aPointOnAShipMovesAndTurnsWithIt() {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getLocation()).thenReturn(new Vector2f(10f, 20f));
        when(ship.getFacing()).thenReturn(0f);
        ShipFramePoint point = ShipFramePoint.of(ship, new Vector2f(15f, 20f));

        when(ship.getLocation()).thenReturn(new Vector2f(-40f, 60f));
        when(ship.getFacing()).thenReturn(180f);
        Vector2f world = point.worldPoint();

        assertEquals(-45f, world.x, 1e-3f);
        assertEquals(60f, world.y, 1e-3f);
    }
}
