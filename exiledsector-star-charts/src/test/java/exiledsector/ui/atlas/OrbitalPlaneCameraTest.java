package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrbitalPlaneCameraTest {

    private static final float CENTRE_X = 500f;
    private static final float CENTRE_Y = 400f;
    private static final float REACH = 300f;
    private static final int ORBIT_SAMPLES = 3600;

    private static OrbitalPlaneCamera aimed(float tiltLevel) {
        OrbitalPlaneCamera camera = new OrbitalPlaneCamera();
        camera.aim(CENTRE_X, CENTRE_Y, REACH, tiltLevel);
        return camera;
    }

    private record OrbitBounds(float halfWidth, float bottom, float top) {

        static OrbitBounds of(OrbitalPlaneCamera camera) {
            float halfWidth = 0f;
            float bottom = Float.POSITIVE_INFINITY;
            float top = Float.NEGATIVE_INFINITY;
            for (int sample = 0; sample < ORBIT_SAMPLES; sample++) {
                double angle = 2 * Math.PI * sample / ORBIT_SAMPLES;
                float planeX = (float) Math.cos(angle) * REACH;
                float planeY = (float) Math.sin(angle) * REACH;
                halfWidth = Math.max(halfWidth, Math.abs(camera.screenX(planeX, planeY) - CENTRE_X));
                bottom = Math.min(bottom, camera.screenY(planeY));
                top = Math.max(top, camera.screenY(planeY));
            }
            return new OrbitBounds(halfWidth, bottom, top);
        }

        float height() {
            return top - bottom;
        }
    }

    @Test
    void anUntiltedCameraLooksStraightDownAndMovesNothing() {
        OrbitalPlaneCamera camera = aimed(0f);

        assertFalse(camera.tilted());
        assertEquals(CENTRE_X + 120f, camera.screenX(120f, -80f), 0.001f);
        assertEquals(CENTRE_Y - 80f, camera.screenY(-80f), 0.001f);
        assertEquals(1f, camera.sizeAt(250f), 0.0001f);
    }

    @Test
    void atFullTiltTheFrontOfTheOrbitIsBelowAndLargerThanTheBack() {
        OrbitalPlaneCamera camera = aimed(1f);
        float frontToBack = camera.sizeAt(-REACH) / camera.sizeAt(REACH);

        assertTrue(camera.tilted());
        assertTrue(frontToBack >= 1.6f && frontToBack <= 2f, "front to back size ratio " + frontToBack);
        assertTrue(camera.screenY(-REACH) < camera.screenY(REACH));
        assertTrue(camera.screenX(REACH, -REACH) - CENTRE_X > camera.screenX(REACH, REACH) - CENTRE_X);
    }

    @Test
    void theOuterOrbitStaysAsWideAsTheReachAndCentredOnTheFocusAtEveryTilt() {
        for (float tiltLevel : new float[]{0f, 0.3f, 0.7f, 1f}) {
            OrbitBounds bounds = OrbitBounds.of(aimed(tiltLevel));

            assertEquals(REACH, bounds.halfWidth(), 0.5f, "width at tilt " + tiltLevel);
            assertEquals(CENTRE_Y, (bounds.bottom() + bounds.top()) / 2f, 0.5f, "centre at tilt " + tiltLevel);
        }
    }

    @Test
    void theOrbitFlattensSteadilyAsTheCameraTiltsDown() {
        float untilted = OrbitBounds.of(aimed(0f)).height();
        float halfway = OrbitBounds.of(aimed(0.5f)).height();
        float full = OrbitBounds.of(aimed(1f)).height();

        assertEquals(REACH * 2f, untilted, 0.5f);
        assertTrue(halfway < untilted && full < halfway);
    }

    @Test
    void thePlaneMatrixProjectsEveryPointWhereTheCameraDoes() {
        OrbitalPlaneCamera camera = aimed(0.8f);
        float[] matrix = new float[OrbitalPlaneCamera.MATRIX_VALUES];
        camera.writePlaneMatrix(matrix);

        for (float[] point : new float[][]{{0f, 0f}, {REACH, 0f}, {-120f, REACH}, {80f, -REACH}, {-200f, -150f}}) {
            float x = matrix[0] * point[0] + matrix[4] * point[1] + matrix[12];
            float y = matrix[1] * point[0] + matrix[5] * point[1] + matrix[13];
            float w = matrix[3] * point[0] + matrix[7] * point[1] + matrix[15];

            assertEquals(camera.screenX(point[0], point[1]), x / w, 0.01f);
            assertEquals(camera.screenY(point[1]), y / w, 0.01f);
        }
        assertEquals(1f, matrix[10]);
    }
}
