package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunBound;
import exiledsector.charts.run.ChartRuns;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RelayLogIntel extends BaseIntelPlugin implements ChartRunBound {

    static final String ICON_CATEGORY = "intel";
    static final String ICON_ID = "discovered_entity";
    static final float PARAGRAPH_PAD = 10f;
    static final float LOG_PAD = 3f;

    record Entry(String relayName, String log) {
    }

    private final String runId;

    public RelayLogIntel(String runId) {
        this.runId = runId;
    }

    static RelayLogIntel report(SectorAPI sector, ChartRun run) {
        IntelManagerAPI manager = sector.getIntelManager();
        if (manager == null || run == null) {
            return null;
        }
        for (IntelInfoPlugin existing : manager.getIntel(RelayLogIntel.class)) {
            if (existing instanceof RelayLogIntel intel && intel.belongsToRun(run.runId())) {
                intel.sendUpdateIfPlayerHasIntel(null, false);
                return intel;
            }
        }
        RelayLogIntel intel = new RelayLogIntel(run.runId());
        manager.addIntel(intel, false);
        return intel;
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return runId != null && runId.equals(wantedRunId);
    }

    static List<Entry> entries(ChartRun run, AtlasAnomalyRecord.Chain stage) {
        List<Entry> entries = new ArrayList<>();
        for (RelayKind kind : RelayKind.values()) {
            if (RelayStates.isWoken(run, kind)) {
                entries.add(new Entry(Translation.gameText(RelayText.PREFIX + kind.key() + ".name"), Translation.gameText(RelayText.logKey(kind, stage))));
            }
        }
        return entries;
    }

    @Override
    protected String getName() {
        return Translation.gameText(RelayText.PREFIX + "logIntel.title");
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(VanillaText.escapeFormat(getName()), getTitleColor(mode), 0f);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        ChartRun run = ChartRuns.unclosed(runId);
        if (run == null) {
            VanillaText.plainPara(info, Translation.gameText("chart.wormhole.dialog.stale"), 0f, Misc.getTextColor());
            return;
        }
        AtlasAnomalyRecord.Chain stage = RelayStages.liveStage(Global.getSector(), run);
        VanillaText.plainPara(info, Translation.gameText(RelayText.trailKey(stage)), 0f, Misc.getTextColor());
        for (Entry entry : entries(run, stage)) {
            VanillaText.plainPara(info, entry.relayName(), PARAGRAPH_PAD, Misc.getHighlightColor());
            VanillaText.plainPara(info, entry.log(), LOG_PAD, Misc.getTextColor());
        }
        VanillaText.plainPara(info, RelayText.status(run, stage), PARAGRAPH_PAD, Misc.getGrayColor());
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName(ICON_CATEGORY, ICON_ID);
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
