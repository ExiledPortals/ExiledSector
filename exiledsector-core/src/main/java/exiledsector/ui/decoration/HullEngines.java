package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TexturedQuadBatch;
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
    private static final FlameBatch SHARED_FLAME_BATCH = new FlameBatch();

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

    FleetHullVisuals visuals() {
        return visuals;
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
        drawSprite(spriteCache, spritePath, screenX, screenY, hullWidth, hullHeight, visuals.centerX(), visuals.centerY(), screenScale, facingDeg,
                alpha);
    }

    static void drawSprite(SpriteCache spriteCache, String spritePath, float screenX, float screenY, float spriteWidth, float spriteHeight,
                           float centerX, float centerY, float screenScale, float facingDeg, float alpha) {
        float angleDeg = facingDeg - SPRITE_ANGLE_OFFSET;
        if (!Float.isNaN(centerX) && !Float.isNaN(centerY)) {
            SpriteDraw.drawAtPivot(spriteCache, spritePath, screenX, screenY, spriteWidth, spriteHeight,
                    centerX * screenScale, centerY * screenScale, Color.WHITE, alpha, angleDeg);
        } else {
            SpriteDraw.drawAtCenter(spriteCache, spritePath, screenX, screenY, spriteWidth, spriteHeight, Color.WHITE, alpha, angleDeg);
        }
    }

    public void drawEngines(SpriteCache spriteCache, TexturedQuadBatch glowQuads, float screenX, float screenY, float facingDeg,
                            float screenScale, float engineLevel, float alpha) {
        if (alpha <= 0f || !hasEngines()) {
            return;
        }
        double facingRad = Math.toRadians(facingDeg);
        SpriteAPI flameTexture = spriteCache.texture(FLAME_PATH);
        if (flameTexture != null) {
            try (FlameBatch flames = SHARED_FLAME_BATCH.begin(flameTexture)) {
                addFlames(flames, screenX, screenY, facingRad, screenScale, screenScale, flameLengthFactor(engineLevel), alpha);
            }
        }
        SpriteAPI glowTexture = spriteCache.texture(GLOW_PATH);
        if (glowTexture != null) {
            try (TexturedQuadBatch quads = glowQuads.beginAdditive(glowTexture)) {
                addGlows(quads, screenX, screenY, facingRad, screenScale, glowFactor(engineLevel), alpha);
            }
        }
    }

    void addFlames(FlameBatch flames, float screenX, float screenY, double facingRad, float screenScale, float detailScale, float lengthFactor,
                   float alpha) {
        float textureRight = flames.textureRight();
        float textureTop = flames.textureTop();
        float facingCos = (float) Math.cos(facingRad);
        float facingSin = (float) Math.sin(facingRad);
        for (FleetHullVisuals.EngineFlame flame : visuals.flames()) {
            float slotX = screenX + HullFrame.rotatedX(flame.forwardOffset(), flame.leftOffset(), facingCos, facingSin) * screenScale;
            float slotY = screenY + HullFrame.rotatedY(flame.forwardOffset(), flame.leftOffset(), facingCos, facingSin) * screenScale;
            float halfWidth = Math.max(detailScale, flame.width() * screenScale) / 2f;
            if (halfWidth < MIN_FLAME_HALF_WIDTH) continue;
            double flameAngle = facingRad + Math.toRadians(flame.angleDeg());
            float directionX = (float) Math.cos(flameAngle);
            float directionY = (float) Math.sin(flameAngle);
            float length = flame.length() * screenScale * lengthFactor;
            float lead = FLAME_LEAD * detailScale;
            float backX = slotX - directionX * lead;
            float backY = slotY - directionY * lead;
            float tipX = slotX + directionX * length;
            float tipY = slotY + directionY * length;
            float sideX = -directionY;
            float sideY = directionX;
            Color color = flame.color();
            float flameAlpha = alpha * color.getAlpha() / COLOR_CHANNEL_MAX;
            flames.vertex(color, 0f, textureRight, textureTop, backX + sideX * halfWidth / 2f, backY + sideY * halfWidth / 2f);
            flames.vertex(color, 0f, textureRight, 0f, backX - sideX * halfWidth / 2f, backY - sideY * halfWidth / 2f);
            flames.vertex(color, flameAlpha, 0f, 0f, slotX - sideX * halfWidth, slotY - sideY * halfWidth);
            flames.vertex(color, flameAlpha, 0f, textureTop, slotX + sideX * halfWidth, slotY + sideY * halfWidth);
            flames.vertex(color, flameAlpha, 0f, 0f, slotX - sideX * halfWidth, slotY - sideY * halfWidth);
            flames.vertex(color, flameAlpha, 0f, textureTop, slotX + sideX * halfWidth, slotY + sideY * halfWidth);
            flames.vertex(color, 0f, textureRight, textureTop, tipX + sideX * halfWidth / 2f, tipY + sideY * halfWidth / 2f);
            flames.vertex(color, 0f, textureRight, 0f, tipX - sideX * halfWidth / 2f, tipY - sideY * halfWidth / 2f);
        }
    }

    void addGlows(TexturedQuadBatch quads, float screenX, float screenY, double facingRad, float screenScale, float glowFactor,
                  float alpha) {
        float facingCos = (float) Math.cos(facingRad);
        float facingSin = (float) Math.sin(facingRad);
        for (FleetHullVisuals.EngineFlame flame : visuals.flames()) {
            float slotX = screenX + HullFrame.rotatedX(flame.forwardOffset(), flame.leftOffset(), facingCos, facingSin) * screenScale;
            float slotY = screenY + HullFrame.rotatedY(flame.forwardOffset(), flame.leftOffset(), facingCos, facingSin) * screenScale;
            float glowSize = flame.length() * screenScale * glowFactor;
            quads.quadAtCenter(slotX, slotY, glowSize, glowSize, 0f, flame.color(), alpha);
        }
    }
}
