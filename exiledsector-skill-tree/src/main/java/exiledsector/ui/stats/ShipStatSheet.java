package exiledsector.ui.stats;

import exiledsector.skills.DescriptionLine;

import java.util.List;
import java.util.Set;

public record ShipStatSheet(List<Section> sections) {

    public enum Trend {
        NEUTRAL,
        BETTER,
        WORSE,
        UNCHANGED
    }

    public enum Detail {
        CHANGED,
        STANDARD,
        FULL
    }

    public record Section(StatGroup group, List<ValueRow> values, List<Grid> grids, List<EffectRow> effects) {

        public boolean isEmpty() {
            return values.isEmpty() && grids.isEmpty() && effects.isEmpty();
        }
    }

    public record ValueRow(String label, String value, String change, Trend trend, String marker, Breakdown breakdown) {
    }

    public record Grid(List<String> columns, List<GridRow> rows) {
    }

    public record GridRow(String label, List<Cell> cells) {
    }

    public record Cell(String text, Trend trend, String marker, Breakdown breakdown) {
    }

    public record EffectRow(DescriptionLine line, String marker, Breakdown breakdown) {
    }

    public enum Combine {
        INCREASED,
        ADDED,
        MORE,
        OTHER
    }

    public record Breakdown(String title, String summary, List<SourceGroup> groups, String result, List<String> notes, Set<String> nodeIds) {

        public boolean isEmpty() {
            return summary == null && groups.isEmpty() && notes.isEmpty();
        }

        public List<SourceLine> sources() {
            return groups.stream().flatMap(group -> group.lines().stream()).toList();
        }
    }

    public record SourceGroup(Combine combine, List<SourceLine> lines, String total) {
    }

    public record SourceLine(String source, String amount, boolean fromTree, List<SourceLine> parts) {

        public SourceLine(String source, String amount, boolean fromTree) {
            this(source, amount, fromTree, List.of());
        }
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }
}
