package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasBubbleShadingTest {

    @Test
    void bubblesAreClearInTheMiddleBrightAtTheRimAndLitFromOneSide() {
        float[] middle = new float[2];
        float[] litRim = new float[2];
        float[] shadedRim = new float[2];
        AtlasBubbleShading.shade(0f, 0f, 0f, 0f, middle);
        AtlasBubbleShading.shade(-0.6f, 0.8f, 0f, 0f, litRim);
        AtlasBubbleShading.shade(0.6f, -0.8f, 0f, 0f, shadedRim);

        assertTrue(litRim[AtlasBubbleShading.ALPHA] > shadedRim[AtlasBubbleShading.ALPHA]);
        assertTrue(shadedRim[AtlasBubbleShading.ALPHA] > middle[AtlasBubbleShading.ALPHA]);
        assertTrue(middle[AtlasBubbleShading.ALPHA] < 0.5f * litRim[AtlasBubbleShading.ALPHA]);
    }

    @Test
    void theBorderBandGlowsEvenWhereTheSurfaceFacesTheCamera() {
        float[] open = new float[2];
        float[] border = new float[2];
        AtlasBubbleShading.shade(0f, 0f, 0f, 0f, open);
        AtlasBubbleShading.shade(0f, 0f, 1f, 0f, border);

        assertTrue(border[AtlasBubbleShading.ALPHA] > 3f * open[AtlasBubbleShading.ALPHA]);
    }

    @Test
    void theHighlightSpotTurnsTheSurfaceWhite() {
        float[] plain = new float[2];
        float[] spot = new float[2];
        AtlasBubbleShading.shade(0.3f, -0.3f, 0f, 0f, plain);
        AtlasBubbleShading.shade(0.3f, -0.3f, 0f, 1f, spot);

        assertTrue(spot[AtlasBubbleShading.WHITENESS] > 0.5f);
        assertTrue(plain[AtlasBubbleShading.WHITENESS] < 0.1f);
        for (float[] shade : new float[][] {plain, spot}) {
            assertTrue(shade[AtlasBubbleShading.ALPHA] <= 1f);
            assertTrue(shade[AtlasBubbleShading.WHITENESS] >= 0f && shade[AtlasBubbleShading.WHITENESS] <= 1f);
        }
    }
}
