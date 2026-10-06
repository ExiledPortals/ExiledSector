package exiledsector.ui.util;

import exiledsector.ui.SkillTreePanelStyle;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;

public final class ReusableText {

    private final float fontSize;
    private final LazyFont.TextAnchor anchor;
    private String text;
    private Color color;
    private LazyFont.DrawableString drawable;
    private String drawnText;
    private Color drawnColor;
    private int alpha = 255;
    private int drawnAlpha = 255;

    public ReusableText(float fontSize, Color color) {
        this(fontSize, color, LazyFont.TextAnchor.TOP_LEFT);
    }

    public ReusableText(float fontSize, Color color, LazyFont.TextAnchor anchor) {
        this.fontSize = fontSize;
        this.color = color;
        this.anchor = anchor;
    }

    public ReusableText set(String value) {
        text = value;
        return this;
    }

    public ReusableText setColor(Color value) {
        color = value;
        return this;
    }

    public ReusableText setAlpha(float value) {
        alpha = Math.round(Math.max(0f, Math.min(1f, value)) * 255f);
        return this;
    }

    public boolean draw(float x, float y) {
        LazyFont.DrawableString current = current();
        if (current == null) {
            return false;
        }
        current.draw(x, y);
        return true;
    }

    public float width() {
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
            drawable = SkillTreePanelStyle.buildSimpleText(font, text, fontSize, withAlpha(color, alpha), anchor);
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
