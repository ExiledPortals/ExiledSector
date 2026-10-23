package exiledsector.charts.expansion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.util.Misc;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class BreachCorePlugin extends BaseCustomEntityPlugin {

    static final float HOLE_RADIUS = 40f;
    static final float HOLE_FADE_SHARE = 1.3f;
    static final float HALO_SIZE = 410f;
    static final float RING_SIZE = HOLE_RADIUS * 2.6f;
    static final float PULSE_SECONDS = 3f;
    static final float PULSE_DEPTH = 0.2f;
    static final float RING_DEGREES_PER_SECOND = -30f;
    static final int HOLE_SEGMENTS = 64;
    static final Color HALO_COLOUR = new Color(255, 170, 255);
    static final Color RING_COLOUR = new Color(255, 170, 255);
    static final String HALO_TEXTURE = "graphics/fx/star_halo.png";
    private static final SpriteCache SPRITES = new SpriteCache(BreachCorePlugin.class);

    private transient SpriteAPI haloSprite;
    private transient SpriteAPI ringSprite;
    private transient float elapsedSeconds;

    @Override
    public void advance(float amount) {
        elapsedSeconds = (elapsedSeconds + amount) % (PULSE_SECONDS * 120f);
    }

    @Override
    public float getRenderRange() {
        return HALO_SIZE;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (entity == null || !loadSprites()) {
            return;
        }
        Vector2f at = entity.getLocation();
        float alpha = viewport.getAlphaMult();
        float pulse = 1f - PULSE_DEPTH + PULSE_DEPTH * (float) Math.sin(elapsedSeconds / PULSE_SECONDS * Math.PI * 2d);
        haloSprite.setSize(HALO_SIZE * pulse, HALO_SIZE * pulse);
        haloSprite.setColor(HALO_COLOUR);
        haloSprite.setAlphaMult(alpha);
        haloSprite.setAdditiveBlend();
        haloSprite.renderAtCenter(at.x, at.y);
        drawHole(at, alpha);
        ringSprite.setSize(RING_SIZE, RING_SIZE);
        ringSprite.setColor(RING_COLOUR);
        ringSprite.setAlphaMult(alpha);
        ringSprite.setAdditiveBlend();
        ringSprite.setAngle(elapsedSeconds * RING_DEGREES_PER_SECOND);
        ringSprite.renderAtCenter(at.x, at.y);
    }

    private static void drawHole(Vector2f at, float alpha) {
        UnitCircle circle = UnitCircle.of(HOLE_SEGMENTS);
        try (GlScope ignored = GlScope.flat()) {
            Misc.setColor(Color.BLACK, alpha);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(at.x, at.y);
            for (int i = 0; i <= HOLE_SEGMENTS; i++) {
                int segment = i % HOLE_SEGMENTS;
                GL11.glVertex2f(at.x + circle.cos(segment) * HOLE_RADIUS, at.y + circle.sin(segment) * HOLE_RADIUS);
            }
            GL11.glEnd();
            HoloShapes.radialGlow(at.x, at.y, HOLE_RADIUS, HOLE_RADIUS * HOLE_FADE_SHARE, Color.BLACK, alpha);
        }
    }

    private boolean loadSprites() {
        if (haloSprite != null && ringSprite != null) {
            return true;
        }
        try {
            haloSprite = SPRITES.sprite(HALO_TEXTURE);
            ringSprite = Global.getSettings().getSprite("misc", "wormhole_ring");
        } catch (RuntimeException e) {
            haloSprite = null;
            ringSprite = null;
        }
        return haloSprite != null && ringSprite != null;
    }
}
