package exiledsector.ui.socket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface BenchTray {

    record Column(String headingKey, List<BenchTrayEntry> entries) {

        public Column {
            entries = List.copyOf(entries);
        }

        public static List<Column> dealt(List<BenchTrayEntry> entries, int columnCount) {
            List<List<BenchTrayEntry>> dealtEntries = new ArrayList<>();
            for (int column = 0; column < columnCount; column++) {
                dealtEntries.add(new ArrayList<>());
            }
            for (int index = 0; index < entries.size(); index++) {
                dealtEntries.get(index % columnCount).add(entries.get(index));
            }
            return dealtEntries.stream().map(columnEntries -> new Column(null, columnEntries)).toList();
        }
    }

    String titleKey();

    String emptyKey();

    List<String> iconPaths();

    List<BenchTrayEntry> entries(BenchTrayContext context);

    default int iconColumns() {
        return 0;
    }

    default List<Column> columns(BenchTrayContext context, Predicate<BenchTrayEntry> shown) {
        return List.of(new Column(null, entries(context).stream().filter(shown).toList()));
    }
}
