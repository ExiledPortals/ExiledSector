package exiledsector.skills.tags;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagWeightsTest {

    private static final Set<String> BALLISTIC_WEAPON = Set.of("weapon", "ballistic", "damage");

    @Test
    void withNoSourcesEveryModifierKeepsItsPoolWeight() {
        assertFalse(TagWeights.NONE.hasSources());
        assertEquals(1f, TagWeights.NONE.multiplier(BALLISTIC_WEAPON));
        assertEquals(2.5f, TagWeights.NONE.weight(2.5f, BALLISTIC_WEAPON));
        assertEquals(TagWeights.NONE.multiplier(BALLISTIC_WEAPON), TagWeights.of(List.of()).multiplier(BALLISTIC_WEAPON));
    }

    @Test
    void aSourceThatMatchesNoTagLeavesTheWeightAlone() {
        TagWeights shieldBias = TagWeights.NONE.with(Map.of("shield", 5f));

        assertTrue(shieldBias.hasSources());
        assertEquals(1f, shieldBias.multiplier(BALLISTIC_WEAPON));
    }

    @Test
    void anyMatchingZeroBlocksEvenBesideABoost() {
        TagWeights noBallistics = TagWeights.NONE.with(Map.of("weapon", 10f, "ballistic", 0f));

        assertEquals(0f, noBallistics.multiplier(BALLISTIC_WEAPON));
        assertEquals(0f, noBallistics.weight(4f, BALLISTIC_WEAPON));
        assertEquals(10f, noBallistics.multiplier(Set.of("weapon", "energy")));
    }

    @Test
    void theStrongestMatchWithinOneSourceApplies() {
        assertEquals(4f, TagWeights.NONE.with(Map.of("weapon", 2f, "ballistic", 4f)).multiplier(BALLISTIC_WEAPON));
        assertEquals(0.1f, TagWeights.NONE.with(Map.of("weapon", 0.5f, "ballistic", 0.1f)).multiplier(BALLISTIC_WEAPON));
        assertEquals(0.2f, TagWeights.NONE.with(Map.of("weapon", 3f, "ballistic", 0.2f)).multiplier(BALLISTIC_WEAPON));
        assertEquals(2f, TagWeights.NONE.with(Map.of("weapon", 2f, "ballistic", 0.5f)).multiplier(BALLISTIC_WEAPON));
    }

    @Test
    void separateSourcesMultiply() {
        TagWeights combined = TagWeights.of(List.of(Map.of("weapon", 2f), Map.of("damage", 3f), Map.of("shield", 7f)));

        assertEquals(6f, combined.multiplier(BALLISTIC_WEAPON));
        assertEquals(9f, combined.weight(1.5f, BALLISTIC_WEAPON));
        assertEquals(0f, combined.with(Map.of("damage", 0f)).multiplier(BALLISTIC_WEAPON));
    }

    @Test
    void aTaglessModifierIsNeverBoostedOrBlocked() {
        TagWeights everything = TagWeights.of(List.of(Map.of("weapon", 0f), Map.of("shield", 10f)));

        assertEquals(1f, everything.multiplier(Set.of()));
        assertEquals(3f, everything.weight(3f, Set.of()));
    }

    @Test
    void aMultiplierMustBeANumberOfZeroOrMore() {
        assertThrows(IllegalArgumentException.class, () -> TagWeights.NONE.with(Map.of("weapon", -1f)));
        assertThrows(IllegalArgumentException.class, () -> TagWeights.NONE.with(Map.of("weapon", Float.NaN)));
        assertThrows(IllegalArgumentException.class, () -> TagWeights.NONE.with(Map.of("weapon", Float.POSITIVE_INFINITY)));
    }
}
