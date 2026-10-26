package exiledsector.charts.wormhole;

import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import org.apache.log4j.Logger;

import java.io.IOException;

public class ChartWormholePlugin extends BaseCustomEntityPlugin {

    private static final Logger LOG = Logger.getLogger(ChartWormholePlugin.class);

    private transient ChartWormholeVisual visual;
    private transient boolean visualMissing;
    private transient boolean newlyCreated;

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        readResolve();
        newlyCreated = true;
    }

    Object readResolve() {
        visual = null;
        visualMissing = false;
        return this;
    }

    @Override
    public void advance(float amount) {
        if (entity != null && loadVisual()) {
            visual.advance(entity, amount);
        }
    }

    @Override
    public float getRenderRange() {
        float radius = entity == null ? 0f : entity.getRadius();
        return radius + radius * ChartWormholeVisual.VISIBLE_RANGE_SHARE;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (entity != null && loadVisual()) {
            visual.render(entity, viewport);
        }
    }

    private boolean loadVisual() {
        if (visual != null) {
            return true;
        }
        if (visualMissing) {
            return false;
        }
        try {
            visual = ChartWormholeVisual.create(entity, newlyCreated);
        } catch (IOException | RuntimeException e) {
            LOG.warn("[ExiledSector] The Star Chart wormhole sprites could not be loaded; the wormhole draws nothing", e);
            visual = null;
        }
        visualMissing = visual == null;
        return !visualMissing;
    }
}
