package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.FallbackSupport;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.LineBatch;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.Rects;
import exiledsector.ui.util.TextLabel;
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
    private static final Color TEXT_COLOR = SkillTreePanelStyle.OP_TEXT_COLOR;
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

    private final TextLabel labelText = new TextLabel(FONT_SIZE, TEXT_COLOR);
    private final TextLabel labelShadowText = new TextLabel(FONT_SIZE, TEXT_SHADOW_COLOR);
    private final LineBatch bevelShadowLines = new LineBatch(EDGE_LINE_WIDTH);
    private final LineBatch bevelHighlightLines = new LineBatch(EDGE_LINE_WIDTH);
    private final LineBatch cornerAccentLines = new LineBatch(1f);

    private Color fillColor;
    private Color overflowColor;

    private boolean initialized = false;
    private int targetSpent;
    private int targetTotal;
    private float displayedSpent;
    private float displayedTotal;
    private int labelledSpent = -1;
    private int labelledTotal = -1;
    private String spentOfTotalLabel;

    SkillTreeReadoutBar(Class<?> owner, int row) {
        this.spriteCache = new SpriteCache(owner);
        this.row = row;
    }

    void advance(float amount, PositionAPI canvasPosition, int targetSpent, int targetTotal, float mouseX, float mouseY, boolean mouseKnown) {
        this.targetSpent = targetSpent;
        this.targetTotal = targetTotal;
        boolean hovered = mouseKnown && isHovered(canvasPosition, mouseX, mouseY);
        if (!initialized) {
            displayedSpent = targetSpent;
            displayedTotal = targetTotal;
            initialized = true;
        }
        float ease = Math.min(1f, amount * PROGRESS_EASE_SPEED);
        displayedSpent += (targetSpent - displayedSpent) * ease;
        displayedTotal += (targetTotal - displayedTotal) * ease;

        if (hovered) {
            hoverFader.fadeIn();
        } else {
            hoverFader.fadeOut();
        }
        hoverFader.advance(amount);
    }

    boolean isHovered(PositionAPI canvasPosition, float x, float y) {
        if (canvasPosition == null) return false;
        return Rects.contains(left(canvasPosition), bottom(canvasPosition), BAR_WIDTH, BAR_HEIGHT, x, y);
    }

    private static float left(PositionAPI canvasPosition) {
        return canvasPosition.getX() + SkillTreeRefitButton.SHIP_CARD_MARGIN;
    }

    private float bottom(PositionAPI canvasPosition) {
        float barTop = canvasPosition.getY() + canvasPosition.getHeight() - SkillTreeRefitButton.SHIP_CARD_MARGIN;
        return barTop - BAR_HEIGHT - row * (BAR_HEIGHT + ROW_GAP);
    }

    void render(PositionAPI canvasPosition, float alphaMult, String labelOverride) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) return;
        float barLeft = left(canvasPosition);
        float barBottom = bottom(canvasPosition);
        if (!initialized) {
            displayedSpent = targetSpent;
            displayedTotal = targetTotal;
        }

        Color standardFill = getFillColor();
        Color overflowFill = getOverflowColor();
        float glowBoost = hoverFader.getBrightness();

        boolean overCapacity = displayedSpent > displayedTotal;
        float fillFraction = displayedTotal > 0f ? Math.min(1f, displayedSpent / displayedTotal) : 0f;
        float fillWidth = overCapacity ? BAR_WIDTH : BAR_WIDTH * fillFraction;
        Color barColor = overCapacity ? overflowFill : standardFill;

        GLDraw.fillQuad(barLeft, barBottom, BAR_WIDTH, BAR_HEIGHT, Color.BLACK, alphaMult);

        if (fillWidth > 0f) {
            GLDraw.fillQuad(barLeft, barBottom, fillWidth, BAR_HEIGHT, barColor, alphaMult * (0.85f + 0.15f * glowBoost));
            drawInnerGlow(barLeft, barBottom, fillWidth, barColor, alphaMult, glowBoost);
            drawLeadingEdgeGlow(barLeft + fillWidth, barBottom, barColor, alphaMult, glowBoost);
        }

        drawEdgeBevel(barLeft, barBottom, barColor, alphaMult, glowBoost);
        drawCornerAccents(barLeft, barBottom, barColor, alphaMult);

        String shownLabel = labelOverride != null ? labelOverride : spentOfTotalLabel();
        drawLabel(font, shownLabel, barLeft, barBottom, alphaMult);
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

    private void drawLeadingEdgeGlow(float edgeX, float barBottom, Color color, float alphaMult, float glowBoost) {
        SpriteDraw.drawAdditiveAtCenter(spriteCache, GLOW_LINE_TEXTURE, edgeX, barBottom + BAR_HEIGHT / 2f,
                LEADING_EDGE_GLOW_WIDTH, BAR_HEIGHT, color, alphaMult * (0.35f + 0.65f * glowBoost));
    }

    private void drawInnerGlow(float barLeft, float barBottom, float fillWidth, Color color, float alphaMult, float glowBoost) {
        if (fillWidth <= 0f || BAR_HEIGHT <= 1f) return;

        float barTop = barBottom + BAR_HEIGHT;
        float barMid = barBottom + BAR_HEIGHT / 2f;

        float whiteBlend = INNER_GLOW_WASH_WHITE_BLEND_BASE + INNER_GLOW_WASH_WHITE_BLEND_HOVER_BOOST * glowBoost;
        Color washColor = Misc.interpolateColor(color, Color.WHITE, whiteBlend);
        GLDraw.fillQuad(barLeft, barBottom, fillWidth, BAR_HEIGHT, washColor, alphaMult * INNER_GLOW_WASH_ALPHA_MULT);

        float peakAlpha = alphaMult * (INNER_GLOW_PEAK_ALPHA_MULT + INNER_GLOW_PEAK_HOVER_BOOST * glowBoost);

        try (GlScope scope = GlScope.flat().additive()) {
            GL11.glBegin(GL11.GL_QUADS);
            glColorAlpha(color, 0f);
            GL11.glVertex2f(barLeft, barBottom);
            GL11.glVertex2f(barLeft + fillWidth, barBottom);
            glColorAlpha(color, peakAlpha);
            GL11.glVertex2f(barLeft + fillWidth, barMid);
            GL11.glVertex2f(barLeft, barMid);
            GL11.glEnd();

            GL11.glBegin(GL11.GL_QUADS);
            glColorAlpha(color, peakAlpha);
            GL11.glVertex2f(barLeft, barMid);
            GL11.glVertex2f(barLeft + fillWidth, barMid);
            glColorAlpha(color, 0f);
            GL11.glVertex2f(barLeft + fillWidth, barTop);
            GL11.glVertex2f(barLeft, barTop);
            GL11.glEnd();
        }
    }

    private void drawEdgeBevel(float barLeft, float barBottom, Color color, float alphaMult, float glowBoost) {
        float barTop = barBottom + BAR_HEIGHT;
        float barRight = barLeft + BAR_WIDTH;
        Color highlight = Misc.interpolateColor(color, Color.WHITE, EDGE_BEVEL_HOVER_WHITE_BLEND * glowBoost);
        bevelShadowLines.clear();
        bevelShadowLines.add(barLeft, barBottom, barLeft, barTop, color, 0.5f * alphaMult);
        bevelShadowLines.add(barRight, barBottom, barRight, barTop, color, 0.5f * alphaMult);
        bevelHighlightLines.clear();
        bevelHighlightLines.add(barLeft + 1f, barBottom, barLeft + 1f, barTop, highlight, alphaMult);
        bevelHighlightLines.add(barRight + 1f, barBottom, barRight + 1f, barTop, highlight, alphaMult);

        try (GlScope scope = GlScope.flat()) {
            bevelShadowLines.flush();
            scope.additive();
            bevelHighlightLines.flush();
        }
    }

    private void drawCornerAccents(float barLeft, float barBottom, Color color, float alphaMult) {
        float barRight = barLeft + BAR_WIDTH;
        float barTop = barBottom + BAR_HEIGHT;
        int opaque = color.getRGB() | 0xFF000000;
        int clear = color.getRGB() & 0x00FFFFFF;
        cornerAccentLines.clear();
        cornerAccentLines.add(barLeft + 1f, barBottom, opaque, barLeft + CORNER_ACCENT_LENGTH, barBottom, clear, alphaMult);
        cornerAccentLines.add(barLeft + 1f, barTop, opaque, barLeft + CORNER_ACCENT_LENGTH, barTop, clear, alphaMult);
        cornerAccentLines.add(barRight - 1f, barBottom, opaque, barRight - CORNER_ACCENT_LENGTH, barBottom, clear, alphaMult);
        cornerAccentLines.add(barRight - 1f, barTop, opaque, barRight - CORNER_ACCENT_LENGTH, barTop, clear, alphaMult);

        try (GlScope scope = GlScope.flat()) {
            cornerAccentLines.flush();
        }
    }

    private void glColorAlpha(Color color, float alpha) {
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(),
                (byte) (int) (255f * Math.max(0f, Math.min(1f, alpha))));
    }

    private void drawLabel(LazyFont font, String label, float barLeft, float barBottom, float alphaMult) {
        float textWidth = font.calcWidth(label, FONT_SIZE);
        float textHeight = FONT_SIZE * SkillTreePanelStyle.FONT_LINE_HEIGHT_FACTOR;
        float textX = barLeft + (BAR_WIDTH - textWidth) / 2f;
        float textY = barBottom + (BAR_HEIGHT + textHeight) / 2f;

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
