package exiledsector.ui.slipstream;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlipstreamRouteTest {

    private static final SlipstreamRoute ELBOW = new SlipstreamRoute(List.of(new Vector2f(0f, 0f), new Vector2f(100f, 0f), new Vector2f(100f, 50f)), 20f);

    @Test
    void lengthSumsTheLegs() {
        assertEquals(150f, ELBOW.length(), 0.001f);
    }

    @Test
    void pointsAreSampledAlongTheLegs() {
        assertEquals(new Vector2f(40f, 0f), ELBOW.pointAt(40f));
        assertEquals(new Vector2f(100f, 25f), ELBOW.pointAt(125f));
        assertEquals(new Vector2f(100f, 50f), ELBOW.pointAt(500f));
        assertEquals(new Vector2f(0f, 0f), ELBOW.pointAt(-10f));
    }

    @Test
    void offsetsAreSidewaysToTheHeading() {
        Vector2f out = new Vector2f();
        ELBOW.offsetPoint(40f, 10f, out);
        assertEquals(new Vector2f(40f, 10f), out);
        ELBOW.offsetPoint(125f, 10f, out);
        assertEquals(new Vector2f(90f, 25f), out);
    }

    @Test
    void aRouteNeedsTwoPoints() {
        List<Vector2f> single = List.of(new Vector2f());
        assertThrows(IllegalArgumentException.class, () -> new SlipstreamRoute(single, 10f));
    }
}
