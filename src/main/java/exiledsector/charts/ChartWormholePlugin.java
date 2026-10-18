package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class ChartWormholePlugin extends BaseCustomEntityPlugin {

    static final String SPRITE_CATEGORY = "misc";
    static final String OUTER_RING_SPRITE = "wormhole_ring";
    static final String INNER_RING_SPRITE = "wormhole_ring2";
    static final float RING_SIZE = 160f;
    static final float OUTER_TURN_DEGREES_PER_SECOND = 12f;
    static final float INNER_TURN_DEGREES_PER_SECOND = -20f;
    static final float PULSE_RADIANS_PER_SECOND = 1.6f;
    static final float PULSE_SIZE_SHARE = 0.06f;
    static final float FULL_TURN_DEGREES = 360f;
    static final float FULL_PULSE_RADIANS = (float) (Math.PI * 2);
    private static final Color RING_COLOUR = new Color(150, 190, 255);
    private static final Color CORE_COLOUR = new Color(210, 170, 255);
    private static final Logger LOG = Logger.getLogger(ChartWormholePlugin.class);

    private transient SpriteAPI outerRing;
    private transient SpriteAPI innerRing;
    private transient boolean spritesMissing;
    private transient float outerTurnDegrees;
    private transient float innerTurnDegrees;
    private transient float pulseRadians;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        readResolve();
    }

    Object readResolve() {
        outerRing = null;
        innerRing = null;
        spritesMissing = false;
        return this;
    }

    @Override
    public void advance(float amount) {
        outerTurnDegrees = wrap(outerTurnDegrees + amount * OUTER_TURN_DEGREES_PER_SECOND, FULL_TURN_DEGREES);
        innerTurnDegrees = wrap(innerTurnDegrees + amount * INNER_TURN_DEGREES_PER_SECOND, FULL_TURN_DEGREES);
        pulseRadians = wrap(pulseRadians + amount * PULSE_RADIANS_PER_SECOND, FULL_PULSE_RADIANS);
    }

    static float wrap(float value, float period) {
        float wrapped = value % period;
        return wrapped < 0f ? wrapped + period : wrapped;
    }

    @Override
    public float getRenderRange() {
        return RING_SIZE + (entity == null ? 0f : entity.getRadius()) + 100f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (entity == null || !loadSprites()) {
            return;
        }
        float alphaMult = viewport.getAlphaMult() * entity.getSensorFaderBrightness() * entity.getSensorContactFaderBrightness();
        if (alphaMult <= 0f) {
            return;
        }
        Vector2f entityLocation = entity.getLocation();
        float pulse = 1f + PULSE_SIZE_SHARE * (float) Math.sin(pulseRadians);
        drawRing(outerRing, RING_COLOUR, RING_SIZE * pulse, outerTurnDegrees, alphaMult, entityLocation);
        drawRing(innerRing, CORE_COLOUR, RING_SIZE * 0.7f / pulse, innerTurnDegrees, alphaMult * 0.8f, entityLocation);
    }

    private static void drawRing(SpriteAPI ring, Color colour, float ringSize, float turnDegrees, float alphaMult, Vector2f centre) {
        ring.setSize(ringSize, ringSize);
        ring.setColor(colour);
        ring.setAngle(turnDegrees);
        ring.setAlphaMult(alphaMult);
        ring.setAdditiveBlend();
        ring.renderAtCenter(centre.x, centre.y);
    }

    private boolean loadSprites() {
        if (outerRing != null && innerRing != null) {
            return true;
        }
        if (spritesMissing) {
            return false;
        }
        try {
            outerRing = Global.getSettings().getSprite(SPRITE_CATEGORY, OUTER_RING_SPRITE);
            innerRing = Global.getSettings().getSprite(SPRITE_CATEGORY, INNER_RING_SPRITE);
        } catch (RuntimeException e) {
            LOG.warn("[ExiledSector] The Star Chart wormhole sprites could not be loaded; the wormhole draws nothing", e);
            outerRing = null;
            innerRing = null;
        }
        spritesMissing = outerRing == null || innerRing == null;
        return !spritesMissing;
    }
}
