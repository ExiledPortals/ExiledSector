package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.belt.AuroraBeltRenderer;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public final class BreachFieldRenderer {

    static final String RIM_TEXTURE = "graphics/planets/aurorae.png";
    static final Color RIM_COLOUR = new Color(16, 6, 28);
    static final float VOID_SHARE = 0.97f;
    static final float RIM_INNER_SHARE = 0.9f;
    static final float RIM_OUTER_SHARE = 1.08f;
    static final float MAX_RIM_DETAIL_PIXELS = 10240f;
    static final int VOID_SEGMENTS = 128;
    private static final SpriteCache SPRITES = new SpriteCache(BreachFieldRenderer.class);

    private BreachFieldRenderer() {
    }

    public static void render(Vector2f centre, float radius, float alphaMult, float elapsedSeconds, float viewMult) {
        render(rimBand(centre, radius, null), radius, alphaMult, elapsedSeconds, viewMult);
    }

    static void render(RadialBand rimBand, float radius, float alphaMult, float elapsedSeconds, float viewMult) {
        if (radius <= 0f || alphaMult <= 0f) {
            return;
        }
        drawVoid(rimBand.center(), radius * VOID_SHARE, alphaMult);
        SpriteAPI rim = SPRITES.texture(RIM_TEXTURE);
        if (rim != null) {
            AuroraBeltRenderer.renderShaded(rim, rimBand, RIM_COLOUR, alphaMult, elapsedSeconds, rimDetail(rimBand.outerRadius(), viewMult));
        }
    }

    static RadialBand rimBand(Vector2f centre, float radius, RadialBand previous) {
        float innerRadius = radius * RIM_INNER_SHARE;
        float outerRadius = radius * RIM_OUTER_SHARE;
        boolean unchanged = previous != null && previous.center() == centre && previous.innerRadius() == innerRadius && previous.outerRadius() == outerRadius;
        return unchanged ? previous : new RadialBand(centre, innerRadius, outerRadius);
    }

    static float rimDetail(float outerRadius, float viewMult) {
        float circumference = (float) (Math.PI * 2d * outerRadius);
        float pixelsPerUnit = viewMult > 0f ? 1f / viewMult : 1f;
        return circumference <= 0f ? pixelsPerUnit : Math.min(pixelsPerUnit, MAX_RIM_DETAIL_PIXELS / circumference);
    }

    private static void drawVoid(Vector2f centre, float voidRadius, float alphaMult) {
        try (GlScope ignored = GlScope.flat()) {
            HoloShapes.disc(centre.x, centre.y, voidRadius, VOID_SEGMENTS, Color.BLACK, alphaMult);
        }
    }
}
