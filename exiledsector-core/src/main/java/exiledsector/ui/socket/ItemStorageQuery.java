package exiledsector.ui.socket;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

final class ItemStorageQuery {

    private record Match(ItemStorageRow row, List<OptionalDouble> termValues) {
    }

    private static final Comparator<Match> NEWEST_FIRST = Comparator.comparingInt((Match match) -> match.row().storeOrder()).reversed();
    private static final Comparator<Match> RAREST_FIRST = Comparator.comparingInt((Match match) -> match.row().rarity().ordinal()).reversed();

    private ItemStorageQuery() {
    }

    static List<ItemStorageRow> apply(List<ItemStorageRow> rows, ItemStorageFilter filter) {
        ItemStorageSearch search = ItemStorageSearch.parse(filter.query);
        List<Match> matches = rows.stream()
                .filter(row -> matchesStatus(row, filter))
                .filter(row -> filter.rarities.isEmpty() || filter.rarities.contains(row.rarity()))
                .filter(row -> search.matches(row.searchLines()))
                .map(row -> new Match(row, search.termValues(row.searchLines())))
                .toList();
        double[] termPeaks = termPeaks(matches, search.termCount());
        return matches.stream()
                .sorted(order(filter.sort, termPeaks))
                .map(Match::row)
                .toList();
    }

    private static Comparator<Match> order(ItemStorageFilter.Sort sort, double[] termPeaks) {
        return switch (sort) {
            case NEWEST -> NEWEST_FIRST;
            case OLDEST -> NEWEST_FIRST.reversed();
            case RAREST -> RAREST_FIRST.thenComparing(NEWEST_FIRST);
            case MOST_COMMON -> RAREST_FIRST.reversed().thenComparing(NEWEST_FIRST);
            case HIGHEST_VALUE -> Comparator.comparingDouble((Match match) -> valueOrder(match, termPeaks, false)).thenComparing(NEWEST_FIRST);
            case LOWEST_VALUE -> Comparator.comparingDouble((Match match) -> valueOrder(match, termPeaks, true)).thenComparing(NEWEST_FIRST);
        };
    }

    private static double[] termPeaks(List<Match> matches, int termCount) {
        double[] peaks = new double[termCount];
        for (Match match : matches) {
            for (int term = 0; term < termCount; term++) {
                OptionalDouble value = match.termValues().get(term);
                if (value.isPresent()) {
                    peaks[term] = Math.max(peaks[term], Math.abs(value.getAsDouble()));
                }
            }
        }
        return peaks;
    }

    private static double valueOrder(Match match, double[] termPeaks, boolean lowestFirst) {
        OptionalDouble score = combinedScore(match.termValues(), termPeaks);
        if (score.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        return lowestFirst ? score.getAsDouble() : -score.getAsDouble();
    }

    private static OptionalDouble combinedScore(List<OptionalDouble> termValues, double[] termPeaks) {
        boolean anyValue = false;
        double score = 0d;
        for (int term = 0; term < termValues.size(); term++) {
            OptionalDouble value = termValues.get(term);
            if (value.isPresent()) {
                anyValue = true;
                score += termPeaks[term] > 0d ? value.getAsDouble() / termPeaks[term] : 0d;
            }
        }
        return anyValue ? OptionalDouble.of(score) : OptionalDouble.empty();
    }

    private static boolean matchesStatus(ItemStorageRow row, ItemStorageFilter filter) {
        ItemStorageFilter.Status status = row.installed() ? ItemStorageFilter.Status.INSTALLED : ItemStorageFilter.Status.FREE;
        return filter.statuses.isEmpty() || filter.statuses.contains(status);
    }
}
