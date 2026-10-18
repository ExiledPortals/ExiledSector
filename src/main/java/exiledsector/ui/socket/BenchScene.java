package exiledsector.ui.socket;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.util.SpriteCache;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class BenchScene {

    static final String ART_PATH = "graphics/ui/exiledSector/modification_bench.png";
    static final float ART_WIDTH = 320f;
    static final float ART_HEIGHT = 180f;
    static final float SOCKET_X = 159f;
    static final float SOCKET_Y = 83.3f;
    static final float LEFT_CONSOLE_X = 72.2f;
    static final float LEFT_CONSOLE_Y = 118.8f;
    static final float RIGHT_CONSOLE_X = 241.2f;
    static final float RIGHT_CONSOLE_Y = 121.2f;
    static final float ORBIT_RADIUS_X = 58f;
    static final float ORBIT_RADIUS_Y = 42f;
    static final float ORBIT_DROP_RADIANS = (float) Math.toRadians(15.0);
    static final float SLOT_RADIUS = 24f;
    static final float TILE_SIZE = 15f;
    static final float DETAILS_LEFT = 95f;
    static final float DETAILS_TOP = 111f;
    static final float DETAILS_WIDTH = 130f;
    static final float DETAILS_HEIGHT = 65f;
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

    static float tileSize(float scale) {
        return Math.max(44f, Math.min(80f, TILE_SIZE * scale));
    }

    static void drawArt(float left, float bottom, float scale, float alpha) {
        SpriteAPI art = SPRITES.sprite(ART_PATH);
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
