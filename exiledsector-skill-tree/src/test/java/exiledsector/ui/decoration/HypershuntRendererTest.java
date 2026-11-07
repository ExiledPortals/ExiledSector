package exiledsector.ui.decoration;

import exiledsector.skills.decoration.Hypershunt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HypershuntRendererTest {

    private static final float TOLERANCE = 0.01f;

    private static Hypershunt hypershunt(boolean clockwise) {
        return new Hypershunt("shunt", 1000f, 2000f, new Hypershunt.Orbit(100f, 40f, clockwise, 0f), 300f, List.of("omega"));
    }

    private static void assertPose(float treeX, float treeY, float spriteAngleDeg, HypershuntRenderer.Pose pose) {
        assertEquals(treeX, pose.treeX(), TOLERANCE);
        assertEquals(treeY, pose.treeY(), TOLERANCE);
        assertEquals(spriteAngleDeg, pose.spriteAngleDeg(), TOLERANCE);
    }

    @Test
    void startsAtItsStartAngleWithTheSpikeTowardsTheStar() {
        assertPose(1100f, 2000f, -90f, HypershuntRenderer.pose(hypershunt(false), 0f));
    }

    @Test
    void orbitsAnticlockwiseOnScreenLikeVanillaUnlessToldOtherwise() {
        assertPose(1000f, 1900f, 0f, HypershuntRenderer.pose(hypershunt(false), 10f));
        assertPose(1000f, 2100f, -180f, HypershuntRenderer.pose(hypershunt(true), 10f));
    }
}
