package exiledsector.ui.atlas;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AtlasRule;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasLine.Tone;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.ArrayList;
import java.util.List;

public final class AtlasNodeText {

    private AtlasNodeText() {
    }

    public static String regionName(String regionId) {
        return AtlasRegion.NORMAL.equals(regionId) ? Translation.text("chart.atlas.normalSpace") : AtlasNames.name("region", regionId);
    }

    public static List<AtlasLine> describe(AtlasGenerator generator, AtlasState state, AtlasCell cell, boolean reachable) {
        AtlasNode node = generator.node(cell);
        List<AtlasLine> lines = new ArrayList<>();
        if (node.isOrigin()) {
            lines.add(new AtlasLine(Translation.text("chart.atlas.node.gateway"), Tone.TITLE));
            return lines;
        }
        lines.add(new AtlasLine(regionName(node.regionId()), Tone.TITLE));
        lines.add(new AtlasLine(Translation.msg("chart.atlas.node.faction")
                .arg("faction", ChartFactions.displayName(AtlasOpening.factionFor(generator, state, cell, node))).text(), Tone.PLAIN));
        lines.add(new AtlasLine(Translation.msg("chart.atlas.node.layout").arg("layout", AtlasNames.name("layout", node.layoutId())).text(), Tone.PLAIN));
        lines.addAll(markers(generator, state, cell, node));
        lines.addAll(stateLines(generator, state, cell, reachable));
        return lines;
    }

    static List<AtlasLine> markers(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasNode node) {
        List<AtlasLine> lines = new ArrayList<>();
        boolean spent = state.has(cell, AtlasState.MARKERS_SPENT);
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null) {
            lines.add(new AtlasLine(AtlasNames.name("special", special.id()), Tone.MARKER));
        }
        for (String anomalyId : AtlasAnomalies.effective(generator, state, cell)) {
            lines.add(new AtlasLine(Translation.msg("chart.atlas.node.anomaly").arg("anomaly", AtlasNames.name("anomaly", anomalyId)).text(), Tone.MARKER));
            lines.addAll(anomalyLines(generator, state, cell, anomalyId));
        }
        if (node.infestationId() != null) {
            lines.add(state.has(cell, AtlasState.CLEANSED)
                    ? new AtlasLine(Translation.text("chart.atlas.node.cleansed"), Tone.GOOD)
                    : new AtlasLine(Translation.msg("chart.atlas.node.infested").arg("infestation", AtlasNames.name("special", node.infestationId())).text(), Tone.BAD));
        }
        if (spent) {
            lines.add(new AtlasLine(Translation.text("chart.atlas.node.spent"), Tone.BAD));
        }
        return lines;
    }

    private static List<AtlasLine> anomalyLines(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId) {
        AtlasRule rule = ChartAnomalies.role(anomalyId, AtlasRule.class);
        return rule == null ? List.of() : rule.atlasLines(generator, state, cell);
    }

    public static StyledText anomalyBody(AtlasGenerator generator, AtlasState state, AtlasCell cell, String anomalyId) {
        List<StyledText> lines = new ArrayList<>(List.of(anomalyBody(anomalyId)));
        anomalyLines(generator, state, cell, anomalyId).forEach(line -> lines.add(line.styled()));
        return StyledText.join(StyledText.of("\n"), lines);
    }

    public static StyledText anomalyBody(String anomalyId) {
        List<StyledText> lines = new ArrayList<>();
        String descriptionKey = "chart.atlas.anomaly." + anomalyId + ".description";
        if (Translation.has(descriptionKey)) {
            lines.add(StyledText.of(Translation.text(descriptionKey)));
        }
        ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
        if (definition != null) {
            for (ChartGenContext.AppliedEffect applied : definition.effects()) {
                lines.add(applied.effect().description(applied.value()));
            }
        }
        return StyledText.join(StyledText.of("\n"), lines);
    }

    static List<AtlasLine> stateLines(AtlasGenerator generator, AtlasState state, AtlasCell cell, boolean reachable) {
        if (state.isCompleted(cell)) {
            return List.of(new AtlasLine(Translation.text("chart.atlas.node.completed"), Tone.GOOD));
        }
        if (AtlasReach.isSpentBastion(generator, state, cell)) {
            return List.of(new AtlasLine(Translation.text("chart.atlas.node.attempted"), Tone.BAD));
        }
        if (!reachable) {
            return List.of(new AtlasLine(Translation.text("chart.atlas.blocked.not_reachable"), Tone.HINT));
        }
        AtlasOpening.Blocker requirement = AtlasOpening.check(generator, state, cell, null);
        AtlasLine open = new AtlasLine(Translation.text("chart.atlas.node.open"), Tone.GOOD);
        return requirement.allowsOpening() ? List.of(open) : List.of(new AtlasLine(Translation.text(requirement.reasonKey()), Tone.MARKER), open);
    }

    public static List<String> confirmLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasNode node = generator.node(cell);
        List<String> lines = new ArrayList<>();
        lines.add(Translation.msg("chart.atlas.confirm.system").arg("region", regionName(node.regionId()))
                .arg("faction", ChartFactions.displayName(AtlasOpening.factionFor(generator, state, cell, node))).text());
        for (AtlasLine marker : markers(generator, state, cell, node)) {
            lines.add(marker.plain());
        }
        for (AtlasRule rule : atlasRulesAt(generator, state, cell)) {
            lines.addAll(rule.confirmLines(generator, state, cell));
        }
        return lines;
    }

    public static List<BenchModeSupport.OpenChoice> entryChoices(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        List<BenchModeSupport.OpenChoice> choices = new ArrayList<>();
        for (AtlasRule rule : atlasRulesAt(generator, state, cell)) {
            choices.addAll(rule.entryChoices(generator, state, cell));
        }
        return choices;
    }

    private static List<AtlasRule> atlasRulesAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return ChartAnomalies.among(AtlasAnomalies.effective(generator, state, cell), AtlasRule.class);
    }
}
