package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.belt.AuroraBeltRenderer;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public final class BreachFieldRenderer {

    static final String RIM_TEXTURE = "graphics/planets/aurorae.png";
    static final String CRACK_TEXTURE = "graphics/fx/emp_arcs.png";
    static final Color RIM_COLOUR = new Color(16, 6, 28);
    static final float VOID_SHARE = 0.97f;
    static final float RIM_INNER_SHARE = 0.9f;
    static final float RIM_OUTER_SHARE = 1.08f;
    static final float CRACK_INNER_SHARE = 0.95f;
    static final float CRACK_OUTER_SHARE = 1.3f;
    static final float CRACK_TILE_SIZE = 450f;
    static final float CRACK_STRENGTH = 0.9f;
    static final float MAX_RIM_DETAIL_PIXELS = 10240f;
    static final int VOID_SEGMENTS = 128;
    static final int CRACK_SEGMENTS = 256;
    private static final SpriteCache SPRITES = new SpriteCache(BreachFieldRenderer.class);

    private BreachFieldRenderer() {
    }

    public static void render(Vector2f centre, float radius, float alphaMult, float elapsedSeconds, float viewMult) {
        if (radius <= 0f || alphaMult <= 0f) {
            return;
        }
        drawVoid(centre, radius * VOID_SHARE, alphaMult);
        SpriteAPI cracks = SPRITES.texture(CRACK_TEXTURE);
        if (cracks != null) {
            drawCracks(cracks, centre, radius, alphaMult);
        }
        SpriteAPI rim = SPRITES.texture(RIM_TEXTURE);
        if (rim != null) {
            RadialBand band = new RadialBand(centre, radius * RIM_INNER_SHARE, radius * RIM_OUTER_SHARE);
            AuroraBeltRenderer.renderShaded(rim, band, RIM_COLOUR, alphaMult, elapsedSeconds, rimDetail(band.outerRadius(), viewMult));
        }
    }

    static float rimDetail(float outerRadius, float viewMult) {
        float circumference = (float) (Math.PI * 2d * outerRadius);
        float pixelsPerUnit = viewMult > 0f ? 1f / viewMult : 1f;
        return circumference <= 0f ? pixelsPerUnit : Math.min(pixelsPerUnit, MAX_RIM_DETAIL_PIXELS / circumference);
    }

    static int crackTilesAround(float radius) {
        float middle = radius * (CRACK_INNER_SHARE + CRACK_OUTER_SHARE) / 2f;
        return Math.max(1, Math.round((float) (Math.PI * 2d * middle) / CRACK_TILE_SIZE));
    }

    static float crackTilesAcross(float radius) {
        return Math.max(1f, radius * (CRACK_OUTER_SHARE - CRACK_INNER_SHARE) / CRACK_TILE_SIZE);
    }

    private static void drawVoid(Vector2f centre, float voidRadius, float alphaMult) {
        UnitCircle circle = UnitCircle.of(VOID_SEGMENTS);
        try (GlScope ignored = GlScope.flat()) {
            Misc.setColor(Color.BLACK, alphaMult);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(centre.x, centre.y);
            for (int i = 0; i <= VOID_SEGMENTS; i++) {
                int segment = i % VOID_SEGMENTS;
                GL11.glVertex2f(centre.x + circle.cos(segment) * voidRadius, centre.y + circle.sin(segment) * voidRadius);
            }
            GL11.glEnd();
        }
    }

    private static void drawCracks(SpriteAPI texture, Vector2f centre, float radius, float alphaMult) {
        UnitCircle circle = UnitCircle.of(CRACK_SEGMENTS);
        float inner = radius * CRACK_INNER_SHARE;
        float outer = radius * CRACK_OUTER_SHARE;
        float texPerSegment = (float) crackTilesAround(radius) / CRACK_SEGMENTS;
        float across = crackTilesAcross(radius);
        float strength = CRACK_STRENGTH * alphaMult;
        try (GlScope ignored = GlScope.textured(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR)) {
            texture.bindTexture();
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= CRACK_SEGMENTS; i++) {
                int segment = i % CRACK_SEGMENTS;
                float cos = circle.cos(segment);
                float sin = circle.sin(segment);
                float texY = texPerSegment * i;
                GL11.glColor4f(strength, strength, strength, 1f);
                GL11.glTexCoord2f(0f, texY);
                GL11.glVertex2f(centre.x + cos * inner, centre.y + sin * inner);
                GL11.glColor4f(0f, 0f, 0f, 1f);
                GL11.glTexCoord2f(across, texY);
                GL11.glVertex2f(centre.x + cos * outer, centre.y + sin * outer);
            }
            GL11.glEnd();
        }
    }
}
