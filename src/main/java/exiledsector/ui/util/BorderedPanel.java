package exiledsector.ui.util;

import com.fs.starfarer.api.graphics.SpriteAPI;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public final class BorderedPanel {

    private static final String EDGE_W_TOP = "graphics/ui/bgs/ui_border1b_w_top.png";
    private static final String EDGE_W_MID = "graphics/ui/bgs/ui_border1b_w.png";
    private static final String EDGE_W_BOT = "graphics/ui/bgs/ui_border1b_w_bot.png";
    private static final String EDGE_E_TOP = "graphics/ui/bgs/ui_border1b_e_top.png";
    private static final String EDGE_E_MID = "graphics/ui/bgs/ui_border1b_e.png";
    private static final String EDGE_E_BOT = "graphics/ui/bgs/ui_border1b_e_bot.png";
    private static final float BORDER_EDGE_WIDTH = 16f;
    private static final float BORDER_SOURCE_PIXELS = 16f;
    private static final float BORDER_CAP_HEIGHT = 8f;
    private static final Color BORDER_TINT = Color.WHITE;

    public static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 230);

    private final SpriteCache spriteCache;
    private final Set<String> insetSprites = new HashSet<>();

    public BorderedPanel(Class<?> owner) {
        this.spriteCache = new SpriteCache(owner);
    }

    public void draw(float x, float y, float width, float height, float alphaMult) {
        GLDraw.fillQuad(x, y, width, height, BACKGROUND_COLOR, alphaMult);

        float edgeWidth = Math.min(BORDER_EDGE_WIDTH, width / 2f);
        float capHeight = Math.min(BORDER_CAP_HEIGHT, height / 2f);
        float midHeight = Math.max(0f, height - capHeight * 2f);

        drawPiece(EDGE_W_TOP, x, y + height - capHeight, edgeWidth, capHeight, alphaMult);
        drawPiece(EDGE_W_MID, x, y + capHeight, edgeWidth, midHeight, alphaMult);
        drawPiece(EDGE_W_BOT, x, y, edgeWidth, capHeight, alphaMult);

        drawPiece(EDGE_E_TOP, x + width - edgeWidth, y + height - capHeight, edgeWidth, capHeight, alphaMult);
        drawPiece(EDGE_E_MID, x + width - edgeWidth, y + capHeight, edgeWidth, midHeight, alphaMult);
        drawPiece(EDGE_E_BOT, x + width - edgeWidth, y, edgeWidth, capHeight, alphaMult);
    }

    private void drawPiece(String path, float x, float y, float width, float height, float alphaMult) {
        if (width <= 0f || height <= 0f) return;
        SpriteAPI sprite = spriteCache.sprite(path);
        if (sprite == null) return;
        if (insetSprites.add(path)) {
            float fullWidth = sprite.getTextureWidth();
            float halfTexel = fullWidth * 0.5f / BORDER_SOURCE_PIXELS;
            sprite.setTexX(halfTexel);
            sprite.setTexWidth(fullWidth - 2f * halfTexel);
        }
        SpriteDraw.drawAtCenter(spriteCache, path, x + width / 2f, y + height / 2f,
                width, height, BORDER_TINT, alphaMult);
    }
}
