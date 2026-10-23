package exiledsector.ui.socket;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class BenchScene {

    static final float ART_WIDTH = 320f;
    static final float ART_HEIGHT = 180f;
    static final float ORBIT_DROP_RADIANS = (float) Math.toRadians(15.0);
    static final float SLOT_RADIUS = 24f;
    static final float TILE_SIZE = 15f;
    static final float SOCKET_GLOW_SHARE = 1.05f;
    static final float DETAILS_SOCKET_GAP = 2.5f;
    static final float DETAILS_WIDTH = 130f;
    static final float DETAILS_BOTTOM = 176f;
    static final float CHART_DETAILS_BOTTOM = 158f;
    static final float OPEN_BUTTON_TOP = 161f;
    static final float OPEN_BUTTON_HEIGHT = 14f;
    static final float NOTICE_TOP = 15f;
    static final float NOTICE_HEIGHT = 13f;
    static final float SOCKET_CLICK_SHARE = 0.6f;
    static final int MIN_WHOLE_SCALE = 2;
    private static final SpriteCache SPRITES = new SpriteCache(BenchScene.class);

    private BenchScene() {
    }

    static float scaleFor(float availableWidth, float availableHeight) {
        float fit = Math.min(availableWidth / ART_WIDTH, availableHeight / ART_HEIGHT);
        int whole = (int) Math.floor(fit);
        return whole >= MIN_WHOLE_SCALE ? whole : Math.max(0.5f, fit);
    }

    static float socketBottom(BenchMode mode) {
        return mode.socketY() + SLOT_RADIUS * SOCKET_GLOW_SHARE;
    }

    static float detailsLeft(BenchMode mode) {
        return mode.socketX() - DETAILS_WIDTH / 2f;
    }

    static float detailsTop(BenchMode mode) {
        return socketBottom(mode) + DETAILS_SOCKET_GAP;
    }

    static float tileSize(float scale) {
        return Math.max(44f, Math.min(80f, TILE_SIZE * scale));
    }

    static void drawArt(BenchMode mode, float left, float bottom, float scale, float alpha) {
        SpriteAPI art = SPRITES.sprite(mode.artPath());
        if (art == null || alpha <= 0f) {
            return;
        }
        art.bindTexture();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        art.setSize(ART_WIDTH * scale, ART_HEIGHT * scale);
        art.setColor(Color.WHITE);
        art.setAlphaMult(alpha);
        art.setNormalBlend();
        art.render(left, bottom);
    }
}
