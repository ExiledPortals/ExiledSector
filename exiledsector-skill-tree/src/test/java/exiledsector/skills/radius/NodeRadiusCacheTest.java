package exiledsector.skills.radius;

import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.tags.SkillTags;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeRadiusCacheTest {

    private static final SkillType HULL = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL).build();
    private static final SkillType EXOSHIP = new SkillType.Builder(ExoshipNodes.EXOSHIP_TYPE_ID, "Exoship", "a.png", SkillTier.SMALL).build();
    private static final Map<RadiusSize, Float> DISTANCES = Map.of(RadiusSize.SMALL, 100f, RadiusSize.MEDIUM, 200f, RadiusSize.LARGE, 300f);

    @Test
    void aNodeExactlyOnTheBoundaryIsInsideAndOneJustPastItIsNot() {
        NodeRadiusCache cache = NodeRadiusCache.of(List.of(node("centre", 0f, 0f), node("edge", 60f, 80f), node("past", 0f, -100.01f)),
                DISTANCES);

        assertEquals(Set.of("edge"), cache.nodeIdsWithin("centre", RadiusSize.SMALL));
        assertEquals(Set.of("edge", "past"), cache.nodeIdsWithin("centre", RadiusSize.MEDIUM));
    }

    @Test
    void theCentreIsNeverInsideItsOwnRadius() {
        NodeRadiusCache cache = NodeRadiusCache.of(List.of(node("centre", 0f, 0f), node("near", 10f, 0f)), DISTANCES);

        for (RadiusSize size : RadiusSize.values()) {
            assertFalse(cache.nodeIdsWithin("centre", size).contains("centre"), size.name());
        }
    }

    @Test
    void membershipIsSymmetric() {
        NodeRadiusCache cache = NodeRadiusCache.of(List.of(node("a", 0f, 0f), node("b", 150f, 0f)), DISTANCES);

        assertTrue(cache.nodeIdsWithin("a", RadiusSize.MEDIUM).contains("b"));
        assertTrue(cache.nodeIdsWithin("b", RadiusSize.MEDIUM).contains("a"));
        assertFalse(cache.nodeIdsWithin("a", RadiusSize.SMALL).contains("b"));
        assertFalse(cache.nodeIdsWithin("b", RadiusSize.SMALL).contains("a"));
    }

    @Test
    void theExoshipAndTheNodesItCarriesAreNeverCentresAndNeverInsideARadius() {
        SkillNode exoship = new SkillNode("exoship", EXOSHIP, List.of(), 0f, 0f);
        SkillNode carried = new SkillNode("carried", HULL, List.of(), 5f, 0f, SkillNodeDecoration.NONE, List.of(SkillTags.EXOTECH_REGION));
        NodeRadiusCache cache = NodeRadiusCache.of(List.of(node("centre", 0f, 0f), exoship, carried), DISTANCES);

        for (RadiusSize size : RadiusSize.values()) {
            assertEquals(Set.of(), cache.nodeIdsWithin("centre", size), size.name());
            assertEquals(Set.of(), cache.nodeIdsWithin("exoship", size), size.name());
            assertEquals(Set.of(), cache.nodeIdsWithin("carried", size), size.name());
        }
        assertTrue(cache.isCentre("centre"));
        assertFalse(cache.isCentre("exoship"));
        assertFalse(cache.isCentre("carried"));
    }

    @Test
    void theThreeSizesAreNested() {
        NodeRadiusCache cache = NodeRadiusCache.of(List.of(node("centre", 0f, 0f), node("small", 90f, 0f), node("medium", 0f, 190f),
                node("large", -290f, 0f), node("outside", 0f, -310f)), DISTANCES);

        assertEquals(Set.of("small"), cache.nodeIdsWithin("centre", RadiusSize.SMALL));
        assertEquals(Set.of("small", "medium"), cache.nodeIdsWithin("centre", RadiusSize.MEDIUM));
        assertEquals(Set.of("small", "medium", "large"), cache.nodeIdsWithin("centre", RadiusSize.LARGE));
    }

    @Test
    void anUnknownCentreHasAnEmptyRadius() {
        NodeRadiusCache cache = NodeRadiusCache.of(List.of(node("centre", 0f, 0f)), DISTANCES);

        assertEquals(Set.of(), cache.nodeIdsWithin("missing", RadiusSize.LARGE));
        assertFalse(cache.isCentre("missing"));
    }

    @Test
    void aZeroDistanceCoversNothingEvenForNodesOnTheSameSpot() {
        Map<RadiusSize, Float> unconfigured = Map.of(RadiusSize.SMALL, 0f, RadiusSize.MEDIUM, 0f, RadiusSize.LARGE, 0f);
        NodeRadiusCache cache = NodeRadiusCache.of(List.of(node("a", 0f, 0f), node("b", 0f, 0f)), unconfigured);

        assertEquals(Set.of(), cache.nodeIdsWithin("a", RadiusSize.LARGE));
    }

    @Test
    void theCachedSetsCannotBeChanged() {
        NodeRadiusCache cache = NodeRadiusCache.of(List.of(node("a", 0f, 0f), node("b", 10f, 0f)), DISTANCES);
        Set<String> nodeIdsWithin = cache.nodeIdsWithin("a", RadiusSize.SMALL);

        assertThrows(UnsupportedOperationException.class, () -> nodeIdsWithin.add("c"));
    }

    @Test
    void theCacheKnowsWhichDistancesItWasBuiltWith() {
        NodeRadiusCache cache = NodeRadiusCache.of(List.of(), DISTANCES);

        assertTrue(cache.isBuiltWith(DISTANCES));
        assertFalse(cache.isBuiltWith(new HashMap<>(DISTANCES)));
    }

    private static SkillNode node(String id, float treeX, float treeY) {
        return new SkillNode(id, HULL, List.of(), treeX, treeY);
    }
}
