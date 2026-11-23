package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayerDriftTest {

    private static final float MOST_SHIFT = 100f;

    @Test
    void aSlowLayerLagsBehindTheCameraByItsSpeed() {
        LayerDrift drift = new LayerDrift(0.8f, MOST_SHIFT, 2f);
        drift.follow(0f, 0f);
        drift.follow(200f, 0f);

        assertEquals(40f, drift.shiftX(), 1e-3f);
        assertEquals(0f, drift.shiftY(), 1e-3f);
    }

    @Test
    void theLagNeverGrowsPastTheMostShiftWhereverTheCameraGoes() {
        LayerDrift slow = new LayerDrift(0.8f, MOST_SHIFT, 2f);
        LayerDrift fast = new LayerDrift(1.25f, MOST_SHIFT, 2f);
        slow.follow(0f, 0f);
        fast.follow(0f, 0f);
        slow.follow(90000f, -40000f);
        fast.follow(90000f, -40000f);

        assertEquals(MOST_SHIFT, (float) Math.hypot(slow.shiftX(), slow.shiftY()), 1e-2f);
        assertEquals(MOST_SHIFT, (float) Math.hypot(fast.shiftX(), fast.shiftY()), 1e-2f);
        assertEquals(-Math.signum(slow.shiftX()), Math.signum(fast.shiftX()));
    }

    @Test
    void aRestingCameraLetsTheLayerSettleBackOntoTheMap() {
        LayerDrift drift = new LayerDrift(0.8f, MOST_SHIFT, 2f);
        drift.follow(0f, 0f);
        drift.follow(300f, 0f);
        for (int frame = 0; frame < 1200; frame++) {
            drift.advance(1f / 60f);
            drift.follow(300f, 0f);
        }

        assertEquals(0f, drift.shiftX(), 0.1f);
    }

    @Test
    void aLayerAtMapSpeedNeverDrifts() {
        LayerDrift drift = new LayerDrift(1f, MOST_SHIFT, 2f);
        drift.follow(0f, 0f);
        drift.follow(5000f, 7000f);

        assertEquals(0f, drift.shiftX());
        assertEquals(0f, drift.shiftY());
    }
}
