package exiledsector.charts.layout.abyss;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.EnumSet;

public class AbyssalDarknessTerrainPlugin extends BaseTerrain {

    public static final String TERRAIN_ID = "exiledSector_chart_abyssal_darkness";
    static final float RENDER_RANGE = 100000f;
    static final float VIGNETTE_ALPHA = 0.85f;
    static final float VIGNETTE_OVERHANG = 400f;
    static final float BACKGROUND_FADE_OUT_SECONDS = 1f;
    static final float MAP_INNER_HALO_ALPHA = 0.6f;
    static final float MAP_OUTER_HALO_ALPHA = 0.8f;

    public record Params(AbyssPalette palette) {
    }

    private Color darkColour;
    private float vignetteLevel;

    @Override
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);
        if (param instanceof Params params) {
            darkColour = params.palette().darkColour();
            vignetteLevel = params.palette().vignette();
        }
    }

    @Override
    protected boolean shouldCheckFleetsToApplyEffect() {
        return false;
    }

    @Override
    public void advance(float amount) {
        LocationAPI location = entity == null ? null : entity.getContainingLocation();
        if (darkColour == null || location == null || !playerIsIn(location)) {
            return;
        }
        location.getBackgroundColorShifter().shift(this, darkColour, 0f, BACKGROUND_FADE_OUT_SECONDS, 1f);
    }

    private static boolean playerIsIn(LocationAPI location) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        return player != null && player.getContainingLocation() == location;
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return EnumSet.of(CampaignEngineLayers.ABOVE);
    }

    @Override
    public float getRenderRange() {
        return RENDER_RANGE;
    }

    @Override
    public boolean containsPoint(Vector2f point, float radius) {
        return false;
    }

    @Override
    public boolean hasTooltip() {
        return false;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (darkColour == null || layer != CampaignEngineLayers.ABOVE || !playerIsIn(entity.getContainingLocation())) {
            return;
        }
        SpriteAPI vignette = AbyssArt.sprite(AbyssArt.VIGNETTE);
        if (vignette == null) {
            return;
        }
        vignette.setSize(viewport.getVisibleWidth() + VIGNETTE_OVERHANG, viewport.getVisibleHeight() + VIGNETTE_OVERHANG);
        vignette.setColor(Color.WHITE);
        vignette.setAlphaMult(VIGNETTE_ALPHA * vignetteLevel * viewport.getAlphaMult());
        vignette.setAngle(0f);
        vignette.setNormalBlend();
        vignette.render(viewport.getLLX() - VIGNETTE_OVERHANG / 2f, viewport.getLLY() - VIGNETTE_OVERHANG / 2f);
    }

    @Override
    public void renderOnMap(float factor, float alphaMult) {
        SpriteAPI halo = AbyssArt.sprite(AbyssArt.HALO);
        if (halo == null || entity == null || entity.getContainingLocation() == null) {
            return;
        }
        for (CustomCampaignEntityAPI photosphere : entity.getContainingLocation().getCustomEntities()) {
            if (!(photosphere.getCustomPlugin() instanceof ChartPhotospherePlugin plugin) || plugin.colour() == null) {
                continue;
            }
            float x = photosphere.getLocation().x * factor;
            float y = photosphere.getLocation().y * factor;
            ChartPhotospherePlugin.drawAdditive(halo, x, y, ChartPhotospherePlugin.innerHaloSize(plugin.lightRadius()) * factor, plugin.colour(),
                    alphaMult * MAP_INNER_HALO_ALPHA * ChartPhotospherePlugin.INNER_HALO_ALPHA);
            ChartPhotospherePlugin.drawAdditive(halo, x, y, ChartPhotospherePlugin.outerHaloSize(plugin.lightRadius()) * factor, plugin.colour(),
                    alphaMult * MAP_OUTER_HALO_ALPHA * ChartPhotospherePlugin.OUTER_HALO_ALPHA);
        }
    }
}
