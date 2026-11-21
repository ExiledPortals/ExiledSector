package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorGhostRendererTest {

    private static final float SCATTER_RADIUS_RATIO = 0.35f;

    @Test
    void eachKeyGetsTheConfiguredNumberOfGhosts() {
        SensorGhostRenderer renderer = new SensorGhostRenderer(7, 0.3f, SCATTER_RADIUS_RATIO);

        assertEquals(7, renderer.ghosts("officer_1").size());
        assertEquals(0.3f, renderer.ghostSizeRatio());
    }

    @Test
    void ghostsAreTheSameForAKeyAndCachedPerKey() {
        SensorGhostRenderer renderer = new SensorGhostRenderer(7, 0.3f, SCATTER_RADIUS_RATIO);
        SensorGhostRenderer other = new SensorGhostRenderer(7, 0.3f, SCATTER_RADIUS_RATIO);

        List<SensorGhostRenderer.Ghost> ghosts = renderer.ghosts("officer_1");

        assertSame(ghosts, renderer.ghosts("officer_1"));
        assertEquals(ghosts, other.ghosts("officer_1"));
        assertNotEquals(ghosts, renderer.ghosts("officer_2"));
    }

    @Test
    void ghostsStayInsideTheScatterRadius() {
        SensorGhostRenderer renderer = new SensorGhostRenderer(50, 0.3f, SCATTER_RADIUS_RATIO);

        for (SensorGhostRenderer.Ghost ghost : renderer.ghosts("node_42")) {
            float radius = (float) Math.hypot(ghost.offsetXFraction(), ghost.offsetYFraction());
            assertTrue(radius <= SCATTER_RADIUS_RATIO + 1e-5f);
            assertTrue(ghost.blinkPeriod() >= 1.2f && ghost.blinkPeriod() <= 2.6f);
        }
    }
}
