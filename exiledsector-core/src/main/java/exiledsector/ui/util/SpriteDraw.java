package exiledsector.ui.util;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class SpriteDraw {

    private static final int DISC_SEGMENTS = 48;
    private static final UnitCircle DISC_CIRCLE = UnitCircle.of(DISC_SEGMENTS);

    private SpriteDraw() {
    }

    public static void drawAtCenter(SpriteCache cache, String path, float cx, float cy, float width, float height,
                                    Color tint, float alphaMult) {
        draw(cache.sprite(path), cx, cy, width, height, tint, alphaMult, 0f, false);
    }

    public static void drawAtCenter(SpriteCache cache, String path, float cx, float cy, float width, float height,
                                    Color tint, float alphaMult, float angleDeg) {
        draw(cache.sprite(path), cx, cy, width, height, tint, alphaMult, angleDeg, false);
    }

    public static void drawAtCenter(SpriteAPI sprite, float cx, float cy, float width, float height,
                                    Color tint, float alphaMult, float angleDeg) {
        draw(sprite, cx, cy, width, height, tint, alphaMult, angleDeg, false);
    }

    public static void drawDiscAtCenter(SpriteAPI texture, float cx, float cy, float radius, float alphaMult) {
        if (texture == null) return;
        float textureLeft = texture.getTexX();
        float textureBottom = texture.getTexY();
        float textureWidth = texture.getTexWidth();
        float textureHeight = texture.getTexHeight();
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)) {
            texture.bindTexture();
            GL11.glColor4f(1f, 1f, 1f, alphaMult);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glTexCoord2f(textureLeft + textureWidth / 2f, textureBottom + textureHeight / 2f);
            GL11.glVertex2f(cx, cy);
            for (int segment = 0; segment <= DISC_SEGMENTS; segment++) {
                int index = segment % DISC_SEGMENTS;
                float cos = DISC_CIRCLE.cos(index);
                float sin = DISC_CIRCLE.sin(index);
                GL11.glTexCoord2f(textureLeft + textureWidth * (0.5f + 0.5f * cos), textureBottom + textureHeight * (0.5f + 0.5f * sin));
                GL11.glVertex2f(cx + cos * radius, cy + sin * radius);
            }
            GL11.glEnd();
        }
    }

    public static void drawAtPivot(SpriteCache cache, String path, float pivotScreenX, float pivotScreenY, float width, float height,
                                   float pivotOffsetX, float pivotOffsetY, Color tint, float alphaMult, float angleDeg) {
        SpriteAPI sprite = cache.sprite(path);
        if (sprite == null) return;
        sprite.setSize(width, height);
        sprite.setCenter(pivotOffsetX, pivotOffsetY);
        style(sprite, tint, alphaMult, angleDeg, false);
        sprite.renderAtCenter(pivotScreenX, pivotScreenY);
    }

    public static void drawAdditiveAtCenter(SpriteCache cache, String path, float cx, float cy, float width, float height,
                                            Color tint, float alphaMult) {
        draw(cache.sprite(path), cx, cy, width, height, tint, alphaMult, 0f, true);
    }

    public static void drawAdditiveAtCenter(SpriteCache cache, String path, float cx, float cy, float width, float height,
                                            Color tint, float alphaMult, float angleDeg) {
        draw(cache.sprite(path), cx, cy, width, height, tint, alphaMult, angleDeg, true);
    }

    public static void drawCellAtCenter(SpriteAPI sheet, int columns, int rows, int cell, float cx, float cy, float width, float height,
                                        float angleDeg, Color tint, float alphaMult, boolean additive) {
        if (sheet == null) return;
        selectCell(sheet, columns, rows, cell);
        draw(sheet, cx, cy, width, height, tint, alphaMult, angleDeg, additive);
    }

    private static void selectCell(SpriteAPI sheet, int columns, int rows, int cell) {
        int safeColumns = Math.max(1, columns);
        int safeRows = Math.max(1, rows);
        int wrapped = Math.floorMod(cell, safeColumns * safeRows);
        float cellWidth = sheet.getTextureWidth() / safeColumns;
        float cellHeight = sheet.getTextureHeight() / safeRows;
        sheet.setTexX(wrapped % safeColumns * cellWidth);
        sheet.setTexY(wrapped / safeColumns * cellHeight);
        sheet.setTexWidth(cellWidth);
        sheet.setTexHeight(cellHeight);
    }

    private static void draw(SpriteAPI sprite, float cx, float cy, float width, float height,
                             Color tint, float alphaMult, float angleDeg, boolean additive) {
        if (sprite == null) return;
        sprite.setSize(width, height);
        style(sprite, tint, alphaMult, angleDeg, additive);
        sprite.renderAtCenter(cx, cy);
    }

    private static void style(SpriteAPI sprite, Color tint, float alphaMult, float angleDeg, boolean additive) {
        sprite.setColor(tint != null ? tint : Color.WHITE);
        sprite.setAlphaMult(alphaMult);
        sprite.setAngle(angleDeg);
        if (additive) {
            sprite.setAdditiveBlend();
        } else {
            sprite.setNormalBlend();
        }
    }
}
