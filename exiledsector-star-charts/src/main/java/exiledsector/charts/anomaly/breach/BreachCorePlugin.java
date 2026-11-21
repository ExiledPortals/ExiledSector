package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class BreachCorePlugin extends BaseCustomEntityPlugin {

    static final float HOLE_RADIUS = 40f;
    static final float HOLE_FADE_SHARE = 1.3f;
    public static final float HALO_SIZE = 410f;
    static final float RING_SIZE = HOLE_RADIUS * 2.6f;
    static final float PULSE_SECONDS = 3f;
    static final float PULSE_DEPTH = 0.2f;
    static final float RING_DEGREES_PER_SECOND = -30f;
    static final int HOLE_SEGMENTS = 64;
    static final Color HALO_COLOUR = new Color(255, 170, 255);
    static final Color RING_COLOUR = new Color(255, 170, 255);
    public static final String HALO_TEXTURE = "graphics/fx/star_halo.png";
    private static final SpriteCache SPRITES = new SpriteCache(BreachCorePlugin.class);

    private transient float shrink;
    private transient SpriteAPI haloSprite;
    private transient SpriteAPI ringSprite;
    private transient float elapsedSeconds;

    @Override
    public void advance(float amount) {
        elapsedSeconds = (elapsedSeconds + amount) % (PULSE_SECONDS * 120f);
    }

    void shrinkTo(float scale) {
        shrink = 1f - Math.max(0f, Math.min(1f, scale));
    }

    float scale() {
        return 1f - shrink;
    }

    @Override
    public float getRenderRange() {
        return HALO_SIZE;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        float scale = scale();
        if (entity == null || scale <= 0f || !loadSprites()) {
            return;
        }
        drawCore(haloSprite, ringSprite, entity.getLocation(), scale, viewport.getAlphaMult(), elapsedSeconds);
    }

    public static void drawCore(SpriteAPI halo, SpriteAPI ring, Vector2f at, float scale, float alpha, float elapsedSeconds) {
        float pulse = 1f - PULSE_DEPTH + PULSE_DEPTH * (float) Math.sin(elapsedSeconds / PULSE_SECONDS * Math.PI * 2d);
        halo.setSize(HALO_SIZE * pulse * scale, HALO_SIZE * pulse * scale);
        halo.setColor(HALO_COLOUR);
        halo.setAlphaMult(alpha);
        halo.setAdditiveBlend();
        halo.renderAtCenter(at.x, at.y);
        drawHole(at, HOLE_RADIUS * scale, alpha);
        ring.setSize(RING_SIZE * scale, RING_SIZE * scale);
        ring.setColor(RING_COLOUR);
        ring.setAlphaMult(alpha);
        ring.setAdditiveBlend();
        ring.setAngle(elapsedSeconds * RING_DEGREES_PER_SECOND);
        ring.renderAtCenter(at.x, at.y);
    }

    public static SpriteAPI loadRing() {
        try {
            return Global.getSettings().getSprite("misc", "wormhole_ring");
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void drawHole(Vector2f at, float holeRadius, float alpha) {
        try (GlScope ignored = GlScope.flat()) {
            HoloShapes.disc(at.x, at.y, holeRadius, HOLE_SEGMENTS, Color.BLACK, alpha);
            HoloShapes.radialGlow(at.x, at.y, holeRadius, holeRadius * HOLE_FADE_SHARE, Color.BLACK, alpha);
        }
    }

    private boolean loadSprites() {
        if (haloSprite != null && ringSprite != null) {
            return true;
        }
        try {
            haloSprite = SPRITES.sprite(HALO_TEXTURE);
        } catch (RuntimeException e) {
            haloSprite = null;
        }
        ringSprite = loadRing();
        return haloSprite != null && ringSprite != null;
    }
}
