package exiledsector.ui.atlas;

import exiledsector.charts.ChartSystemSketch;
import exiledsector.charts.ChartSystemSketch.Body;
import exiledsector.ui.node.NodeMetrics;
import exiledsector.ui.node.NodeRingArt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasSystemPreviewTest {

    @Test
    void bodiesGoRoundOncePerOrbitScaledToPreviewSeconds() {
        float period = 100f * AtlasSystemPreview.SECONDS_PER_DAY;

        assertEquals(30f, AtlasSystemPreview.angleAt(30f, 100f, 0f));
        assertEquals(390f, AtlasSystemPreview.angleAt(30f, 100f, period), 0.001f);
        assertEquals(30f, AtlasSystemPreview.angleAt(30f, 0f, period));
    }

    @Test
    void bodySizesStayWithinTheirShareOfThePreview() {
        assertEquals(100f * AtlasSystemPreview.MAX_STAR_SHARE, AtlasSystemPreview.starPixels(new Body("x", 5000f, 0f, 0f, 0f), 100f), 0.001f);
        assertEquals(100f * AtlasSystemPreview.MIN_PLANET_SHARE, AtlasSystemPreview.planetPixels(new Body("x", 1f, 0f, 0f, 0f), 100f), 0.001f);
        assertEquals(AtlasSystemPreview.MIN_BODY_PIXELS, AtlasSystemPreview.planetPixels(new Body("x", 1f, 0f, 0f, 0f), 5f));
    }

    @Test
    void ringKindsUseTheMatchingVanillaBandTexture() {
        assertEquals(AtlasSystemPreview.ASTEROID_RINGS, AtlasSystemPreview.ringTexture("asteroid_belt"));
        assertEquals(AtlasSystemPreview.ICE_RINGS, AtlasSystemPreview.ringTexture("ring_ice"));
        assertEquals(AtlasSystemPreview.SPECIAL_RINGS, AtlasSystemPreview.ringTexture("ring_special"));
        assertEquals(AtlasSystemPreview.DUST_RINGS, AtlasSystemPreview.ringTexture("ring_dust"));
        assertEquals(AtlasSystemPreview.DUST_RINGS, AtlasSystemPreview.ringTexture(null));
    }

    @Test
    void theShadowFallsOnTheSideFacingAwayFromTheStar() {
        assertEquals(0f, AtlasSystemPreview.shadowAlpha(1f, 0f, 1f, 0f));
        assertEquals(AtlasSystemPreview.SHADOW_MAX, AtlasSystemPreview.shadowAlpha(-1f, 0f, 1f, 0f));
        assertEquals(AtlasSystemPreview.SHADOW_FLOOR, AtlasSystemPreview.shadowAlpha(0f, 1f, 1f, 0f));
    }

    @Test
    void anEmptyNebulaStillGetsAPreviewScale() {
        ChartSystemSketch nebula = new ChartSystemSketch(null, List.of(new Body("nebula_center_old", 0f, 0f, 0f, 0f)), List.of(), List.of());

        assertTrue(AtlasSystemPreview.Prepared.of("k", nebula).outerRadius() >= AtlasSystemPreview.MIN_OUTER_RADIUS);
    }

    @Test
    void neighbouringPortalsNeverTouchEvenAtTheWidestJitter() {
        float generatorJitter = 0.4f;
        float closest = (1f - 2f * generatorJitter * AtlasMapCanvas.DISPLAY_JITTER_SHARE) * AtlasMapCanvas.CELL_PIXELS;
        float portalReach = NodeRingArt.portalReach(NodeMetrics.donutRadius(NodeMetrics.NODE_SIZE * AtlasMapCanvas.NODE_SCALE));

        assertTrue(closest > portalReach * 2f);
    }

    @Test
    void displayedPositionsKeepOnlyPartOfTheGeneratorsJitter() {
        assertEquals(2f * AtlasMapCanvas.CELL_PIXELS, AtlasMapCanvas.displayed(2, 2f));
        assertEquals((2f + 0.4f * AtlasMapCanvas.DISPLAY_JITTER_SHARE) * AtlasMapCanvas.CELL_PIXELS, AtlasMapCanvas.displayed(2, 2.4f), 0.01f);
    }
}
