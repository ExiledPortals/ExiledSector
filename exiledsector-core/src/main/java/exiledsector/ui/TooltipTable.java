package exiledsector.ui;

import java.util.List;

public record TooltipTable(String heading, List<String> headers, List<Row> rows) {

    public enum Tone {
        PLAIN,
        HIGHLIGHTED,
        TOTAL,
        DETAIL
    }

    public record Row(List<String> cells, Tone tone) {

        public static Row of(boolean highlighted, String... cells) {
            return of(highlighted ? Tone.HIGHLIGHTED : Tone.PLAIN, cells);
        }

        public static Row of(Tone tone, String... cells) {
            return new Row(List.of(cells), tone);
        }

        public boolean highlighted() {
            return tone == Tone.HIGHLIGHTED;
        }
    }

    int columnCount() {
        if (!headers.isEmpty()) {
            return headers.size();
        }
        return rows.isEmpty() ? 0 : rows.get(0).cells().size();
    }
}
