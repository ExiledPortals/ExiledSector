package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasBubbleFieldTest {

    private static final float RADIUS = 2f * AtlasMapCanvas.CELL_PIXELS;
    private static final int POINTS = 96;

    private static float[] circle(float centreX, float centreY, float radius) {
        float[] outline = new float[POINTS * 2];
        for (int point = 0; point < POINTS; point++) {
            double angle = 2.0 * Math.PI * point / POINTS;
            outline[point * 2] = centreX + radius * (float) Math.cos(angle);
            outline[point * 2 + 1] = centreY + radius * (float) Math.sin(angle);
        }
        return outline;
    }

    private static AtlasBubbleField disc() {
        return AtlasBubbleField.of(List.of(circle(0f, 0f, RADIUS)), -RADIUS, -RADIUS, RADIUS, RADIUS);
    }

    private static int column(AtlasBubbleField field, float worldX) {
        return (int) Math.floor((worldX - field.originX()) / field.texelSize());
    }

    private static int row(AtlasBubbleField field, float worldY) {
        return (int) Math.floor((worldY - field.originY()) / field.texelSize());
    }

    @Test
    void oneBubbleRisesFromTheBorderToItsMiddle() {
        AtlasBubbleField field = disc();

        assertEquals(RADIUS, field.peak(), 0.08f * RADIUS);
        assertEquals(1f, field.heightShareAt(0f, 0f), 0.02f);
        assertTrue(field.heightShareAt(0.5f * RADIUS, 0f) > field.heightShareAt(0.9f * RADIUS, 0f));
        assertEquals(0f, field.heightShareAt(1.2f * RADIUS, 0f), 1e-6f);
    }

    @Test
    void theRimGlowsTheMiddleStaysClearAndTheLitSideIsBrighter() {
        AtlasBubbleField field = disc();
        float rim = 0.97f * RADIUS;
        float middle = field.alphaAt(column(field, 0f), row(field, 0f));
        float litRim = field.alphaAt(column(field, -rim), row(field, 0f));
        float shadedRim = field.alphaAt(column(field, rim), row(field, 0f));

        assertTrue(litRim > shadedRim, litRim + " vs " + shadedRim);
        assertTrue(shadedRim > middle, shadedRim + " vs " + middle);
        assertEquals(0f, field.alphaAt(column(field, 1.1f * RADIUS), row(field, 0f)), 1e-6f);
    }

    @Test
    void oneHighlightSpotSitsOnTheLitSideOfTheTop() {
        AtlasBubbleField field = disc();
        float offset = AtlasBubbleField.SPOT_SHIFT_SHARE * field.peak() / (float) Math.sqrt(2.0);
        float lit = field.alphaAt(column(field, -offset), row(field, -offset));
        float far = field.alphaAt(column(field, offset), row(field, offset));

        assertTrue(lit > 3f * far, lit + " vs " + far);
    }

    @Test
    void holesStayEmpty() {
        AtlasBubbleField ring = AtlasBubbleField.of(List.of(circle(0f, 0f, RADIUS), circle(0f, 0f, 0.4f * RADIUS)), -RADIUS, -RADIUS, RADIUS, RADIUS);

        assertEquals(0f, ring.heightShareAt(0f, 0f), 1e-6f);
        assertTrue(ring.heightShareAt(0.7f * RADIUS, 0f) > 0.9f);
        assertFalse(ring.coversAny(-0.1f * RADIUS, -0.1f * RADIUS, 0.1f * RADIUS, 0.1f * RADIUS));
    }
}
