package exiledsector.charts.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.Set;

public abstract class ChartRunIntel extends BaseIntelPlugin implements ChartRunBound {

    static final String ICON_CATEGORY = "intel";
    static final String ICON_ID = "discovered_entity";
    static final float STATUS_PAD = 3f;
    private static final Color STATUS_COLOUR = new Color(170, 170, 170);

    private final String runId;

    protected ChartRunIntel(String runId) {
        this.runId = runId;
    }

    public static String iconPath() {
        return Global.getSettings().getSpriteName(ICON_CATEGORY, ICON_ID);
    }

    public String runId() {
        return runId;
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return ChartRunBound.sameRun(runId, wantedRunId);
    }

    protected String listStatus() {
        return null;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(VanillaText.escapeFormat(getName()), getTitleColor(mode), 0f);
        String status = listStatus();
        if (status != null) {
            info.addPara(VanillaText.escapeFormat(status), STATUS_COLOUR, STATUS_PAD);
        }
    }

    @Override
    public String getIcon() {
        return iconPath();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_EXPLORATION);
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return null;
    }

    @Override
    public boolean shouldRemoveIntel() {
        return ChartRuns.unclosed(runId) == null;
    }
}
