package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.ScarText;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunIntel;
import exiledsector.charts.run.ChartRuns;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;

public class RelayLogIntel extends ChartRunIntel {

    static final float PARAGRAPH_PAD = 10f;
    static final float LOG_PAD = 3f;

    record Entry(String relayName, String log, RelayChoice pick) {
    }

    public RelayLogIntel(String runId) {
        super(runId);
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

    static List<Entry> entries(ChartRun run, AtlasAnomalyRecord.Chain stage) {
        List<Entry> entries = new ArrayList<>();
        for (RelayKind kind : RelayKind.values()) {
            if (RelayStates.isWoken(run, kind)) {
                entries.add(new Entry(Translation.gameText(RelayText.PREFIX + kind.key() + ".name"), Translation.gameText(RelayText.logKey(kind, stage)),
                        RelayChoices.picked(run, kind)));
            }
        }
        return entries;
    }

    @Override
    protected String getName() {
        return Translation.gameText(RelayText.PREFIX + "logIntel.title");
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        ChartRun run = ChartRuns.unclosed(runId());
        if (run == null) {
            VanillaText.plainPara(info, Translation.gameText("chart.wormhole.dialog.stale"), 0f, Misc.getTextColor());
            return;
        }
        AtlasAnomalyRecord.Chain stage = RelayStages.liveStage(Global.getSector(), run);
        VanillaText.plainPara(info, Translation.gameText(RelayText.trailKey(stage)), 0f, Misc.getTextColor());
        List<Entry> entries = entries(run, stage);
        HullFacts bossHull = entries.stream().anyMatch(entry -> entry.pick() != null) ? RelayChoices.bossHull(Global.getSector(), run) : null;
        for (Entry entry : entries) {
            VanillaText.plainPara(info, entry.relayName(), PARAGRAPH_PAD, Misc.getHighlightColor());
            VanillaText.plainPara(info, entry.log(), LOG_PAD, Misc.getTextColor());
            if (entry.pick() != null) {
                VanillaText.addPara(info, RelayText.pickedLine(entry.pick()), LOG_PAD, Misc.getTextColor());
                VanillaText.addPara(info, ScarText.effectsLine(entry.pick().scarId(), bossHull), LOG_PAD, Misc.getTextColor());
            }
        }
        VanillaText.plainPara(info, RelayText.status(run, stage), PARAGRAPH_PAD, Misc.getGrayColor());
    }

}
