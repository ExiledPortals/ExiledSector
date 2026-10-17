package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TexturedQuadBatch;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class HullEngines {

    static final String FLAME_PATH = "graphics/fx/particleline32ln.png";
    static final String GLOW_PATH = "graphics/fx/hit_glow.png";
    static final float SPRITE_ANGLE_OFFSET = 90f;
    private static final float FLAME_LEAD = 3f;
    private static final float MIN_FLAME_HALF_WIDTH = 0.3f;
    private static final float GLOW_SIZE_MULT = 0.75f;
    private static final float IDLE_FLAME_LENGTH = 0.25f;
    private static final float IDLE_GLOW_GROWTH = 0.75f;
    private static final float COLOR_CHANNEL_MAX = 255f;

    public static final HullEngines NONE = new HullEngines(FleetHullVisuals.NONE);

    private final FleetHullVisuals visuals;

    private HullEngines(FleetHullVisuals visuals) {
        this.visuals = visuals;
    }

    public static HullEngines of(ShipHullSpecAPI hullSpec) {
        return hullSpec == null ? NONE : new HullEngines(FleetHullVisuals.of(hullSpec));
    }

    static HullEngines of(FleetHullVisuals visuals) {
        return visuals == null ? NONE : new HullEngines(visuals);
    }

    public boolean hasEngines() {
        return !visuals.flames().isEmpty();
    }

    static float flameLengthFactor(float engineLevel) {
        return (IDLE_FLAME_LENGTH + engineLevel) * (0.5f + 0.5f * engineLevel);
    }

    static float glowFactor(float engineLevel) {
        return GLOW_SIZE_MULT * (1f + IDLE_GLOW_GROWTH * (1f - engineLevel)) * (0.5f + 0.5f * engineLevel);
    }

    public void drawHull(SpriteCache spriteCache, String spritePath, float screenX, float screenY, float hullWidth, float hullHeight,
                         float screenScale, float facingDeg, float alpha) {
        float angleDeg = facingDeg - SPRITE_ANGLE_OFFSET;
        if (visuals.hasCenter()) {
            SpriteDraw.drawAtPivot(spriteCache, spritePath, screenX, screenY, hullWidth, hullHeight,
                    visuals.centerX() * screenScale, visuals.centerY() * screenScale, Color.WHITE, alpha, angleDeg);
        } else {
            SpriteDraw.drawAtCenter(spriteCache, spritePath, screenX, screenY, hullWidth, hullHeight, Color.WHITE, alpha, angleDeg);
        }
    }

    public void drawEngines(SpriteCache spriteCache, TexturedQuadBatch glowQuads, float screenX, float screenY, float facingDeg,
                            float screenScale, float zoom, float engineLevel, float alpha) {
        if (alpha <= 0f || !hasEngines()) {
            return;
        }
        double facingRad = Math.toRadians(facingDeg);
        SpriteAPI flameTexture = spriteCache.texture(FLAME_PATH);
        if (flameTexture != null) {
            try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
                flameTexture.bindTexture();
                GL11.glBegin(GL11.GL_QUADS);
                addFlames(screenX, screenY, facingRad, screenScale, zoom, flameLengthFactor(engineLevel), alpha,
                        flameTexture.getTextureWidth(), flameTexture.getTextureHeight());
                GL11.glEnd();
            }
        }
        SpriteAPI glowTexture = spriteCache.texture(GLOW_PATH);
        if (glowTexture != null) {
            try (TexturedQuadBatch quads = glowQuads.beginAdditive(glowTexture)) {
                addGlows(quads, screenX, screenY, facingRad, screenScale, glowFactor(engineLevel), alpha);
            }
        }
    }

    void addFlames(float screenX, float screenY, double facingRad, float screenScale, float zoom, float lengthFactor, float alpha,
                   float textureRight, float textureTop) {
        float facingCos = (float) Math.cos(facingRad);
        float facingSin = (float) Math.sin(facingRad);
        for (FleetHullVisuals.EngineFlame flame : visuals.flames()) {
            float slotX = screenX + (flame.forwardOffset() * facingCos - flame.leftOffset() * facingSin) * screenScale;
            float slotY = screenY + (flame.forwardOffset() * facingSin + flame.leftOffset() * facingCos) * screenScale;
            float halfWidth = Math.max(zoom, flame.width() * screenScale) / 2f;
            if (halfWidth < MIN_FLAME_HALF_WIDTH) continue;
            double flameAngle = facingRad + Math.toRadians(flame.angleDeg());
            float directionX = (float) Math.cos(flameAngle);
            float directionY = (float) Math.sin(flameAngle);
            float length = flame.length() * screenScale * lengthFactor;
            float lead = FLAME_LEAD * zoom;
            float backX = slotX - directionX * lead;
            float backY = slotY - directionY * lead;
            float tipX = slotX + directionX * length;
            float tipY = slotY + directionY * length;
            float sideX = -directionY;
            float sideY = directionX;
            Color color = flame.color();
            float flameAlpha = alpha * color.getAlpha() / COLOR_CHANNEL_MAX;
            flameVertex(color, 0f, textureRight, textureTop, backX + sideX * halfWidth / 2f, backY + sideY * halfWidth / 2f);
            flameVertex(color, 0f, textureRight, 0f, backX - sideX * halfWidth / 2f, backY - sideY * halfWidth / 2f);
            flameVertex(color, flameAlpha, 0f, 0f, slotX - sideX * halfWidth, slotY - sideY * halfWidth);
            flameVertex(color, flameAlpha, 0f, textureTop, slotX + sideX * halfWidth, slotY + sideY * halfWidth);
            flameVertex(color, flameAlpha, 0f, 0f, slotX - sideX * halfWidth, slotY - sideY * halfWidth);
            flameVertex(color, flameAlpha, 0f, textureTop, slotX + sideX * halfWidth, slotY + sideY * halfWidth);
            flameVertex(color, 0f, textureRight, textureTop, tipX + sideX * halfWidth / 2f, tipY + sideY * halfWidth / 2f);
            flameVertex(color, 0f, textureRight, 0f, tipX - sideX * halfWidth / 2f, tipY - sideY * halfWidth / 2f);
        }
    }

    void addGlows(TexturedQuadBatch quads, float screenX, float screenY, double facingRad, float screenScale, float glowFactor,
                  float alpha) {
        float facingCos = (float) Math.cos(facingRad);
        float facingSin = (float) Math.sin(facingRad);
        for (FleetHullVisuals.EngineFlame flame : visuals.flames()) {
            float slotX = screenX + (flame.forwardOffset() * facingCos - flame.leftOffset() * facingSin) * screenScale;
            float slotY = screenY + (flame.forwardOffset() * facingSin + flame.leftOffset() * facingCos) * screenScale;
            float glowSize = flame.length() * screenScale * glowFactor;
            quads.quadAtCenter(slotX, slotY, glowSize, glowSize, 0f, flame.color(), alpha);
        }
    }

    private static void flameVertex(Color color, float alpha, float textureX, float textureY, float x, float y) {
        GL11.glColor4f(color.getRed() / COLOR_CHANNEL_MAX, color.getGreen() / COLOR_CHANNEL_MAX, color.getBlue() / COLOR_CHANNEL_MAX, alpha);
        GL11.glTexCoord2f(textureX, textureY);
        GL11.glVertex2f(x, y);
    }
}
