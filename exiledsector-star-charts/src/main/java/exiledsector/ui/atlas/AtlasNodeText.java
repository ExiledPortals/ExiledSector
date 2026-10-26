package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.charts.ChartAnomalies;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasNode;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasReach;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasSpecial;
import exiledsector.charts.atlas.AtlasState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AtlasNodeText {

    public record Line(String text, Tone tone) {
    }

    public enum Tone { TITLE, PLAIN, MARKER, GOOD, BAD, HINT }

    private AtlasNodeText() {
    }

    public static String name(String category, String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        String key = "chart.atlas." + category + "." + id;
        return Translation.has(key) ? Translation.text(key) : tidy(id);
    }

    static String tidy(String id) {
        String[] words = id.replace('-', '_').split("_");
        StringBuilder tidied = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (tidied.length() > 0) {
                tidied.append(' ');
            }
            tidied.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return tidied.toString();
    }

    public static String regionName(String regionId) {
        return AtlasRegion.NORMAL.equals(regionId) ? Translation.text("chart.atlas.normalSpace") : name("region", regionId);
    }

    public static String factionName(String factionId) {
        SectorAPI sector = Global.getSector();
        FactionAPI faction = sector == null || factionId == null ? null : sector.getFaction(factionId);
        return faction == null ? tidy(factionId == null ? "" : factionId) : faction.getDisplayName();
    }

    public static List<Line> describe(AtlasGenerator generator, AtlasState state, AtlasCell cell, boolean reachable) {
        AtlasNode node = generator.node(cell);
        List<Line> lines = new ArrayList<>();
        if (node.isOrigin()) {
            lines.add(new Line(Translation.text("chart.atlas.node.gateway"), Tone.TITLE));
            return lines;
        }
        lines.add(new Line(regionName(node.regionId()), Tone.TITLE));
        lines.add(new Line(Translation.msg("chart.atlas.node.faction").arg("faction", factionName(AtlasOpening.factionFor(generator, state, cell, node))).text(),
                Tone.PLAIN));
        lines.add(new Line(Translation.msg("chart.atlas.node.layout").arg("layout", name("layout", node.layoutId())).text(), Tone.PLAIN));
        lines.addAll(markers(generator, state, cell, node));
        lines.addAll(stateLines(generator, state, cell, reachable));
        return lines;
    }

    static List<Line> markers(AtlasGenerator generator, AtlasState state, AtlasCell cell, AtlasNode node) {
        List<Line> lines = new ArrayList<>();
        boolean spent = state.has(cell, AtlasState.MARKERS_SPENT);
        AtlasSpecial special = generator.specialAt(cell);
        if (special != null) {
            lines.add(new Line(name("special", special.id()), Tone.MARKER));
        }
        if (node.powerfulBoss() && !spent) {
            lines.add(new Line(Translation.text("chart.atlas.node.powerfulBoss"), Tone.MARKER));
        }
        for (String anomalyId : AtlasAnomalies.effective(generator, state, cell)) {
            lines.add(new Line(Translation.msg("chart.atlas.node.anomaly").arg("anomaly", name("anomaly", anomalyId)).text(), Tone.MARKER));
        }
        if (node.infestationId() != null) {
            lines.add(state.has(cell, AtlasState.CLEANSED)
                    ? new Line(Translation.text("chart.atlas.node.cleansed"), Tone.GOOD)
                    : new Line(Translation.msg("chart.atlas.node.infested").arg("infestation", name("special", node.infestationId())).text(), Tone.BAD));
        }
        if (spent) {
            lines.add(new Line(Translation.text("chart.atlas.node.spent"), Tone.BAD));
        }
        return lines;
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

    static List<Line> stateLines(AtlasGenerator generator, AtlasState state, AtlasCell cell, boolean reachable) {
        if (state.isCompleted(cell)) {
            return List.of(new Line(Translation.text("chart.atlas.node.completed"), Tone.GOOD));
        }
        if (AtlasReach.isSpentBastion(generator, state, cell)) {
            return List.of(new Line(Translation.text("chart.atlas.node.attempted"), Tone.BAD));
        }
        if (!reachable) {
            return List.of(new Line(Translation.text("chart.atlas.blocked.not_reachable"), Tone.HINT));
        }
        AtlasOpening.Blocker requirement = AtlasOpening.check(generator, state, cell, null);
        Line open = new Line(Translation.text("chart.atlas.node.open"), Tone.GOOD);
        return requirement.allowsOpening() ? List.of(open) : List.of(new Line(Translation.text(requirement.reasonKey()), Tone.MARKER), open);
    }

    public static List<String> confirmLines(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        AtlasNode node = generator.node(cell);
        List<String> lines = new ArrayList<>();
        lines.add(Translation.msg("chart.atlas.confirm.system").arg("region", regionName(node.regionId()))
                .arg("faction", factionName(AtlasOpening.factionFor(generator, state, cell, node))).text());
        for (Line marker : markers(generator, state, cell, node)) {
            lines.add(marker.text());
        }
        return lines;
    }
}
