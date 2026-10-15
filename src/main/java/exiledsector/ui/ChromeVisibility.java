package exiledsector.ui;

final class ChromeVisibility {

    static final float FADE_SECONDS = 0.2f;
    private static final CanvasMode.Chrome[] CHROME_ELEMENTS = CanvasMode.Chrome.values();

    private final float[] modeFadeByChrome = new float[CHROME_ELEMENTS.length];
    private boolean faded;
    private float transitionChromeAlpha = 1f;
    private float transitionTreeAlpha = 1f;

    void advance(float amount, CanvasMode mode, float chromeAlpha, float treeAlpha) {
        transitionChromeAlpha = chromeAlpha;
        transitionTreeAlpha = treeAlpha;
        float step = faded ? amount / FADE_SECONDS : 1f;
        faded = true;
        for (CanvasMode.Chrome chrome : CHROME_ELEMENTS) {
            int index = chrome.ordinal();
            if (mode.shows(chrome)) {
                modeFadeByChrome[index] = Math.min(1f, modeFadeByChrome[index] + step);
            } else if (!mode.hidesChromeThroughTransition()) {
                modeFadeByChrome[index] = Math.max(0f, modeFadeByChrome[index] - step);
            }
        }
    }

    float alpha(CanvasMode.Chrome chrome) {
        float transitionAlpha = switch (chrome) {
            case READOUTS -> 1f;
            case SEARCH -> transitionTreeAlpha;
            case STATS_TOGGLE, TEMPLATE_BAR, STORAGE_BUTTON -> transitionChromeAlpha;
        };
        return modeFadeByChrome[chrome.ordinal()] * transitionAlpha;
    }
}
