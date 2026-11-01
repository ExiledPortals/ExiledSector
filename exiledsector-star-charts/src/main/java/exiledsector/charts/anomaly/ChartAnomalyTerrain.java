package exiledsector.charts.anomaly;

import com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;

public abstract class ChartAnomalyTerrain extends BaseTerrain {

    protected static final float TOOLTIP_PAD = 10f;

    protected abstract String nameKey();

    protected abstract void tooltipLines(TooltipMakerAPI tooltip);

    protected abstract void advanceLive(float amount);

    protected boolean isGone() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || entity == null || entity.getContainingLocation() == null || isGone()) {
            return;
        }
        super.advance(amount);
        advanceLive(amount);
    }

    @Override
    protected boolean shouldCheckFleetsToApplyEffect() {
        return false;
    }

    @Override
    public String getEffectCategory() {
        return getTerrainId();
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    public boolean isTooltipExpandable() {
        return false;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        I18n.forGameText(() -> {
            tooltip.addTitle(getTerrainName());
            tooltipLines(tooltip);
        });
    }

    @Override
    public String getTerrainName() {
        return Translation.gameText(nameKey());
    }

    @Override
    public String getNameForTooltip() {
        return getTerrainName();
    }
}
