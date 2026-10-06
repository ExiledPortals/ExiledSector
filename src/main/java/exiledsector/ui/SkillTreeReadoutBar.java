package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.ReusableText;
import exiledsector.ui.util.FallbackSupport;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.LineBatch;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.Rects;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class SkillTreeReadoutBar {

    private static final String GLOW_LINE_TEXTURE = "graphics/hud/line4x4.png";
    private static final String STANDARD_COLOR_KEY = "progressBarStandardColor";
    private static final String OVERFLOW_COLOR_KEY = "progressBarOverflowColor";
    private static final Color FALLBACK_FILL_COLOR = new Color(0, 121, 216);
    private static final Color FALLBACK_OVERFLOW_COLOR = new Color(220, 90, 70);
    private static final Color TEXT_COLOR = new Color(0xFD, 0xD0, 0x00);
    private static final Color TEXT_SHADOW_COLOR = new Color(0, 0, 0, 200);
    private static final float TEXT_SHADOW_OFFSET_X = 1f;
    private static final float TEXT_SHADOW_OFFSET_Y = -1f;

    private static final float BAR_WIDTH = 297f;
    private static final float BAR_HEIGHT = 30f;
    private static final float ROW_GAP = 8f;
    private static final float FONT_SIZE = 20f;

    private static final float EDGE_LINE_WIDTH = 2f;
    private static final float CORNER_ACCENT_LENGTH = 15f;
    private static final float LEADING_EDGE_GLOW_WIDTH = 8f;
    private static final float PROGRESS_EASE_SPEED = 10f;

    private static final float INNER_GLOW_WASH_ALPHA_MULT = 0.75f;
    private static final float INNER_GLOW_WASH_WHITE_BLEND_BASE = 0.15f;
    private static final float INNER_GLOW_WASH_WHITE_BLEND_HOVER_BOOST = 0.35f;
    private static final float INNER_GLOW_PEAK_ALPHA_MULT = 0.75f;
    private static final float INNER_GLOW_PEAK_HOVER_BOOST = 0.25f;
    private static final float EDGE_BEVEL_HOVER_WHITE_BLEND = 0.5f;

    private static final float HOVER_FADE_IN = 0.05f;
    private static final float HOVER_FADE_OUT = 0.25f;

    private final SpriteCache spriteCache;
    private final int row;
    private final FaderUtil hoverFader = new FaderUtil(HOVER_FADE_IN, HOVER_FADE_OUT);

    private final ReusableText labelText = new ReusableText(FONT_SIZE, TEXT_COLOR);
    private final ReusableText labelShadowText = new ReusableText(FONT_SIZE, TEXT_SHADOW_COLOR);
    private final LineBatch bevelShadowLines = new LineBatch(EDGE_LINE_WIDTH);
    private final LineBatch bevelHighlightLines = new LineBatch(EDGE_LINE_WIDTH);
    private final LineBatch cornerAccentLines = new LineBatch(1f);

    private Color fillColor;
    private Color overflowColor;

    private boolean initialized = false;
    private int spent;
    private int total;
    private float displayedSpent;
    private float displayedTotal;
    private int labelledSpent = -1;
    private int labelledTotal = -1;
    private String spentOfTotalLabel;

    SkillTreeReadoutBar(Class<?> owner, int row) {
        this.spriteCache = new SpriteCache(owner);
        this.row = row;
    }

    void advance(float amount, PositionAPI position, int spent, int total, float mouseX, float mouseY, boolean mouseKnown) {
        this.spent = spent;
        this.total = total;
        boolean hovered = mouseKnown && isHovered(position, mouseX, mouseY);
        if (!initialized) {
            displayedSpent = spent;
            displayedTotal = total;
            initialized = true;
        }
        float ease = Math.min(1f, amount * PROGRESS_EASE_SPEED);
        displayedSpent += (spent - displayedSpent) * ease;
        displayedTotal += (total - displayedTotal) * ease;

        if (hovered) {
            hoverFader.fadeIn();
        } else {
            hoverFader.fadeOut();
        }
        hoverFader.advance(amount);
    }

    boolean isHovered(PositionAPI position, float x, float y) {
        if (position == null) return false;
        return Rects.contains(left(position), bottom(position), BAR_WIDTH, BAR_HEIGHT, x, y);
    }

    private static float left(PositionAPI position) {
        return position.getX() + SkillTreeRefitButton.SHIP_CARD_MARGIN;
    }

    private float bottom(PositionAPI position) {
        float top = position.getY() + position.getHeight() - SkillTreeRefitButton.SHIP_CARD_MARGIN;
        return top - BAR_HEIGHT - row * (BAR_HEIGHT + ROW_GAP);
    }

    void render(PositionAPI position, float alphaMult, String labelOverride) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;
        float left = left(position);
        float bottom = bottom(position);
        if (!initialized) {
            displayedSpent = spent;
            displayedTotal = total;
        }

        Color fill = getFillColor();
        Color overflow = getOverflowColor();
        float glowBoost = hoverFader.getBrightness();

        boolean overCapacity = displayedSpent > displayedTotal;
        float fraction = displayedTotal > 0f ? Math.min(1f, displayedSpent / displayedTotal) : 0f;
        float fillWidth = overCapacity ? BAR_WIDTH : BAR_WIDTH * fraction;
        Color barColor = overCapacity ? overflow : fill;

        GLDraw.fillQuad(left, bottom, BAR_WIDTH, BAR_HEIGHT, Color.BLACK, alphaMult);

        if (fillWidth > 0f) {
            GLDraw.fillQuad(left, bottom, fillWidth, BAR_HEIGHT, barColor, alphaMult * (0.85f + 0.15f * glowBoost));
            drawInnerGlow(left, bottom, fillWidth, barColor, alphaMult, glowBoost);
            drawLeadingEdgeGlow(left + fillWidth, bottom, barColor, alphaMult, glowBoost);
        }

        drawEdgeBevel(left, bottom, barColor, alphaMult, glowBoost);
        drawCornerAccents(left, bottom, barColor, alphaMult);

        String label = labelOverride != null ? labelOverride : spentOfTotalLabel();
        drawLabel(font, label, left, bottom, alphaMult);
    }

    private String spentOfTotalLabel() {
        int shownSpent = Math.round(displayedSpent);
        int shownTotal = Math.round(displayedTotal);
        if (shownSpent != labelledSpent || shownTotal != labelledTotal) {
            labelledSpent = shownSpent;
            labelledTotal = shownTotal;
            spentOfTotalLabel = shownSpent + " / " + shownTotal;
        }
        return spentOfTotalLabel;
    }

    private void drawLeadingEdgeGlow(float edgeX, float bottom, Color color, float alphaMult, float glowBoost) {
        SpriteDraw.drawAdditiveAtCenter(spriteCache, GLOW_LINE_TEXTURE, edgeX, bottom + BAR_HEIGHT / 2f,
                LEADING_EDGE_GLOW_WIDTH, BAR_HEIGHT, color, alphaMult * (0.35f + 0.65f * glowBoost));
    }

    private void drawInnerGlow(float left, float bottom, float width, Color color, float alphaMult, float glowBoost) {
        if (width <= 0f || BAR_HEIGHT <= 1f) return;

        float top = bottom + BAR_HEIGHT;
        float mid = bottom + BAR_HEIGHT / 2f;

        float whiteBlend = INNER_GLOW_WASH_WHITE_BLEND_BASE + INNER_GLOW_WASH_WHITE_BLEND_HOVER_BOOST * glowBoost;
        Color washColor = Misc.interpolateColor(color, Color.WHITE, whiteBlend);
        GLDraw.fillQuad(left, bottom, width, BAR_HEIGHT, washColor, alphaMult * INNER_GLOW_WASH_ALPHA_MULT);

        float peakAlpha = alphaMult * (INNER_GLOW_PEAK_ALPHA_MULT + INNER_GLOW_PEAK_HOVER_BOOST * glowBoost);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        GL11.glBegin(GL11.GL_QUADS);
        glColorAlpha(color, 0f);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(left + width, bottom);
        glColorAlpha(color, peakAlpha);
        GL11.glVertex2f(left + width, mid);
        GL11.glVertex2f(left, mid);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_QUADS);
        glColorAlpha(color, peakAlpha);
        GL11.glVertex2f(left, mid);
        GL11.glVertex2f(left + width, mid);
        glColorAlpha(color, 0f);
        GL11.glVertex2f(left + width, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawEdgeBevel(float left, float bottom, Color color, float alphaMult, float glowBoost) {
        float top = bottom + BAR_HEIGHT;
        float right = left + BAR_WIDTH;
        Color highlight = Misc.interpolateColor(color, Color.WHITE, EDGE_BEVEL_HOVER_WHITE_BLEND * glowBoost);
        bevelShadowLines.clear();
        bevelShadowLines.add(left, bottom, left, top, color, 0.5f * alphaMult);
        bevelShadowLines.add(right, bottom, right, top, color, 0.5f * alphaMult);
        bevelHighlightLines.clear();
        bevelHighlightLines.add(left + 1f, bottom, left + 1f, top, highlight, alphaMult);
        bevelHighlightLines.add(right + 1f, bottom, right + 1f, top, highlight, alphaMult);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        bevelShadowLines.flush();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        bevelHighlightLines.flush();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawCornerAccents(float left, float bottom, Color color, float alphaMult) {
        float right = left + BAR_WIDTH;
        float top = bottom + BAR_HEIGHT;
        int opaque = color.getRGB() | 0xFF000000;
        int clear = color.getRGB() & 0x00FFFFFF;
        cornerAccentLines.clear();
        cornerAccentLines.add(left + 1f, bottom, opaque, left + CORNER_ACCENT_LENGTH, bottom, clear, alphaMult);
        cornerAccentLines.add(left + 1f, top, opaque, left + CORNER_ACCENT_LENGTH, top, clear, alphaMult);
        cornerAccentLines.add(right - 1f, bottom, opaque, right - CORNER_ACCENT_LENGTH, bottom, clear, alphaMult);
        cornerAccentLines.add(right - 1f, top, opaque, right - CORNER_ACCENT_LENGTH, top, clear, alphaMult);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        cornerAccentLines.flush();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void glColorAlpha(Color color, float alpha) {
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
                (byte) (int) (255f * Math.max(0f, Math.min(1f, alpha))));
    }

    private void drawLabel(LazyFont font, String label, float left, float bottom, float alphaMult) {
        float textWidth = font.calcWidth(label, FONT_SIZE);
        float textHeight = FONT_SIZE * SkillTreePanelStyle.FONT_LINE_HEIGHT_FACTOR;
        float textX = left + (BAR_WIDTH - textWidth) / 2f;
        float textY = bottom + (BAR_HEIGHT + textHeight) / 2f;

        labelShadowText.set(label).setAlpha(alphaMult).draw(textX + TEXT_SHADOW_OFFSET_X, textY + TEXT_SHADOW_OFFSET_Y);
        labelText.set(label).setAlpha(alphaMult).draw(textX, textY);
    }

    private Color getFillColor() {
        if (fillColor == null) {
            fillColor = colorOrFallback(STANDARD_COLOR_KEY, FALLBACK_FILL_COLOR);
        }
        return fillColor;
    }

    private Color getOverflowColor() {
        if (overflowColor == null) {
            overflowColor = colorOrFallback(OVERFLOW_COLOR_KEY, FALLBACK_OVERFLOW_COLOR);
        }
        return overflowColor;
    }

    private Color colorOrFallback(String settingsKey, Color fallback) {
        return FallbackSupport.getOrFallback(() -> Global.getSettings().getColor(settingsKey), fallback,
                Logger.getLogger(SkillTreeReadoutBar.class), "Failed to read settings colour " + settingsKey);
    }
}
