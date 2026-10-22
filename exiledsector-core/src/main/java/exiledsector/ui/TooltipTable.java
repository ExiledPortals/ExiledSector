package exiledsector.ui;

import java.util.List;

public record TooltipTable(String heading, List<String> headers, List<Row> rows) {

    public record Row(List<String> cells, boolean highlighted) {

        public static Row of(boolean highlighted, String... cells) {
            return new Row(List.of(cells), highlighted);
        }
    }
}
