package exiledsector.ui;

import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;

final class SkillTreeUiButton {

    private static final float PADDING_X = 16f;
    private static final float FONT_SIZE = SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
    private static final float HOVER_INSET = 3f;
    private static final float HOVER_ALPHA = 0.25f;
    private static final float SELECTED_ALPHA = 0.18f;
    private static final Color TEXT_COLOR = SkillTreePanelStyle.TOOLTIP_BODY_COLOR;
    private static final Color DISABLED_COLOR = new Color(110, 110, 110);
    private static final float FADE_SECONDS = 0.2f;
    private static final float MAX_FADE_STEP_SECONDS = 0.1f;

    private final BorderedPanel borderPanel = new BorderedPanel(SkillTreeUiButton.class);
    private final TextLabel labelText = new TextLabel(FONT_SIZE, TEXT_COLOR);
    private Color textColor = TEXT_COLOR;
    private ScreenRect buttonBounds = ScreenRect.NONE;
    private boolean buttonEnabled = true;
    private boolean buttonSelected;
    private boolean buttonShown;
    private float fadeAlpha;
    private long lastFadeNanos;

    SkillTreeUiButton(String label) {
        labelText.set(label);
    }

    void setLabel(String value) {
        labelText.set(value);
    }

    void setEnabled(boolean value) {
        buttonEnabled = value;
    }

    void setSelected(boolean value) {
        buttonSelected = value;
    }

    void setTextColor(Color value) {
        textColor = value == null ? TEXT_COLOR : value;
    }

    float preferredWidth() {
        return labelText.width() + PADDING_X * 2f;
    }

    void place(float left, float bottom, float width, float height) {
        buttonBounds = new ScreenRect(left, bottom, width, height);
        buttonShown = true;
    }

    void hide() {
        buttonShown = false;
        if (fadeAlpha <= 0f) {
            buttonBounds = ScreenRect.NONE;
        }
    }

    ScreenRect bounds() {
        return buttonShown ? buttonBounds : ScreenRect.NONE;
    }

    boolean contains(float x, float y) {
        return buttonShown && buttonBounds.contains(x, y);
    }

    boolean isClickable(float x, float y) {
        return buttonEnabled && buttonShown && buttonBounds.contains(x, y);
    }

    void render(float mouseX, float mouseY, float alphaMult) {
        if (buttonBounds == ScreenRect.NONE) {
            return;
        }
        advanceFade();
        if (!buttonShown && fadeAlpha <= 0f) {
            buttonBounds = ScreenRect.NONE;
            return;
        }
        alphaMult *= fadeAlpha;
        borderPanel.draw(buttonBounds.left(), buttonBounds.bottom(), buttonBounds.width(), buttonBounds.height(), alphaMult);
        float fillAlpha = 0f;
        if (buttonEnabled && buttonBounds.contains(mouseX, mouseY)) {
            fillAlpha = HOVER_ALPHA;
        } else if (buttonSelected) {
            fillAlpha = SELECTED_ALPHA;
        }
        if (fillAlpha > 0f) {
            GLDraw.fillQuad(buttonBounds.left() + HOVER_INSET, buttonBounds.bottom() + HOVER_INSET, buttonBounds.width() - HOVER_INSET * 2f,
                    buttonBounds.height() - HOVER_INSET * 2f, SkillTreePanelStyle.GLOW_COLOR, fillAlpha * alphaMult);
        }
        labelText.setColor(buttonEnabled ? textColor : DISABLED_COLOR);
        labelText.setAlpha(alphaMult);
        labelText.draw(buttonBounds.left() + (buttonBounds.width() - labelText.width()) / 2f,
                buttonBounds.bottom() + buttonBounds.height() / 2f + FONT_SIZE / 2f);
    }

    private void advanceFade() {
        long now = System.nanoTime();
        float seconds = lastFadeNanos == 0L ? 0f : Math.min(MAX_FADE_STEP_SECONDS, (now - lastFadeNanos) / 1_000_000_000f);
        lastFadeNanos = now;
        float step = seconds / FADE_SECONDS;
        fadeAlpha = buttonShown ? Math.min(1f, fadeAlpha + step) : Math.max(0f, fadeAlpha - step);
    }
}
