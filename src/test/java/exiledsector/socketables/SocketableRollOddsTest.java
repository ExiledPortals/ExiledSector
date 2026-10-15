package exiledsector.socketables;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableRollOddsTest {

    private static final double EXACT = 1e-9;

    private static SocketableDefinition definition(boolean unique, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new SocketableDefinition("odds", SocketType.SUBROUTINE, "Odds", SocketableDefinition.FALLBACK_ICON, 1f, unique, "",
                prefixes, suffixes, "");
    }

    @Test
    void aUniqueAlwaysHasEveryModifier() {
        SocketableDefinition unique = definition(true, List.of(new PoolEntry("A", 1f, 2f, 5f), new PoolEntry("B", 1f, 2f, 1f)), List.of());

        assertEquals(Map.of("A", 1.0, "B", 1.0), SocketableRollOdds.foundChances(unique));
    }

    @Test
    void effectCountChancesAddUpToOne() {
        double total = 0.0;
        for (int effectCount : SocketableRollOdds.EFFECT_COUNTS) {
            total += SocketableRollOdds.effectCountChance(effectCount);
        }
        assertEquals(1.0, total, EXACT);
    }

    @Test
    void inclusionChanceFollowsWeightedDrawsWithoutReplacement() {
        assertEquals(0.25, SocketableRollOdds.inclusionChance(List.of(1.0, 3.0), 0, 1), EXACT);
        assertEquals(1.0, SocketableRollOdds.inclusionChance(List.of(1.0, 3.0), 0, 2), EXACT);
        assertEquals(0.25 + 0.25 / 3.0 + 0.25, SocketableRollOdds.inclusionChance(List.of(1.0, 1.0, 2.0), 0, 2), EXACT);
    }

    @Test
    void aSmallPoolGetsTheExpectedChances() {
        SocketableDefinition small = definition(false, List.of(new PoolEntry("A", 1f, 2f, 1f), new PoolEntry("B", 1f, 2f, 1f)),
                List.of(new PoolEntry("C", 1f, 2f, 1f)));

        Map<String, Double> found = SocketableRollOdds.foundChances(small);
        Map<String, Double> synthesised = SocketableRollOdds.synthesisedChances(small);

        assertEquals(0.55 * 0.5 + 0.35 * (0.5 + 0.25) + 0.10, found.get("A"), 1e-6);
        assertEquals(1.0, found.get("C"), 1e-6);
        assertEquals(0.5, synthesised.get("A"), EXACT);
        assertEquals(1.0, synthesised.get("C"), EXACT);
    }

    @Test
    void theChancesMatchWhatTheRollerActuallyRolls() {
        SocketableDefinition weighted = definition(false,
                List.of(new PoolEntry("P1", 1f, 2f, 1500f), new PoolEntry("P2", 1f, 2f, 250f), new PoolEntry("P3", 1f, 2f, 1000f),
                        new PoolEntry("P4", 1f, 2f, 25f)),
                List.of(new PoolEntry("S1", 1f, 2f, 1500f), new PoolEntry("S2", 1f, 2f, 50f), new PoolEntry("S3", 1f, 2f, 500f)));
        int sampleCount = 20000;
        Map<String, Integer> foundCounts = new HashMap<>();
        Map<String, Integer> synthesisedCounts = new HashMap<>();
        for (long seed = 0; seed < sampleCount; seed++) {
            for (RolledEffect effect : SocketableRoller.roll(weighted, seed, false)) {
                foundCounts.merge(effect.effectName(), 1, Integer::sum);
            }
            for (RolledEffect effect : SocketableRoller.rollCommon(weighted, seed, false)) {
                synthesisedCounts.merge(effect.effectName(), 1, Integer::sum);
            }
        }
        assertMatches(SocketableRollOdds.foundChances(weighted), foundCounts, sampleCount);
        assertMatches(SocketableRollOdds.synthesisedChances(weighted), synthesisedCounts, sampleCount);
    }

    private static void assertMatches(Map<String, Double> chances, Map<String, Integer> counts, int sampleCount) {
        chances.forEach((effectName, chance) -> {
            double observed = counts.getOrDefault(effectName, 0) / (double) sampleCount;
            assertTrue(Math.abs(observed - chance) < 0.015, effectName + " rolled " + observed + " but the odds say " + chance);
        });
    }

    @Test
    void aRangeIsWrittenFromWeakestToStrongest() {
        String range = new RolledEffect("HULL_MULT", 6f).rangeDescription(new PoolEntry("HULL_MULT", 4f, 6f, 1f)).plain();
        String fixed = new RolledEffect("HULL_MULT", 5f).rangeDescription(new PoolEntry("HULL_MULT", 5f, 5f, 1f)).plain();
        String scaled = new RolledEffect("ARMOR_FLAT", 5f).rangeDescription(PoolEntry.perHullSize("ARMOR_FLAT", 1f, List.of(5f, 10f, 15f, 20f)))
                .plain();

        assertTrue(range.contains("4 to 6%"), range);
        assertTrue(fixed.contains("5%") && !fixed.contains(" to "), fixed);
        assertTrue(scaled.contains("5/10/15/20"), scaled);

        String twoValues = new RolledEffect("FLUX_SCALED_RATE_OF_FIRE_PERCENT", 13f)
                .rangeDescription(new PoolEntry("FLUX_SCALED_RATE_OF_FIRE_PERCENT", 5f, 13f, 1f)).plain();
        assertEquals(2, twoValues.chars().filter(character -> character == '(').count(), twoValues);
        String untaggedBefore = new RolledEffect("BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT", 9f)
                .rangeDescription(new PoolEntry("BALLISTIC_WEAPON_PIERCE_CHANCE_PERCENT", 5f, 9f, 1f)).plain();
        assertTrue(untaggedBefore.contains("5 to 9%"), untaggedBefore);
    }
}
