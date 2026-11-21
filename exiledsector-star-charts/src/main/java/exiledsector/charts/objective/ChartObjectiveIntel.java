package exiledsector.charts.objective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.skirmish.SkirmishFight;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunIntel;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

public class ChartObjectiveIntel extends ChartRunIntel {

    static final float PARAGRAPH_PAD = 10f;

    public ChartObjectiveIntel(String runId) {
        super(runId);
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

    public static String objectiveName(ChartRun run) {
        return run.hasBoss() ? Translation.gameText("chart.objective.boss.name") : "";
    }

    private static void addOfficerParas(TooltipMakerAPI info, ChartRun run) {
        SkirmishFight fight = SkirmishFight.of(run.atlasNode());
        if (fight != null && fight.betweenOfficers()) {
            addOfficerPara(info, run, fight.attackerRecordId(), "chart.intel.officerAttacks", true);
            addOfficerPara(info, run, fight.defenderRecordId(), "chart.intel.officerDefends", true);
            return;
        }
        Integer officerRecordId = OfficerFleets.recordIdOf(run.atlasNode());
        if (officerRecordId != null) {
            boolean attacks = fight != null;
            addOfficerPara(info, run, officerRecordId, attacks ? "chart.intel.officerAttacks" : "chart.intel.officer", attacks);
        }
    }

    private static void addOfficerPara(TooltipMakerAPI info, ChartRun run, int officerRecordId, String key, boolean fightsHere) {
        AtlasState state = AtlasState.getIfCreated(Global.getSector());
        AtlasOfficer officer = state == null ? null : state.officer(officerRecordId);
        if (officer == null) {
            AtlasOfficer fallen = state == null ? null : OfficerTurns.fallenOfficer(state, officerRecordId);
            if (fallen != null) {
                info.addPara(safe(I18n.forGameText(() -> Translation.msg("chart.intel.officerDied").arg("title", OfficerText.title(fallen)).text())),
                        PARAGRAPH_PAD);
            }
            return;
        }
        info.addPara(safe(I18n.forGameText(() -> Translation.msg(key).arg("title", OfficerText.title(officer))
                .arg("faction", ChartFactions.displayName(officer.factionId())).text())), PARAGRAPH_PAD);
        for (AtlasLine line : I18n.forGameText(() -> OfficerText.detailLines(officer))) {
            VanillaText.addPara(info, line.styled(), PARAGRAPH_PAD, Misc.getTextColor());
        }
        if (!officer.isActive() || (!fightsHere && officer.commandCentre().pack() != run.atlasNode().cell())) {
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
        return null;
    }

    static String safe(String text) {
        return text == null ? "" : text.replace("%", "%%");
    }

    @Override
    protected String getName() {
        ChartRun run = ChartRuns.unclosed(runId());
        String objective = run == null ? "" : objectiveName(run);
        return I18n.forGameText(() -> Translation.msg("chart.intel.title").arg("objective", objective).text());
    }

    @Override
    protected String listStatus() {
        ChartRun run = ChartRuns.unclosed(runId());
        return run == null ? null : progressText(run);
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        ChartRun run = ChartRuns.unclosed(runId());
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
    }

}
