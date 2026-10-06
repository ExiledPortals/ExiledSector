package exiledsector.ui;

import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.ReusableText;

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

    private final BorderedPanel panel = new BorderedPanel(SkillTreeUiButton.class);
    private final ReusableText text = new ReusableText(FONT_SIZE, TEXT_COLOR);
    private Color textColor = TEXT_COLOR;
    private ScreenRect bounds = ScreenRect.NONE;
    private boolean enabled = true;
    private boolean selected;
    private boolean shown;
    private float fade;
    private long lastFadeNanos;

    SkillTreeUiButton(String label) {
        text.set(label);
    }

    void setLabel(String value) {
        text.set(value);
    }

    void setEnabled(boolean value) {
        enabled = value;
    }

    void setSelected(boolean value) {
        selected = value;
    }

    void setTextColor(Color value) {
        textColor = value == null ? TEXT_COLOR : value;
    }

    float preferredWidth() {
        return text.width() + PADDING_X * 2f;
    }

    void place(float left, float bottom, float width, float height) {
        bounds = new ScreenRect(left, bottom, width, height);
        shown = true;
    }

    void hide() {
        shown = false;
        if (fade <= 0f) {
            bounds = ScreenRect.NONE;
        }
    }

    boolean contains(float x, float y) {
        return shown && bounds.contains(x, y);
    }

    boolean isClickable(float x, float y) {
        return enabled && shown && bounds.contains(x, y);
    }

    void render(float mouseX, float mouseY, float alphaMult) {
        if (bounds == ScreenRect.NONE) {
            return;
        }
        advanceFade();
        if (!shown && fade <= 0f) {
            bounds = ScreenRect.NONE;
            return;
        }
        alphaMult *= fade;
        panel.draw(bounds.left(), bounds.bottom(), bounds.width(), bounds.height(), alphaMult);
        float fillAlpha = 0f;
        if (enabled && bounds.contains(mouseX, mouseY)) {
            fillAlpha = HOVER_ALPHA;
        } else if (selected) {
            fillAlpha = SELECTED_ALPHA;
        }
        if (fillAlpha > 0f) {
            GLDraw.fillQuad(bounds.left() + HOVER_INSET, bounds.bottom() + HOVER_INSET, bounds.width() - HOVER_INSET * 2f,
                    bounds.height() - HOVER_INSET * 2f, SkillTreePanelStyle.GLOW_COLOR, fillAlpha * alphaMult);
        }
        text.setColor(enabled ? textColor : DISABLED_COLOR);
        text.setAlpha(alphaMult);
        text.draw(bounds.left() + (bounds.width() - text.width()) / 2f,
                bounds.bottom() + bounds.height() / 2f + FONT_SIZE / 2f);
    }

    private void advanceFade() {
        long now = System.nanoTime();
        float seconds = lastFadeNanos == 0L ? 0f : Math.min(MAX_FADE_STEP_SECONDS, (now - lastFadeNanos) / 1_000_000_000f);
        lastFadeNanos = now;
        float step = seconds / FADE_SECONDS;
        fade = shown ? Math.min(1f, fade + step) : Math.max(0f, fade - step);
    }
}
