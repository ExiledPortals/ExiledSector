package exiledsector.ui.socket;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class SocketStorageQuery {

    static final int PAGE_SIZE = 40;

    private SocketStorageQuery() {
    }

    static List<SocketStorageRow> apply(List<SocketStorageRow> rows, SocketStorageFilter filter) {
        String[] words = filter.query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        Comparator<SocketStorageRow> order = comparator(filter.sort);
        if (filter.descending) {
            order = order.reversed();
        }
        return rows.stream()
                .filter(row -> !filter.hiddenKinds.contains(row.kind()))
                .filter(row -> !filter.hiddenGrades.contains(row.grade()))
                .filter(row -> !filter.hiddenAlignments.contains(row.alignment()))
                .filter(row -> filter.themes.isEmpty() || !Collections.disjoint(row.themes(), filter.themes))
                .filter(row -> matchesStatus(row, filter.status))
                .filter(row -> matchesWords(row, words))
                .sorted(order.thenComparingInt(SocketStorageRow::order))
                .toList();
    }

    static int pageCount(int rowCount) {
        return Math.max(1, (rowCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    static int clampPage(int page, int rowCount) {
        return Math.max(0, Math.min(page, pageCount(rowCount) - 1));
    }

    static List<SocketStorageRow> page(List<SocketStorageRow> rows, int page) {
        int first = clampPage(page, rows.size()) * PAGE_SIZE;
        return rows.subList(first, Math.min(rows.size(), first + PAGE_SIZE));
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
