package exiledsector.charts.anomaly.breach;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreachFieldRendererTest {

    @Test
    void theRimFollowsTheZoomUntilItsDetailIsCapped() {
        assertEquals(2f, BreachFieldRenderer.rimDetail(100f, 0.5f), 0.001f);
        float huge = BreachFieldRenderer.rimDetail(5000f, 0.5f);
        assertEquals(BreachFieldRenderer.MAX_RIM_DETAIL_PIXELS, huge * (float) (Math.PI * 2d * 5000f), 1f);
    }
}
