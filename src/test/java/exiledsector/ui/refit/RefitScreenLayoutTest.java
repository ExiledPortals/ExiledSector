package exiledsector.ui.refit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefitScreenLayoutTest {

    @Test
    void onAWideScreenTheFallbackFrameIsTheVanillaMaximumCentredOnScreen() {
        RefitScreenLayout layout = RefitScreenLayout.fallback(1920f, 1080f);

        assertEquals(1252f, layout.frameWidth(), 0.001f);
        assertEquals(852f, layout.frameHeight(), 0.001f);
        assertEquals((1920f - 1252f) / 2f, layout.frameX(), 0.001f);
        assertEquals((1080f - 852f) / 2f, layout.frameY(), 0.001f);
    }

    @Test
    void onASmallScreenTheFallbackFrameStopsAtTheVanillaMinimum() {
        RefitScreenLayout layout = RefitScreenLayout.fallback(1024f, 700f);

        assertEquals(RefitScreenLayout.MIN_TAB_WIDTH + 2f, layout.frameWidth(), 0.001f);
        assertEquals(RefitScreenLayout.MIN_TAB_HEIGHT + 2f, layout.frameHeight(), 0.001f);
    }

    @Test
    void theFallbackShipAreaStartsRightOfTheFleetListAndDrawsAtOneToOne() {
        RefitScreenLayout layout = RefitScreenLayout.fallback(1920f, 1080f);

        assertEquals(layout.frameX() + 1f + RefitScreenLayout.FLEET_LIST_WIDTH, layout.shipAreaX(), 0.001f);
        assertEquals(1250f - RefitScreenLayout.FLEET_LIST_WIDTH - RefitScreenLayout.TAB_INSET, layout.shipAreaWidth(), 0.001f);
        assertEquals(1f, layout.shipZoom(), 0.001f);
    }

    @Test
    void theRefitZoomIsReadOnlyWhenItSitsInsideTheDisplaysOwnZoomRange() {
        assertEquals(1.25f, RefitScreenLayout.currentZoom(new float[]{0.5f, 1.5f, 1.25f, 1.5f}), 0.001f);
        assertEquals(1f, RefitScreenLayout.currentZoom(new float[]{0.5f, 1.5f, 7f, 7f}), 0.001f, "outside its own range means a field moved");
        assertEquals(1f, RefitScreenLayout.currentZoom(new float[]{0.5f}), 0.001f, "too few fields means the class changed");
        assertEquals(1f, RefitScreenLayout.currentZoom(new float[]{0f, 0f, 0f}), 0.001f);
    }
}
