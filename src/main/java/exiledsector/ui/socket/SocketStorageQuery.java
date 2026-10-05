package exiledsector.ui.socket;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class SocketStorageQuery {

    private SocketStorageQuery() {
    }

    static List<SocketStorageRow> apply(List<SocketStorageRow> rows, SocketStorageFilter filter) {
        String[] words = filter.query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        Comparator<SocketStorageRow> order = comparator(filter.sort);
        if (filter.descending) {
            order = order.reversed();
        }
        return rows.stream()
                .filter(row -> matchesStatus(row, filter.status))
                .filter(row -> matchesWords(row, words))
                .sorted(order.thenComparingInt(SocketStorageRow::order))
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

    private static Comparator<SocketStorageRow> comparator(SocketStorageFilter.Sort sort) {
        return switch (sort) {
            case ACQUIRED -> Comparator.comparingInt(SocketStorageRow::order);
            case NAME -> Comparator.comparing(row -> row.name().toLowerCase(Locale.ROOT));
            case KIND -> Comparator.comparing(SocketStorageRow::kind);
            case GRADE -> Comparator.comparing(SocketStorageRow::grade);
            case ALIGNMENT -> Comparator.comparing(SocketStorageRow::alignment);
            case EFFECT_COUNT -> Comparator.comparingInt(SocketStorageRow::effectCount);
        };
    }
}
