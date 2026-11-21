package exiledsector.ui.belt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BandPlacementTest {

    @Test
    void aPlacedBandSitsAroundItsPlaneCentre() {
        BandPlacement placement = BandPlacement.of(new FlatProjection(), 40f, -25f);

        assertEquals(40f, placement.planeX(0f, 0f), 1e-4f);
        assertEquals(-25f, placement.planeY(0f, 0f), 1e-4f);
        assertEquals(50f, placement.planeX(10f, 0f), 1e-4f);
        assertEquals(-25f, placement.planeY(10f, 0f), 1e-4f);
    }

    @Test
    void turningABandRotatesItsVerticesAroundTheCentre() {
        BandPlacement placement = BandPlacement.of(new FlatProjection(), 40f, -25f);
        placement.turnBy(60f);
        placement.turnBy(30f);

        assertEquals(40f, placement.planeX(10f, 0f), 1e-4f);
        assertEquals(-15f, placement.planeY(10f, 0f), 1e-4f);
        assertEquals(30f, placement.planeX(0f, 10f), 1e-4f);
        assertEquals(-25f, placement.planeY(0f, 10f), 1e-4f);
    }

    private static final class FlatProjection implements PlaneProjection {

        @Override
        public float screenX(float planeX, float planeY) {
            return planeX;
        }

        @Override
        public float screenY(float planeX, float planeY) {
            return planeY;
        }
    }
}
