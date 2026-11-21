package exiledsector.ui.atlas;

import exiledsector.ui.SearchQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class AtlasTreeSearch implements SearchQuery {

    static final float DIM_ALPHA = 0.2f;
    private static final String TERM_SEPARATOR = "\n";

    record Entry(String nodeId, Collection<String> anomalyTags, Collection<String> terms) {
    }

    private Map<String, String> termsByNodeId = Map.of();
    private Map<String, Set<String>> anomaliesByNodeId = Map.of();
    private String query = "";
    private String needle = "";
    private Set<String> focusedAnomalies = Set.of();
    private Set<String> matchedNodeIds = Set.of();
    private List<String> matchOrder = List.of();
    private int nextMatchIndex;

    void index(Collection<Entry> entries) {
        Map<String, String> terms = new LinkedHashMap<>();
        Map<String, Set<String>> anomalies = new LinkedHashMap<>();
        for (Entry entry : entries) {
            terms.put(entry.nodeId(), lowered(String.join(TERM_SEPARATOR, entry.terms())));
            anomalies.put(entry.nodeId(), Set.copyOf(entry.anomalyTags()));
        }
        termsByNodeId = terms;
        anomaliesByNodeId = anomalies;
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

    void focusAnomalies(Set<String> anomalyIds) {
        Set<String> focus = anomalyIds == null ? Set.of() : Set.copyOf(anomalyIds);
        if (!focus.equals(focusedAnomalies)) {
            focusedAnomalies = focus;
            refreshMatches();
        }
    }

    boolean isActive() {
        return !focusedAnomalies.isEmpty() || !needle.isEmpty();
    }

    boolean matches(String nodeId) {
        return matchedNodeIds.contains(nodeId);
    }

    float nodeAlpha(String nodeId) {
        return !isActive() || matches(nodeId) ? 1f : DIM_ALPHA;
    }

    float backgroundAlpha() {
        return isActive() ? DIM_ALPHA : 1f;
    }

    String nextMatch() {
        if (matchOrder.isEmpty()) {
            return null;
        }
        int index = nextMatchIndex % matchOrder.size();
        nextMatchIndex = index + 1;
        return matchOrder.get(index);
    }

    private void refreshMatches() {
        nextMatchIndex = 0;
        List<String> found = new ArrayList<>();
        if (!focusedAnomalies.isEmpty()) {
            anomaliesByNodeId.forEach((nodeId, anomalies) -> {
                if (anomalies.stream().anyMatch(focusedAnomalies::contains)) {
                    found.add(nodeId);
                }
            });
        } else if (!needle.isEmpty()) {
            termsByNodeId.forEach((nodeId, terms) -> {
                if (terms.contains(needle)) {
                    found.add(nodeId);
                }
            });
        }
        matchOrder = List.copyOf(found);
        matchedNodeIds = new LinkedHashSet<>(found);
    }

    private static String lowered(String text) {
        return text.toLowerCase(Locale.ROOT);
    }
}
