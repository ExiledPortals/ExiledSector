package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketArcsTest {

    private static final float STEP = 0.016f;
    private static final float EPSILON = 1e-4f;

    @Test
    void thePerimeterRunsAnticlockwiseAroundTheSquareFromTheBottomLeftCorner() {
        float[][] expected = {{-1f, -1f}, {0f, -1f}, {1f, -1f}, {1f, 0f}, {1f, 1f}, {0f, 1f}, {-1f, 1f}, {-1f, 0f}};
        for (int i = 0; i < expected.length; i++) {
            float position = i / 2f;
            assertEquals(expected[i][0], SocketArcs.perimeterX(position), EPSILON, "x at " + position);
            assertEquals(expected[i][1], SocketArcs.perimeterY(position), EPSILON, "y at " + position);
        }
        assertEquals(SocketArcs.perimeterX(0.3f), SocketArcs.perimeterX(4.3f), EPSILON);
        assertEquals(SocketArcs.perimeterY(-0.5f), SocketArcs.perimeterY(3.5f), EPSILON);
    }

    @Test
    void arcsHugTheBorderAndNeverExceedTheConcurrentLimit() {
        SocketArcs arcs = new SocketArcs(new Random(7));
        int seen = 0;
        for (int frame = 0; frame < 2000; frame++) {
            arcs.advance(STEP);
            List<SocketArcs.Arc> current = arcs.arcs("socket");
            assertTrue(current.size() <= SocketArcs.MAX_ARCS);
            for (SocketArcs.Arc arc : current) {
                seen++;
                assertTrue(arc.alpha() > 0f && arc.alpha() <= 1f);
                for (int i = 0; i < arc.pointCount(); i++) {
                    float reach = Math.max(Math.abs(arc.x(i)), Math.abs(arc.y(i)));
                    assertTrue(reach <= 1f - SocketArcs.BORDER_INSET + SocketArcs.JITTER + EPSILON, "point outside the frame: " + reach);
                    assertTrue(reach >= 1f - SocketArcs.BORDER_INSET - SocketArcs.JITTER - EPSILON, "point too far inside: " + reach);
                }
            }
        }
        assertTrue(seen > 0);
    }

    @Test
    void aSocketThatStopsBeingDrawnLosesItsArcs() {
        SocketArcs arcs = new SocketArcs(new Random(3));
        for (int frame = 0; frame < 200 && arcs.arcs("socket").isEmpty(); frame++) {
            arcs.advance(STEP);
        }
        assertFalse(arcs.arcs("socket").isEmpty());

        arcs.advance(STEP);
        arcs.advance(STEP);

        assertTrue(arcs.arcs("socket").isEmpty());
    }

    @Test
    void theSquareEdgeIsHalfTheSizeStraightOnAndFurtherTowardsACorner() {
        assertEquals(10f, SkillTreeNodeGeometry.squareEdgeDistance(10f, 5f, 0f), EPSILON);
        assertEquals(10f, SkillTreeNodeGeometry.squareEdgeDistance(10f, 0f, -3f), EPSILON);
        assertEquals(10f * (float) Math.sqrt(2), SkillTreeNodeGeometry.squareEdgeDistance(10f, 4f, 4f), EPSILON);
        assertEquals(10f, SkillTreeNodeGeometry.squareEdgeDistance(10f, 0f, 0f), EPSILON);
    }
}
