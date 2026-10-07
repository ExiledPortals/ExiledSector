package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

final class SkillTreeSocketRenderer {

    private static final String FRAME_PREFIX = "graphics/ui/bgs/panel00_";
    private static final String CENTER = FRAME_PREFIX + "center.png";
    private static final String TOP = FRAME_PREFIX + "top.png";
    private static final String BOTTOM = FRAME_PREFIX + "bot.png";
    private static final String LEFT = FRAME_PREFIX + "left.png";
    private static final String RIGHT = FRAME_PREFIX + "right.png";
    private static final String TOP_LEFT = FRAME_PREFIX + "top_left.png";
    private static final String TOP_RIGHT = FRAME_PREFIX + "top_right.png";
    private static final String BOTTOM_LEFT = FRAME_PREFIX + "bot_left.png";
    private static final String BOTTOM_RIGHT = FRAME_PREFIX + "bot_right.png";
    private static final float CORNER_RATIO = 0.25f;
    private static final float CONTENT_RATIO = 0.7f;
    private static final float EDGE_MARGIN_RATIO = 10f / 31f;
    private static final float BEVEL_RATIO = 30f / 31f;

    private static final Color UNALLOCATED_TINT = new Color(120, 120, 120);
    private static final float UNALLOCATED_ALPHA = 0.6f;
    private static final float ACCENT_TINT_STRENGTH = 0.6f;

