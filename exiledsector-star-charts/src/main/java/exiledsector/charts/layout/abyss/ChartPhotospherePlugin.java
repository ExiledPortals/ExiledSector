package exiledsector.charts.layout.abyss;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import exiledsector.ui.belt.AuroraBeltRenderer;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class ChartPhotospherePlugin extends BaseCustomEntityPlugin {

    public static final String ENTITY_TYPE = "exiledSector_chart_photosphere";
    static final float CORE_SIZE_MULT = 1.8f;
    static final float BAND_INNER_MULT = 0.3f;
    static final float BAND_OUTER_MULT = 1.3f;
    static final float BAND_ALPHA = 0.5f;
    static final float INNER_HALO_SHARE = 0.05f;
    static final float OUTER_HALO_SHARE = 0.5f;
    static final float INNER_HALO_ALPHA = 0.3f;
    static final float OUTER_HALO_ALPHA = 0.22f;
    static final float ANIMATION_WRAP_SECONDS = 3600f;

    public record Params(Color colour, float lightRadius) {
    }

    private Color colour;
    private float lightRadius;
    private transient float elapsedSeconds;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        if (pluginParams instanceof Params params) {
            colour = params.colour();
            lightRadius = params.lightRadius();
        }
    }

    public Color colour() {
        return colour;
    }

    public float lightRadius() {
        return lightRadius;
    }

    static float outerHaloSize(float lightRadius) {
        return lightRadius * OUTER_HALO_SHARE;
    }

    static float innerHaloSize(float lightRadius) {
        return lightRadius * INNER_HALO_SHARE;
    }

    @Override
    public void advance(float amount) {
        elapsedSeconds = (elapsedSeconds + amount) % ANIMATION_WRAP_SECONDS;
    }

    @Override
    public float getRenderRange() {
        return (entity == null ? 0f : entity.getRadius()) + outerHaloSize(lightRadius) * 0.5f;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (entity == null || colour == null) {
            return;
        }
        Vector2f at = entity.getLocation();
        float alphaMult = viewport.getAlphaMult();
        if (layer == CampaignEngineLayers.TERRAIN_7A) {
            renderCore(at, entity.getRadius(), alphaMult, viewport.getViewMult());
        } else if (layer == CampaignEngineLayers.ABOVE) {
            renderHalo(at, lightRadius, alphaMult, colour);
        }
    }

    private void renderCore(Vector2f at, float radius, float alphaMult, float viewMult) {
        SpriteAPI band = AbyssArt.texture(AbyssArt.BAND);
        if (band != null) {
            AuroraBeltRenderer.render(band, new RadialBand(at, radius * BAND_INNER_MULT, radius * BAND_OUTER_MULT), colour, alphaMult * BAND_ALPHA,
                    elapsedSeconds, viewMult > 0f ? 1f / viewMult : 1f);
        }
        SpriteDraw.drawAtCenter(AbyssArt.sprite(AbyssArt.CORE), at.x, at.y, radius * CORE_SIZE_MULT, radius * CORE_SIZE_MULT, colour, alphaMult, 0f);
    }

    static void renderHalo(Vector2f at, float lightRadius, float alphaMult, Color colour) {
        SpriteAPI halo = AbyssArt.sprite(AbyssArt.HALO);
        if (halo == null) {
            return;
        }
        float inner = innerHaloSize(lightRadius);
        float outer = outerHaloSize(lightRadius);
        drawAdditive(halo, at.x, at.y, inner, colour, alphaMult * INNER_HALO_ALPHA);
        drawAdditive(halo, at.x, at.y, outer, colour, alphaMult * OUTER_HALO_ALPHA);
    }

    static void drawAdditive(SpriteAPI halo, float x, float y, float size, Color colour, float alpha) {
        halo.setSize(size, size);
        halo.setColor(colour);
        halo.setAlphaMult(alpha);
        halo.setAngle(0f);
        halo.setAdditiveBlend();
        halo.renderAtCenter(x, y);
    }
}
