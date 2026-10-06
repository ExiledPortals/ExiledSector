package exiledsector.ui;

import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;

final class ReusableText {

    private final float fontSize;
    private String text;
    private Color color;
    private LazyFont.DrawableString drawable;
    private String drawnText;
    private Color drawnColor;
    private int alpha = 255;
    private int drawnAlpha = 255;

    ReusableText(float fontSize, Color color) {
        this.fontSize = fontSize;
        this.color = color;
    }

    ReusableText set(String value) {
        text = value;
        return this;
    }

    ReusableText setColor(Color value) {
        color = value;
        return this;
    }

    ReusableText setAlpha(float value) {
        alpha = Math.round(Math.max(0f, Math.min(1f, value)) * 255f);
        return this;
    }

    boolean draw(float x, float y) {
        LazyFont.DrawableString current = current();
        if (current == null) {
            return false;
        }
        current.draw(x, y);
        return true;
    }

    float width() {
        LazyFont.DrawableString current = current();
        return current == null ? 0f : current.getWidth();
    }

    private LazyFont.DrawableString current() {
        if (text == null) {
            return null;
        }
        if (drawable == null) {
            LazyFont font = SkillTreePanelStyle.font();
            if (font == null) {
                return null;
            }
            drawable = SkillTreePanelStyle.buildSimpleText(font, text, fontSize, withAlpha(color, alpha));
            drawnText = text;
            drawnColor = color;
            drawnAlpha = alpha;
        }
        if (!text.equals(drawnText)) {
            drawable.setText(text);
            drawnText = text;
        }
        if (!color.equals(drawnColor) || alpha != drawnAlpha) {
            drawable.setBaseColor(withAlpha(color, alpha));
            drawnColor = color;
            drawnAlpha = alpha;
        }
        return drawable;
    }

    private static Color withAlpha(Color color, int alpha) {
        return alpha == 255 ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha * color.getAlpha() / 255);
    }
}
