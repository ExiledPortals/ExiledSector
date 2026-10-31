package exiledsector.charts.anomaly.heist;

import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

public class ListeningPostPlugin extends BaseCustomEntityPlugin {

    static final float TOOLTIP_WIDTH = 300f;
    static final float LINE_PAD = 3f;

    @Override
    public boolean hasCustomMapTooltip() {
        return true;
    }

    @Override
    public float getMapTooltipWidth() {
        return TOOLTIP_WIDTH;
    }

    @Override
    public void createMapTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        if (entity == null) {
            return;
        }
        tooltip.addTitle(VanillaText.escapeFormat(entity.getName()));
        addStatus(tooltip);
    }

    @Override
    public void appendToCampaignTooltip(TooltipMakerAPI tooltip, VisibilityLevel level) {
        addStatus(tooltip);
    }

    private void addStatus(TooltipMakerAPI tooltip) {
        PostState state = entity == null ? null : PostState.of(ChartRuns.liveIn(entity.getContainingLocation()));
        if (state == null) {
            return;
        }
        PostScript script = PostScript.in(entity.getContainingLocation());
        for (String line : PostText.lines(state, script)) {
            VanillaText.plainPara(tooltip, line, LINE_PAD, state.alarm() && !state.taken() ? Misc.getNegativeHighlightColor() : Misc.getTextColor());
        }
    }
}
