package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.anomaly.ChartAnomalyTerrain;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;
import exiledsector.ui.belt.RadialBand;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumSet;

public class EventHorizonTerrainPlugin extends ChartAnomalyTerrain {

    private static final EnumSet<CampaignEngineLayers> LAYERS = EnumSet.of(CampaignEngineLayers.TERRAIN_1);

    public record Params(float radius) {
    }

    private float radius;
    private transient RadialBand rimBand;
    private transient float elapsedSeconds;

    @Override
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);
        if (param instanceof Params params) {
            radius = Math.max(0f, params.radius());
        }
    }

    public float radius() {
        return radius;
    }

    @Override
    protected String nameKey() {
        return "chart.anomaly.black_hole.eventHorizon.name";
    }

    @Override
    protected void advanceLive(float amount) {
        elapsedSeconds = (elapsedSeconds + amount) % BreachTerrainPlugin.ANIMATION_WRAP_SECONDS;
    }

    @Override
    public boolean containsPoint(Vector2f point, float pointRadius) {
        return radius > 0f && entity != null && point != null && Misc.getDistance(entity.getLocation(), point) < radius + pointRadius;
    }

    @Override
    public float getRenderRange() {
        return radius + BreachTerrainPlugin.RENDER_MARGIN;
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return LAYERS;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (entity == null || radius <= 0f) {
            return;
        }
        rimBand = BreachFieldRenderer.rimBand(entity.getLocation(), radius, rimBand);
        BreachFieldRenderer.render(rimBand, radius, viewport.getAlphaMult(), elapsedSeconds, viewport.getViewMult());
    }

    @Override
    protected void tooltipLines(TooltipMakerAPI tooltip) {
        VanillaText.plainPara(tooltip, Translation.gameText("chart.anomaly.black_hole.eventHorizon.description"), TOOLTIP_PAD, Misc.getTextColor());
    }

    @Override
    public float getMaxEffectRadius(Vector2f locFrom) {
        return radius;
    }

    @Override
    public float getOptimalEffectRadius(Vector2f locFrom) {
        return radius * 0.5f;
    }
}
