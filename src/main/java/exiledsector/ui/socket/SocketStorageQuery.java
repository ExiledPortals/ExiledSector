package exiledsector.ui.socket;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class SocketStorageQuery {

    private SocketStorageQuery() {
    }

    static List<SocketStorageRow> apply(List<SocketStorageRow> rows, SocketStorageFilter filter) {
        String[] words = filter.query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        return rows.stream()
                .filter(row -> matchesStatus(row, filter.status))
                .filter(row -> filter.rarities.isEmpty() || filter.rarities.contains(row.rarity()))
                .filter(row -> filter.grades.isEmpty() || filter.grades.contains(row.grade()))
                .filter(row -> matchesWords(row, words))
                .sorted(Comparator.comparingInt(SocketStorageRow::order).reversed())
                .toList();
    }

    private static boolean matchesStatus(SocketStorageRow row, SocketStorageFilter.Status status) {
        return switch (status) {
            case ALL -> true;
            case FREE -> !row.installed();
            case INSTALLED -> row.installed();
        };
    }

    private static boolean matchesWords(SocketStorageRow row, String[] words) {
        for (String word : words) {
            if (!word.isEmpty() && !row.searchText().contains(word)) {
                return false;
            }
        }
        return true;
    }
}
