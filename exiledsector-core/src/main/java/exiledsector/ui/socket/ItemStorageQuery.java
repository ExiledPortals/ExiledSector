package exiledsector.ui.socket;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class ItemStorageQuery {

    private ItemStorageQuery() {
    }

    static List<ItemStorageRow> apply(List<ItemStorageRow> rows, ItemStorageFilter filter) {
        String[] words = filter.query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        return rows.stream()
                .filter(row -> matchesStatus(row, filter.status))
                .filter(row -> filter.rarities.isEmpty() || filter.rarities.contains(row.rarity()))
                .filter(row -> matchesWords(row, words))
                .sorted(Comparator.comparingInt(ItemStorageRow::storeOrder).reversed())
                .toList();
    }

    private static boolean matchesStatus(ItemStorageRow row, ItemStorageFilter.Status status) {
        return switch (status) {
            case ALL -> true;
            case FREE -> !row.installed();
            case INSTALLED -> row.installed();
        };
    }

    private static boolean matchesWords(ItemStorageRow row, String[] words) {
        for (String word : words) {
            if (!word.isEmpty() && !row.searchText().contains(word)) {
                return false;
            }
        }
        return true;
    }
}
