package exiledsector.ui.socket;

import java.util.Comparator;
import java.util.List;

final class ItemStorageQuery {

    private static final Comparator<ItemStorageRow> NEWEST_FIRST = Comparator.comparingInt(ItemStorageRow::storeOrder).reversed();
    private static final Comparator<ItemStorageRow> RAREST_FIRST = Comparator.comparingInt((ItemStorageRow row) -> row.rarity().ordinal()).reversed();

    private ItemStorageQuery() {
    }

    static List<ItemStorageRow> apply(List<ItemStorageRow> rows, ItemStorageFilter filter) {
        return apply(rows, filter, true);
    }

    static List<ItemStorageRow> apply(List<ItemStorageRow> rows, ItemStorageFilter filter, boolean withModifierSearch) {
        ItemStorageSearch search = ItemStorageSearch.parse(filter.query);
        return rows.stream()
                .filter(row -> matchesStatus(row, filter))
                .filter(row -> filter.rarities.isEmpty() || filter.rarities.contains(row.rarity()))
                .filter(row -> search.matches(row.searchLines()))
                .filter(row -> !withModifierSearch || filter.modifierSearch.matches(row.statTotals()))
                .sorted(order(filter.sort))
                .toList();
    }

    private static Comparator<ItemStorageRow> order(ItemStorageFilter.Sort sort) {
        return switch (sort) {
            case NEWEST -> NEWEST_FIRST;
            case OLDEST -> NEWEST_FIRST.reversed();
            case RAREST -> RAREST_FIRST.thenComparing(NEWEST_FIRST);
        };
    }

    private static boolean matchesStatus(ItemStorageRow row, ItemStorageFilter filter) {
        ItemStorageFilter.Status status = row.installed() ? ItemStorageFilter.Status.INSTALLED : ItemStorageFilter.Status.FREE;
        return filter.statuses.isEmpty() || filter.statuses.contains(status);
    }
}
