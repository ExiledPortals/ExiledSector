package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChromeVisibilityTest {

    private static final float HALF_FADE = ChromeVisibility.FADE_SECONDS / 2f;
    private static final float TOLERANCE = 1e-5f;

    private final ChromeVisibility visibility = new ChromeVisibility();

    @Test
    void theFirstFrameSnapsToTheModeTableInsteadOfFading() {
        visibility.advance(0.016f, CanvasMode.ROOT_CHOICE, 1f, 1f);

        assertEquals(0f, visibility.alpha(CanvasMode.Chrome.SEARCH));
        assertEquals(0f, visibility.alpha(CanvasMode.Chrome.STORAGE_BUTTON));
        assertEquals(1f, visibility.alpha(CanvasMode.Chrome.STATS_TOGGLE));
        assertEquals(1f, visibility.alpha(CanvasMode.Chrome.READOUTS));
    }

    @Test
    void everyRenderedElementMatchesTheModeTableOnceSettled() {
        for (CanvasMode mode : CanvasMode.values()) {
            if (mode.hidesChromeThroughTransition()) {
                continue;
            }
            ChromeVisibility settled = new ChromeVisibility();
            settled.advance(0f, CanvasMode.TREE, 1f, 1f);
            settled.advance(ChromeVisibility.FADE_SECONDS, mode, 1f, 1f);
            for (CanvasMode.Chrome chrome : CanvasMode.Chrome.values()) {
                assertEquals(mode.shows(chrome) ? 1f : 0f, settled.alpha(chrome), mode + " " + chrome);
            }
        }
    }

    @Test
    void followingTheFleetFadesTheSearchBarAndTemplateBarOut() {
        visibility.advance(0f, CanvasMode.TREE, 1f, 1f);
        visibility.advance(HALF_FADE, CanvasMode.FLEET_FOLLOW, 1f, 1f);

        assertEquals(0.5f, visibility.alpha(CanvasMode.Chrome.SEARCH), TOLERANCE);
        assertEquals(0.5f, visibility.alpha(CanvasMode.Chrome.TEMPLATE_BAR), TOLERANCE);
        assertEquals(1f, visibility.alpha(CanvasMode.Chrome.STORAGE_BUTTON));

        visibility.advance(HALF_FADE, CanvasMode.FLEET_FOLLOW, 1f, 1f);

        assertEquals(0f, visibility.alpha(CanvasMode.Chrome.SEARCH), TOLERANCE);
    }

    @Test
    void hyperspaceFadesChromeWithItsTransitionAlphas() {
        visibility.advance(0f, CanvasMode.TREE, 1f, 1f);
        visibility.advance(ChromeVisibility.FADE_SECONDS, CanvasMode.HYPERSPACE, 0.25f, 0.75f);

        assertEquals(0.75f, visibility.alpha(CanvasMode.Chrome.SEARCH), TOLERANCE);
        assertEquals(0.25f, visibility.alpha(CanvasMode.Chrome.STATS_TOGGLE), TOLERANCE);
        assertEquals(0.25f, visibility.alpha(CanvasMode.Chrome.TEMPLATE_BAR), TOLERANCE);
        assertEquals(1f, visibility.alpha(CanvasMode.Chrome.READOUTS));
    }

    @Test
    void chromeAlreadyHiddenBeforeHyperspaceStaysHiddenThroughIt() {
        visibility.advance(0f, CanvasMode.FLEET_FOLLOW, 1f, 1f);
        visibility.advance(0.016f, CanvasMode.HYPERSPACE, 1f, 1f);

        assertEquals(0f, visibility.alpha(CanvasMode.Chrome.SEARCH));
        assertEquals(1f, visibility.alpha(CanvasMode.Chrome.STATS_TOGGLE));
    }
}
