package exiledsector.socketables;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableRollerTest {

    private static final int SAMPLES = 4000;

    private SocketableDefinition military;

    @BeforeEach
    void setUp() throws Exception {
        military = SocketableFixtures.registerMilitary();
    }

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
    }

    @Test
    void theSameSeedAlwaysRollsTheSameEffects() {
        assertEquals(SocketableRoller.roll(military, 42L), SocketableRoller.roll(military, 42L));
        assertNotEquals(SocketableRoller.roll(military, 42L), SocketableRoller.roll(military, 43L));
    }

    @Test
    void rollsAreWholeNumbersInsideTheirRangeWithNoRepeatedEffect() {
        Map<String, PoolEntry> pool = military.pool().stream()
                .collect(Collectors.toMap(PoolEntry::effectName, Function.identity()));
        for (long seed = 0; seed < SAMPLES; seed++) {
            List<RolledEffect> rolled = SocketableRoller.roll(military, seed);
            Set<String> names = new HashSet<>();
            for (RolledEffect effect : rolled) {
                PoolEntry entry = pool.get(effect.effectName());
                assertTrue(names.add(effect.effectName()), "repeated " + effect);
                assertTrue(effect.magnitude() >= entry.min() && effect.magnitude() <= entry.max(), effect.toString());
                assertEquals(Math.rint(effect.magnitude()), effect.magnitude());
            }
        }
    }

    @Test
    void mostRollsHaveTwoEffectsSomeThreeAndAFewFour() {
        int[] counts = new int[5];
        for (long seed = 0; seed < SAMPLES; seed++) {
            counts[SocketableRoller.roll(military, seed).size()]++;
        }
        assertEquals(0.55, counts[2] / (double) SAMPLES, 0.04);
        assertEquals(0.35, counts[3] / (double) SAMPLES, 0.04);
        assertEquals(0.10, counts[4] / (double) SAMPLES, 0.03);
    }

    @Test
    void commonRollsOnePrefixAndOneSuffixAndRaresNeverMoreThanTwoOfEither() {
        boolean twoPrefixes = false;
        boolean twoSuffixes = false;
        for (long seed = 0; seed < SAMPLES; seed++) {
            List<RolledEffect> rolled = SocketableRoller.roll(military, seed);
            long prefixes = rolled.stream().filter(effect -> military.isPrefix(effect.effectName())).count();
            long suffixes = rolled.size() - prefixes;
            assertTrue(prefixes <= 2 && suffixes <= 2, rolled.toString());
            if (rolled.size() == 2) {
                assertEquals(1, prefixes, rolled.toString());
            }
            twoPrefixes |= rolled.size() == 3 && prefixes == 2;
            twoSuffixes |= rolled.size() == 3 && suffixes == 2;
        }
        assertTrue(twoPrefixes && twoSuffixes, "three-effect rolls should split both ways");
    }

    @Test
    void aPoolSmallerThanTheRollIsUsedUpWithoutFailing() throws Exception {
        SocketableDefinitions.register(new org.json.JSONArray().put(SocketableFixtures.row("tiny", "subroutine", "HULL_MULT:4:6")));

        for (long seed = 0; seed < 50; seed++) {
            assertEquals(1, SocketableRoller.roll(SocketableDefinitions.get("tiny"), seed).size());
        }
    }
}
