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
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerText;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.ArrayList;
import java.util.List;

public final class AtlasNodeText {

    private AtlasNodeText() {
    }

    public static String regionName(String regionId) {
        return AtlasNames.region(regionId);
    }

    public record SystemCard(String title, String layout, String factionId, AtlasOfficer officer, boolean commandCentre,
                             List<AtlasLine> notes, List<AtlasLine> status) {
    }

    public static String title(AtlasGenerator generator, AtlasCell cell) {
        AtlasNode node = generator.node(cell);
        return node.isOrigin() ? Translation.text("chart.atlas.node.gateway") : regionName(node.regionId());
    }

    public static SystemCard card(AtlasGenerator generator, AtlasState state, AtlasCell cell, boolean reachable) {
        AtlasNode node = generator.node(cell);
        if (node.isOrigin()) {
            return new SystemCard(title(generator, cell), "", null, null, false, List.of(), List.of());
        }
        AtlasCell centre = state.commandCentreOf(cell);
        AtlasOfficer officer = centre == null ? null : state.officerAt(centre);
        List<AtlasLine> notes = new ArrayList<>(notes(generator, state, cell, node));
        if (officer == null) {
            notes.addAll(fledOfficerLines(generator, state, cell));
        }
        return new SystemCard(title(generator, cell), AtlasNames.name("layout", node.layoutId()), AtlasOpening.factionFor(generator, state, cell, node),
                officer, officer != null && centre.equals(cell), notes, stateLines(generator, state, cell, reachable));
    }

    public static StyledText officerBody(AtlasOfficer officer) {
        List<StyledText> lines = new ArrayList<>(OfficerBoardTooltip.detailLines(officer, OfficerText.faction(officer)));
        OfficerText.detailLines(officer).forEach(line -> lines.add(line.styled()));
        return StyledText.join(StyledText.of("\n"), lines);
    }

    static List<AtlasLine> fledOfficerLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        List<AtlasLine> lines = new ArrayList<>();
        for (AtlasOfficer fled : state.officers()) {
            if (OfficerText.fledFrom(fled, cell)) {
                lines.add(new AtlasLine(OfficerText.fledLine(generator, fled), Tone.HINT));
            }
        }
        return lines;
    }

    static List<AtlasLine> markers(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasNode node) {
        List<AtlasLine> lines = new ArrayList<>(notes(generator, state, cell, node));
        List<AtlasLine> anomalies = new ArrayList<>();
        for (String anomalyId : AtlasAnomalies.effective(generator, state, cell)) {
            anomalies.add(new AtlasLine(Translation.msg("chart.atlas.node.anomaly").arg("anomaly", AtlasNames.name("anomaly", anomalyId)).text(), Tone.MARKER));
            anomalies.addAll(anomalyLines(generator, state, cell, anomalyId));
        }
        lines.addAll(generator.specialAt(cell) == null ? 0 : 1, anomalies);
        return lines;
    }

    static List<AtlasLine> notes(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasNode node) {
        List<AtlasLine> lines = new ArrayList<>();
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null) {
            lines.add(new AtlasLine(AtlasNames.name("special", special.id()), Tone.MARKER));
        }
        if (node.infestationId() != null && !state.has(cell, AtlasState.INFESTATION_CLEARED)) {
            lines.add(new AtlasLine(Translation.msg("chart.atlas.node.infested").arg("infestation", AtlasNames.name("special", node.infestationId())).text(), Tone.BAD));
        }
        if (state.has(cell, AtlasState.ANOMALIES_SPENT)) {
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
        String rewardKey = "chart.atlas.anomaly." + anomalyId + ".reward";
        if (Translation.has(rewardKey)) {
            lines.add(Translation.styled(rewardKey));
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
            return List.of();
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
