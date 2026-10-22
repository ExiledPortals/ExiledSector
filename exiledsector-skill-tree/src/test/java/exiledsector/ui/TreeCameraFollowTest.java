package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeCameraFollowTest {

    private static TreeCamera settledOn(float treeX, float treeY) {
        TreeCamera camera = new TreeCamera();
        camera.startFollowing(1f);
        camera.follow(treeX, treeY, TreeCamera.FOLLOW_SETTLE_SECONDS);
        return camera;
    }

    @Test
    void aSettledFollowCentresOnTheMovingTarget() {
        TreeCamera camera = settledOn(500f, -200f);

        camera.follow(900f, 300f, 0.1f);

        assertEquals(900f, camera.centreTreeX(), 1e-3f);
        assertEquals(300f, camera.centreTreeY(), 1e-3f);
    }

    @Test
    void draggingWhileFollowingKeepsTheSameOffsetFromTheTargetAsItMoves() {
        TreeCamera camera = settledOn(0f, 0f);

        camera.startDrag();
        camera.dragBy(120f, -40f);
        camera.stopDrag();
        camera.follow(1000f, 2000f, 0.1f);

        assertEquals(1000f - 120f, camera.centreTreeX(), 1e-3f);
        assertEquals(2000f - 40f, camera.centreTreeY(), 1e-3f);
    }

    @Test
    void stoppingAndRestartingTheFollowForgetsTheDrag() {
        TreeCamera camera = settledOn(0f, 0f);
        camera.startDrag();
        camera.dragBy(300f, 300f);
        camera.stopDrag();

        camera.stopFollowing();
        camera.startFollowing(1f);
        camera.follow(50f, 60f, TreeCamera.FOLLOW_SETTLE_SECONDS);

        assertEquals(50f, camera.centreTreeX(), 1e-3f);
        assertEquals(60f, camera.centreTreeY(), 1e-3f);
    }
}
