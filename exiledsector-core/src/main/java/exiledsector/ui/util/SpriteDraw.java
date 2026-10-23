package exiledsector.ui.util;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;

import java.awt.Color;

public final class SpriteDraw {

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
