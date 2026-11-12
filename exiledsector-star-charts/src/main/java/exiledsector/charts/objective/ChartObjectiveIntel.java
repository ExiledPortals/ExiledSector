package exiledsector.charts.objective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunBound;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.Set;

public class ChartObjectiveIntel extends BaseIntelPlugin implements ChartRunBound {

    static final String ICON_CATEGORY = "intel";
    static final String ICON_ID = "discovered_entity";
    static final float PARAGRAPH_PAD = 10f;
    static final float BULLET_PAD = 3f;
    private static final Color PROGRESS_COLOUR = new Color(170, 170, 170);

    private final String runId;

    public ChartObjectiveIntel(String runId) {
        this.runId = runId;
    }

    public static ChartObjectiveIntel addFor(SectorAPI sector, ChartRun run) {
        if (!run.hasBoss() || sector.getIntelManager() == null) {
            return null;
        }
        ChartObjectiveIntel intel = new ChartObjectiveIntel(run.runId());
        intel.setImportant(true);
        sector.getIntelManager().addIntel(intel, true);
        return intel;
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return runId != null && runId.equals(wantedRunId);
    }

    String runId() {
        return runId;
    }

    public static String objectiveName(ChartRun run) {
        return run.hasBoss() ? Translation.gameText("chart.objective.boss.name") : "";
    }

    private static void addOfficerParas(TooltipMakerAPI info, ChartRun run) {
        Integer officerRecordId = OfficerFleets.recordIdOf(run.atlasNode());
        if (officerRecordId == null) {
            return;
        }
        AtlasOfficer officer = OfficerFleets.officerOf(run.atlasNode());
        if (officer == null) {
            AtlasState state = AtlasState.getIfCreated(Global.getSector());
            AtlasOfficer fallen = state == null ? null : OfficerTurns.fallenOfficer(state, officerRecordId);
            if (fallen != null) {
                info.addPara(safe(I18n.forGameText(() -> Translation.msg("chart.intel.officerDied").arg("title", OfficerText.title(fallen)).text())),
                        PARAGRAPH_PAD);
            }
            return;
        }
        info.addPara(safe(I18n.forGameText(() -> Translation.msg("chart.intel.officer").arg("title", OfficerText.title(officer))
                .arg("faction", ChartFactions.displayName(officer.factionId())).text())), PARAGRAPH_PAD);
        for (AtlasLine line : I18n.forGameText(() -> OfficerText.detailLines(officer))) {
            VanillaText.addPara(info, line.styled(), PARAGRAPH_PAD, Misc.getTextColor());
        }
        if (!officer.isActive() || officer.commandCentre().pack() != run.atlasNode().cell()) {
            info.addPara(safe(I18n.forGameText(() -> Translation.msg("chart.intel.officerFled").arg("title", OfficerText.title(officer)).text())),
                    PARAGRAPH_PAD);
        }
    }

    public static String descriptionKey(ChartRun run) {
        return run.hasBoss() ? "chart.objective.boss." + run.bossType().key() + ".description" : "chart.wormhole.dialog.stale";
    }

    public static String progressText(ChartRun run) {
        if (run.isObjectiveComplete()) {
            return Translation.gameText("chart.intel.complete");
        }
        if (run.isObjectiveLost()) {
            return Translation.gameText("chart.intel.lost");
        }
        return null;
    }

    static String safe(String text) {
        return text == null ? "" : text.replace("%", "%%");
    }

    @Override
    protected String getName() {
        ChartRun run = ChartRuns.unclosed(runId);
        String objective = run == null ? "" : objectiveName(run);
        return I18n.forGameText(() -> Translation.msg("chart.intel.title").arg("objective", objective).text());
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(safe(getName()), getTitleColor(mode), 0f);
        ChartRun run = ChartRuns.unclosed(runId);
        String progress = run == null ? null : progressText(run);
        if (progress != null) {
            info.addPara(safe(progress), PROGRESS_COLOUR, BULLET_PAD);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        ChartRun run = ChartRuns.unclosed(runId);
        if (run == null || !run.hasBoss()) {
            info.addPara(safe(Translation.gameText("chart.wormhole.dialog.stale")), 0f);
            return;
        }
        int tier = run.tier();
        String faction = ChartFactions.displayName(run.contentFaction());
        info.addPara(safe(Translation.gameText(descriptionKey(run))), 0f);
        info.addPara(safe(I18n.forGameText(() -> Translation.msg("chart.intel.tier").arg("tier", tier).arg("faction", faction).text())),
                PARAGRAPH_PAD);
        addOfficerParas(info, run);
        String progress = progressText(run);
        if (progress != null) {
            info.addPara(safe(progress), PARAGRAPH_PAD);
        }
        AnomalyRun anomalyRun = AnomalyRun.live(Global.getSector());
        if (anomalyRun != null && anomalyRun.run() == run) {
            for (String line : ChartAnomalies.intelLines(anomalyRun)) {
                info.addPara(safe(line), PARAGRAPH_PAD);
            }
        }
        if (!run.isObjectiveComplete() && !run.isObjectiveLost()) {
            info.addPara(safe(Translation.gameText("chart.intel.reward")), PARAGRAPH_PAD);
        }
    }

    public static String iconPath() {
        return Global.getSettings().getSpriteName(ICON_CATEGORY, ICON_ID);
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
