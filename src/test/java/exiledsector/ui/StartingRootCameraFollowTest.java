package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartingRootCameraFollowTest {

    private static final float EPSILON = 0.001f;

    @Test
    void theCameraStaysWhereItWasOnTheFirstFrameInsteadOfJumpingToTheTarget() {
        StartingRootCameraFollow follow = new StartingRootCameraFollow(0f, 0f, 120f, -80f, 1f);

        assertEquals(0f, follow.x(120f, 0f), EPSILON);
        assertEquals(0f, follow.y(-80f, 0f), EPSILON);
    }

    @Test
    void theCameraEndsCentredOnTheTargetWhenTheFlightLands() {
        StartingRootCameraFollow follow = new StartingRootCameraFollow(0f, 0f, 120f, -80f, 1f);

        assertEquals(1082f, follow.x(1082f, 1f), EPSILON);
        assertEquals(616f, follow.y(616f, 1f), EPSILON);
    }

    @Test
    void midFlightTheCameraTracksTheTargetWhileTheStartingGapCloses() {
        StartingRootCameraFollow follow = new StartingRootCameraFollow(0f, 0f, 120f, -80f, 1f);

        assertEquals(600f - 60f, follow.x(600f, 0.5f), EPSILON);
        assertEquals(300f + 40f, follow.y(300f, 0.5f), EPSILON);
    }

    @Test
    void returningToTheChoicePansFromWhereverTheCameraWasBackToTheCentre() {
        StartingRootCameraFollow follow = new StartingRootCameraFollow(1500f, -900f, 0f, 0f, 1f);

        assertEquals(1500f, follow.x(0f, 0f), EPSILON);
        assertEquals(750f, follow.x(0f, 0.5f), EPSILON);
        assertEquals(0f, follow.y(0f, 1f), EPSILON);
    }

    @Test
    void zoomEasesBackToTheChoosingZoom() {
        StartingRootCameraFollow follow = new StartingRootCameraFollow(0f, 0f, 0f, 0f, 0.4f);

        assertEquals(0.4f, follow.zoom(0f), EPSILON);
        assertEquals(0.7f, follow.zoom(0.5f), EPSILON);
        assertEquals(StartingRootCameraFollow.CHOOSING_ZOOM, follow.zoom(1f), EPSILON);
    }
}
