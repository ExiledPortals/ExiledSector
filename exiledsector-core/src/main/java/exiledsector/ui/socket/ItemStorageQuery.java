package exiledsector.ui.socket;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

final class ItemStorageQuery {

    private record Match(ItemStorageRow row, OptionalDouble value) {
    }

    private static final Comparator<Match> NEWEST_FIRST = Comparator.comparingInt((Match match) -> match.row().storeOrder()).reversed();

    private ItemStorageQuery() {
    }

    static List<ItemStorageRow> apply(List<ItemStorageRow> rows, ItemStorageFilter filter) {
        ItemStorageSearch search = ItemStorageSearch.parse(filter.query);
        return rows.stream()
                .filter(row -> matchesStatus(row, filter.status))
                .filter(row -> filter.rarities.isEmpty() || filter.rarities.contains(row.rarity()))
                .filter(row -> search.matches(row.searchLines()))
                .map(row -> new Match(row, search.matchedValue(row.searchLines())))
                .sorted(order(filter.sort, filter.sortReversed))
                .map(Match::row)
                .toList();
    }

    private static Comparator<Match> order(ItemStorageFilter.Sort sort, boolean reversed) {
        return switch (sort) {
            case AGE -> reversed ? NEWEST_FIRST.reversed() : NEWEST_FIRST;
            case RARITY -> {
                Comparator<Match> rarestFirst = Comparator.comparingInt((Match match) -> match.row().rarity().ordinal()).reversed();
                yield (reversed ? rarestFirst.reversed() : rarestFirst).thenComparing(NEWEST_FIRST);
            }
            case VALUE -> Comparator.comparingDouble((Match match) -> valueOrder(match, reversed)).thenComparing(NEWEST_FIRST);
        };
    }

    private static double valueOrder(Match match, boolean lowestFirst) {
        if (match.value().isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double value = match.value().getAsDouble();
        return lowestFirst ? value : -value;
    }

    private static boolean matchesStatus(ItemStorageRow row, ItemStorageFilter.Status status) {
        return switch (status) {
            case ALL -> true;
            case FREE -> !row.installed();
            case INSTALLED -> row.installed();
        };
    }
}
