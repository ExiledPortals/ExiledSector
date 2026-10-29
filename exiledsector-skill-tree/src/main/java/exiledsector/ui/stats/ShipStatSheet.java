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

    public record Breakdown(String title, String summary, List<SourceLine> sources, List<String> notes, Set<String> nodeIds) {

        public boolean isEmpty() {
            return summary == null && sources.isEmpty() && notes.isEmpty();
        }
    }

    public record SourceLine(String source, String amount, boolean fromTree) {
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }
}
