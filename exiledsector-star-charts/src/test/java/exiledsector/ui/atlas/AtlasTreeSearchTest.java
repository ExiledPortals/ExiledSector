package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasTreeNode;
import exiledsector.charts.tree.ShippedAtlasTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeSearchTest {

    private static final float DELTA = 0.001f;

    private static AtlasTreeSearch searchOver() {
        AtlasTreeSearch search = new AtlasTreeSearch();
        search.index(List.of(
                new AtlasTreeSearch.Entry("breach_1", List.of("spatial_breach"), List.of("Breach Chance", "More Spatial Breaches")),
                new AtlasTreeSearch.Entry("debris_1", List.of("anomalous_debris"), List.of("Debris Chance", "Salvage from wrecks")),
                new AtlasTreeSearch.Entry("hybrid_1", List.of("spatial_breach", "anomalous_debris"), List.of("Rude Awakening")),
                new AtlasTreeSearch.Entry("travel_1", List.of(), List.of("Boss Star Charts", "loot"))));
        return search;
    }

    @Test
    void nothingIsDimmedUntilTheSearchHasSomethingToLookFor() {
        AtlasTreeSearch search = searchOver();

        assertFalse(search.isActive());
        assertEquals(1f, search.nodeAlpha("breach_1"), DELTA);
        assertEquals(1f, search.backgroundAlpha(), DELTA);
        assertNull(search.nextMatch());
    }

    @Test
    void aQueryMatchesTitlesDescriptionsAndTagsIgnoringCase() {
        AtlasTreeSearch search = searchOver();

        search.setQuery("SALVAGE");
        assertTrue(search.matches("debris_1"));
        assertFalse(search.matches("breach_1"));
        assertEquals(AtlasTreeSearch.DIM_ALPHA, search.nodeAlpha("breach_1"), DELTA);

        search.setQuery("loot");
        assertTrue(search.matches("travel_1"));
        assertEquals(AtlasTreeSearch.DIM_ALPHA, search.backgroundAlpha(), DELTA);
    }

    @Test
    void submittingCyclesThroughTheMatches() {
        AtlasTreeSearch search = searchOver();
        search.setQuery("chance");

        assertEquals("breach_1", search.nextMatch());
        assertEquals("debris_1", search.nextMatch());
        assertEquals("breach_1", search.nextMatch());
    }

    @Test
    void aHoveredCatalystHighlightsItsAnomalysNodesOverTheQueryUntilItLeaves() {
        AtlasTreeSearch search = searchOver();
        search.setQuery("boss");

        search.focusAnomalies(Set.of("spatial_breach"));
        assertTrue(search.matches("breach_1"));
        assertTrue(search.matches("hybrid_1"));
        assertFalse(search.matches("debris_1"));
        assertFalse(search.matches("travel_1"));

        search.focusAnomalies(Set.of());
        assertTrue(search.matches("travel_1"));
        assertFalse(search.matches("breach_1"));
    }

    @Test
    void aCrossPollinatedNodeLightsUpForEitherOfItsCatalysts() {
        AtlasTreeSearch search = searchOver();

        search.focusAnomalies(Set.of("anomalous_debris"));
        assertTrue(search.matches("hybrid_1"));
        search.focusAnomalies(Set.of("spatial_breach"));
        assertTrue(search.matches("hybrid_1"));
    }

    @Test
    void everyShippedCrossPollinatedNodeLightsUpForEachOfItsAnomalies() throws Exception {
        List<AtlasTreeSearch.Entry> entries = new ArrayList<>();
        List<AtlasTreeNode> hybrids = new ArrayList<>();
        for (AtlasTreeNode node : ShippedAtlasTree.load().topology().nodes()) {
            entries.add(new AtlasTreeSearch.Entry(node.getId(), node.getTags(), List.of()));
            if (new HashSet<>(node.getTags()).size() > 1) {
                hybrids.add(node);
            }
        }
        AtlasTreeSearch search = new AtlasTreeSearch();
        search.index(entries);

        assertFalse(hybrids.isEmpty());
        for (AtlasTreeNode hybrid : hybrids) {
            for (String anomaly : hybrid.getTags()) {
                search.focusAnomalies(Set.of(anomaly));
                assertTrue(search.matches(hybrid.getId()), hybrid.getId() + " for " + anomaly);
            }
        }
    }
}
