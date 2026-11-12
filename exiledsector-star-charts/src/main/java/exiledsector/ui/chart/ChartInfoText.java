package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.loot.ChartLootTotals;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerDefeat;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemName;
import exiledsector.items.SlottedLine;
import exiledsector.items.StarChart;
import exiledsector.ui.atlas.AtlasNodeText;
import exiledsector.ui.socket.AtlasAnomalyPanel.Entry;

import java.util.ArrayList;
import java.util.List;

final class ChartInfoText {

    record Objective(String name, StyledText body) {
    }

    record Content(ItemName chartName, String summary, String rewardButtonLabel, List<Entry> anomalies, Objective objective,
                   List<StyledText> rewardLines, List<SlottedLine> modifierLines) {
    }

    record Key(String runId, boolean objectiveComplete, boolean objectiveLost, List<String> anomalyIds, boolean rewardUnchosen) {

        static Key of(ChartRun run) {
            return new Key(run.runId(), run.isObjectiveComplete(), run.isObjectiveLost(), List.copyOf(run.anomalyIds()),
                    unchosenRewardHolder() != null);
        }

        boolean matches(ChartRun run) {
            return runId.equals(run.runId()) && objectiveComplete == run.isObjectiveComplete() && objectiveLost == run.isObjectiveLost()
                    && anomalyIds.equals(run.anomalyIds()) && rewardUnchosen == (unchosenRewardHolder() != null);
        }
    }

    private ChartInfoText() {
    }

    static Content of(ChartRun run) {
        StarChart chart = ChartRunEffects.chartOf(run);
        int tier = run.tier();
        String faction = ChartFactions.displayName(run.contentFaction());
        AtlasOfficer unchosenRewardHolder = unchosenRewardHolder();
        return I18n.forGameText(() -> new Content(chart == null ? null : chart.displayName(),
                Translation.msg("chart.intel.tier").arg("tier", tier).arg("faction", faction).text(),
                unchosenRewardHolder == null ? null
                        : Translation.msg("chart.panel.chooseReward").arg("officer", OfficerText.title(unchosenRewardHolder)).text(),
                anomalies(run), objective(run),
                ChartLootTotals.lines(ChartRunEffects.contextOf(run)), modifierLines(chart)));
    }

    static AtlasOfficer unchosenRewardHolder() {
        return OfficerDefeat.nextUnchosen(AtlasState.getIfCreated(Global.getSector()));
    }

    static List<Entry> anomalies(ChartRun run) {
        List<Entry> entries = new ArrayList<>();
        for (String anomalyId : run.anomalyIds()) {
            ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
            entries.add(new Entry(AtlasNames.anomaly(anomalyId), definition == null ? "" : definition.icon(), AtlasNodeText.anomalyBody(anomalyId)));
        }
        return entries;
    }

    static Objective objective(ChartRun run) {
        if (!run.hasBoss()) {
            return null;
        }
        String progress = ChartObjectiveIntel.progressText(run);
        String status = progress != null ? progress : Translation.gameText("chart.intel.reward");
        return new Objective(ChartObjectiveIntel.objectiveName(run),
                StyledText.of(Translation.gameText(ChartObjectiveIntel.descriptionKey(run)) + "\n" + status));
    }

    static List<SlottedLine> modifierLines(StarChart chart) {
        if (chart == null) {
            return List.of();
        }
        List<SlottedLine> lines = new ArrayList<>();
        chart.implicitLines(false).forEach(line -> lines.add(new SlottedLine(null, line)));
        lines.addAll(chart.slottedEffectLines(false, null));
        return lines;
    }
}