    private static final String ARC_FRINGE_TEXTURE = "graphics/fx/beamfringe.png";
    private static final String ARC_CORE_TEXTURE = "graphics/fx/beamcore.png";
    private static final float ARC_FRINGE_WIDTH_RATIO = 0.14f;
    private static final float ARC_CORE_WIDTH_RATIO = 0.045f;
    private static final Color ARC_CORE_COLOR = new Color(235, 245, 255);

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeSocketRenderer.class);
    private final SpriteCache frameCache = new SpriteCache(SkillTreeSocketRenderer.class);
    private final Set<String> insetPieces = new HashSet<>();
    private final SocketArcs socketArcs = new SocketArcs(new Random());
    private Color tintedAccent;
    private Color allocatedTint;

    void advance(float amount) {
        socketArcs.advance(amount);
    }

    static float visibleEdgeDistance(float frameSize, float towardX, float towardY) {
        float halfSize = frameSize / 2f;
        float corner = frameSize * CORNER_RATIO;
        return SkillTreeNodeGeometry.octagonEdgeDistance(halfSize - corner * EDGE_MARGIN_RATIO, 2f * halfSize - corner * BEVEL_RATIO,
                towardX, towardY);
    }

    void drawFrame(float cx, float cy, float frameSize, boolean allocated, Color accent, float alphaMult) {
        Color tint = allocated ? allocatedTint(accent) : UNALLOCATED_TINT;
        float alpha = allocated ? alphaMult : alphaMult * UNALLOCATED_ALPHA;
        float halfSize = frameSize / 2f;
        float corner = frameSize * CORNER_RATIO;
        float inner = frameSize - 2f * corner;
        float edgeOffset = halfSize - corner / 2f;

        drawPiece(CENTER, cx, cy, inner, inner, tint, alpha);
        drawPiece(TOP, cx, cy + edgeOffset, inner, corner, tint, alpha);
        drawPiece(BOTTOM, cx, cy - edgeOffset, inner, corner, tint, alpha);
        drawPiece(LEFT, cx - edgeOffset, cy, corner, inner, tint, alpha);
        drawPiece(RIGHT, cx + edgeOffset, cy, corner, inner, tint, alpha);
        drawPiece(TOP_LEFT, cx - edgeOffset, cy + edgeOffset, corner, corner, tint, alpha);
        drawPiece(TOP_RIGHT, cx + edgeOffset, cy + edgeOffset, corner, corner, tint, alpha);
        drawPiece(BOTTOM_LEFT, cx - edgeOffset, cy - edgeOffset, corner, corner, tint, alpha);
        drawPiece(BOTTOM_RIGHT, cx + edgeOffset, cy - edgeOffset, corner, corner, tint, alpha);
    }

    void drawContent(float cx, float cy, float frameSize, String contentIconPath, Color iconTint, float alphaMult) {
        if (contentIconPath != null && !contentIconPath.isEmpty()) {
            float contentSize = frameSize * CONTENT_RATIO;
            SpriteDraw.drawAtCenter(spriteCache, contentIconPath, cx, cy, contentSize, contentSize, iconTint, alphaMult);
        }
    }

    void drawArcs(String nodeId, float cx, float cy, float frameSize, Color accent, float alphaMult) {
        SpriteAPI fringe = spriteCache.texture(ARC_FRINGE_TEXTURE);
        SpriteAPI core = spriteCache.texture(ARC_CORE_TEXTURE);
        if (fringe == null || core == null) return;
        float halfSize = frameSize / 2f;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        for (SocketArcs.Arc arc : socketArcs.arcs(nodeId)) {
            float alpha = arc.alpha() * alphaMult;
            Misc.setColor(accent, alpha);
            drawArcStrip(fringe, arc, cx, cy, halfSize, frameSize * ARC_FRINGE_WIDTH_RATIO);
            Misc.setColor(ARC_CORE_COLOR, alpha);
            drawArcStrip(core, arc, cx, cy, halfSize, frameSize * ARC_CORE_WIDTH_RATIO);
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void drawArcStrip(SpriteAPI texture, SocketArcs.Arc arc, float cx, float cy, float halfSize, float stripWidth) {
        texture.bindTexture();
        int last = arc.pointCount() - 1;
        float texWidth = texture.getTextureWidth();
        float texHeight = texture.getTextureHeight();
        float halfWidth = stripWidth / 2f;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= last; i++) {
            int before = Math.max(0, i - 1);
            int after = Math.min(last, i + 1);
            float tangentX = (arc.x(after) - arc.x(before)) * halfSize;
            float tangentY = (arc.y(after) - arc.y(before)) * halfSize;
            float length = (float) Math.hypot(tangentX, tangentY);
            float normalX = length == 0f ? 0f : -tangentY / length * halfWidth;
            float normalY = length == 0f ? 0f : tangentX / length * halfWidth;
            float x = cx + arc.x(i) * halfSize;
            float y = cy + arc.y(i) * halfSize;
            float u = texWidth * i / last;
            GL11.glTexCoord2f(u, 0f);
            GL11.glVertex2f(x + normalX, y + normalY);
            GL11.glTexCoord2f(u, texHeight);
            GL11.glVertex2f(x - normalX, y - normalY);
        }
        GL11.glEnd();
    }

    private Color allocatedTint(Color accent) {
        if (accent != tintedAccent) {
            tintedAccent = accent;
            allocatedTint = Misc.interpolateColor(Color.WHITE, accent, ACCENT_TINT_STRENGTH);
        }
        return allocatedTint;
    }

    private void drawPiece(String path, float cx, float cy, float pieceWidth, float pieceHeight, Color tint, float alphaMult) {
        if (pieceWidth <= 0f || pieceHeight <= 0f) return;
        SpriteAPI sprite = frameCache.sprite(path);
        if (sprite == null) return;
        if (insetPieces.add(path)) {
            insetByHalfATexel(sprite);
        }
        SpriteDraw.drawAtCenter(frameCache, path, cx, cy, pieceWidth, pieceHeight, tint, alphaMult);
    }

    private static void insetByHalfATexel(SpriteAPI sprite) {
        float texelWidth = sprite.getTextureWidth() / sprite.getWidth();
        float texelHeight = sprite.getTextureHeight() / sprite.getHeight();
        sprite.setTexX(texelWidth / 2f);
        sprite.setTexY(texelHeight / 2f);
        sprite.setTexWidth(sprite.getTextureWidth() - texelWidth);
        sprite.setTexHeight(sprite.getTextureHeight() - texelHeight);
    }
}
