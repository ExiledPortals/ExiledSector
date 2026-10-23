package exiledsector.charts.expansion;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class BreachCorePlugin extends BaseCustomEntityPlugin {

    static final float GLOW_SIZE = 220f;
    static final float RING_SIZE = 160f;
    static final float PULSE_SECONDS = 3f;
    static final float PULSE_DEPTH = 0.25f;
    static final float RING_DEGREES_PER_SECOND = 40f;
    static final Color GLOW_COLOUR = new Color(210, 120, 255);
    static final Color RING_COLOUR = new Color(255, 220, 255);

    private transient SpriteAPI glowSprite;
    private transient SpriteAPI ringSprite;
    private transient float elapsedSeconds;

    @Override
    public void advance(float amount) {
        elapsedSeconds = (elapsedSeconds + amount) % (PULSE_SECONDS * 120f);
    }

    @Override
    public float getRenderRange() {
        return GLOW_SIZE * 2f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (entity == null || !loadSprites()) {
            return;
        }
        Vector2f at = entity.getLocation();
        float alpha = viewport.getAlphaMult();
        float pulse = 1f - PULSE_DEPTH + PULSE_DEPTH * (float) Math.sin(elapsedSeconds / PULSE_SECONDS * Math.PI * 2d);
        glowSprite.setSize(GLOW_SIZE * pulse, GLOW_SIZE * pulse);
        glowSprite.setColor(GLOW_COLOUR);
        glowSprite.setAlphaMult(alpha);
        glowSprite.setAdditiveBlend();
        glowSprite.renderAtCenter(at.x, at.y);
        ringSprite.setSize(RING_SIZE, RING_SIZE);
        ringSprite.setColor(RING_COLOUR);
        ringSprite.setAlphaMult(alpha);
        ringSprite.setAdditiveBlend();
        ringSprite.setAngle(elapsedSeconds * RING_DEGREES_PER_SECOND);
        ringSprite.renderAtCenter(at.x, at.y);
    }

    private boolean loadSprites() {
        if (glowSprite != null && ringSprite != null) {
            return true;
        }
        try {
            glowSprite = Global.getSettings().getSprite("misc", "fx_radial");
            ringSprite = Global.getSettings().getSprite("misc", "wormhole_ring");
        } catch (RuntimeException e) {
            glowSprite = null;
            ringSprite = null;
        }
        return glowSprite != null && ringSprite != null;
    }
}
