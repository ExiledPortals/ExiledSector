package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.ui.SearchQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class AtlasSearch implements SearchQuery {

    static final float DIM_ALPHA = 0.2f;
    private static final String TERM_SEPARATOR = "\n";

    private Map<AtlasCell, String> termsByCell = Map.of();
    private String query = "";
    private String needle = "";
    private Set<AtlasCell> matchedCells = Set.of();
    private List<AtlasCell> matchOrder = List.of();
    private int nextMatchIndex;

    void index(Map<AtlasCell, List<String>> searchTerms) {
        Map<AtlasCell, String> indexed = new LinkedHashMap<>();
        searchTerms.forEach((cell, terms) -> indexed.put(cell, lowered(String.join(TERM_SEPARATOR, terms))));
        termsByCell = indexed;
        refreshMatches();
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public void setQuery(String query) {
        String entered = query == null ? "" : query;
        if (!entered.equals(this.query)) {
            this.query = entered;
            needle = lowered(entered.strip());
            refreshMatches();
        }
    }

    boolean isActive() {
        return !needle.isEmpty();
    }

    boolean matches(AtlasCell cell) {
        return matchedCells.contains(cell);
    }

    float alpha(AtlasCell cell) {
        return !isActive() || matches(cell) ? 1f : DIM_ALPHA;
    }

    float laneAlpha(AtlasCell from, AtlasCell to) {
        return !isActive() || (matches(from) && matches(to)) ? 1f : DIM_ALPHA;
    }

    float backgroundAlpha() {
        return isActive() ? DIM_ALPHA : 1f;
    }

    AtlasCell nextMatch() {
        if (matchOrder.isEmpty()) {
            return null;
        }
        int index = nextMatchIndex % matchOrder.size();
        nextMatchIndex = index + 1;
        return matchOrder.get(index);
    }

    private void refreshMatches() {
        nextMatchIndex = 0;
        if (needle.isEmpty()) {
            matchedCells = Set.of();
            matchOrder = List.of();
            return;
        }
        List<AtlasCell> found = new ArrayList<>();
        termsByCell.forEach((cell, terms) -> {
            if (terms.contains(needle)) {
                found.add(cell);
            }
        });
        matchOrder = List.copyOf(found);
        matchedCells = new LinkedHashSet<>(found);
    }

    private static String lowered(String text) {
        return text.toLowerCase(Locale.ROOT);
    }
}
