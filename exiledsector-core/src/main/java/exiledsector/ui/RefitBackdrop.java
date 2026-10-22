package exiledsector.ui;

import com.fs.starfarer.api.Global;
import exiledsector.ui.refit.RefitScreenLayout;
import exiledlib.ui.util.GLDraw;
import exiledlib.ui.util.GlScope;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class RefitBackdrop {

    static final float GRID_SPACING = 25f;
    static final float FRAME_FILL_ALPHA = 1f;
    static final float FRAME_BORDER_ALPHA = 0.5f;
    static final float SHIP_AREA_TINT_ALPHA = 0.4f;
    static final float SHIP_AREA_SHADE_ALPHA = 0.4f;
    private static final float GRID_LINE_WIDTH = 1f;
    private static final float GRID_GLOW_WIDTH = 3f;
    private static final float GRID_GLOW_ALPHA = 0.5f;
    private static final float LINE_MIDDLE_ALPHA = 0.5f;
    private static final Color FALLBACK_BORDER = new Color(100, 200, 255, 255);
    private static final Color FALLBACK_GRID = new Color(70, 200, 255, 75);
    private static final Color FALLBACK_TINT = new Color(31, 94, 112, 175);

    private final Color borderColor = settingsColor("widgetBorderColorDark", FALLBACK_BORDER);
    private final Color gridColor = settingsColor("standardGridColor", FALLBACK_GRID);
    private final Color tintColor = settingsColor("buttonBgDark", FALLBACK_TINT);

    private static Color settingsColor(String id, Color fallback) {
        try {
            Color color = Global.getSettings().getColor(id);
            return color == null ? fallback : color;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    void drawFrame(RefitScreenLayout layout, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        GLDraw.fillQuad(layout.frameX() + 1f, layout.frameY() + 1f, layout.frameWidth() - 2f, layout.frameHeight() - 2f, Color.BLACK,
                FRAME_FILL_ALPHA * alpha);
        GLDraw.strokeQuad(layout.frameX() + 0.5f, layout.frameY() + 0.5f, layout.frameWidth() - 1f, layout.frameHeight() - 1f, borderColor, 1f,
                FRAME_BORDER_ALPHA * alpha);
    }

    void drawShipArea(RefitScreenLayout layout, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        float left = layout.frameX() + 1f;
        float bottom = layout.shipAreaY();
        float width = layout.shipAreaX() + layout.shipAreaWidth() - left;
        float height = layout.shipAreaHeight();
        drawGrid(left, bottom, width, height, layout.shipCentreX(), layout.shipCentreY(), GRID_LINE_WIDTH, GL11.GL_ONE_MINUS_SRC_ALPHA, alpha);
        drawGrid(left, bottom, width, height, layout.shipCentreX(), layout.shipCentreY(), GRID_GLOW_WIDTH, GL11.GL_ONE, alpha * GRID_GLOW_ALPHA);
        GLDraw.fillQuad(left, bottom, width, height, tintColor, SHIP_AREA_TINT_ALPHA * alpha);
        GLDraw.fillQuad(left, bottom, width, height, Color.BLACK, SHIP_AREA_SHADE_ALPHA * alpha);
    }

    private void drawGrid(float left, float bottom, float width, float height, float centreX, float centreY, float lineWidth,
                          int destinationFactor, float alpha) {
        float red = gridColor.getRed() / 255f;
        float green = gridColor.getGreen() / 255f;
        float blue = gridColor.getBlue() / 255f;
        float edgeAlpha = gridColor.getAlpha() / 255f * alpha;
        float middleAlpha = edgeAlpha * LINE_MIDDLE_ALPHA;
        try (GlScope scope = GlScope.flat().blend(GL11.GL_SRC_ALPHA, destinationFactor).lineWidth(lineWidth)) {
            GL11.glBegin(GL11.GL_LINES);
            for (float lineX : gridLines(centreX, left, left + width)) {
                fadingLine(lineX, bottom, lineX, bottom + height, red, green, blue, edgeAlpha, middleAlpha);
            }
            for (float lineY : gridLines(centreY, bottom, bottom + height)) {
                fadingLine(left, lineY, left + width, lineY, red, green, blue, edgeAlpha, middleAlpha);
            }
            GL11.glEnd();
        }
    }

    static float[] gridLines(float centre, float min, float max) {
        int below = (int) Math.floor((centre - min) / GRID_SPACING);
        int above = (int) Math.floor((max - centre) / GRID_SPACING);
        float[] lines = new float[Math.max(0, below + above + 1)];
        for (int index = 0; index < lines.length; index++) {
            lines[index] = centre + (index - below) * GRID_SPACING;
        }
        return lines;
    }

    private static void fadingLine(float startX, float startY, float endX, float endY, float red, float green, float blue, float edgeAlpha,
                                   float middleAlpha) {
        float middleX = (startX + endX) / 2f;
        float middleY = (startY + endY) / 2f;
        GL11.glColor4f(red, green, blue, edgeAlpha);
        GL11.glVertex2f(startX, startY);
        GL11.glColor4f(red, green, blue, middleAlpha);
        GL11.glVertex2f(middleX, middleY);
        GL11.glVertex2f(middleX, middleY);
        GL11.glColor4f(red, green, blue, edgeAlpha);
        GL11.glVertex2f(endX, endY);
    }
}
