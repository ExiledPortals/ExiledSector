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
        Map<String, SocketableDefinition.PoolEntry> pool = military.pool().stream()
                .collect(Collectors.toMap(SocketableDefinition.PoolEntry::effectName, Function.identity()));
        for (long seed = 0; seed < SAMPLES; seed++) {
            List<RolledEffect> rolled = SocketableRoller.roll(military, seed);
            Set<String> names = new HashSet<>();
            for (RolledEffect effect : rolled) {
                SocketableDefinition.PoolEntry entry = pool.get(effect.effectName());
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
    void theEffectCountThresholdsFollowTheProposedOdds() {
        assertEquals(2, SocketableRoller.effectCount(0f));
        assertEquals(2, SocketableRoller.effectCount(0.549f));
        assertEquals(3, SocketableRoller.effectCount(0.55f));
        assertEquals(3, SocketableRoller.effectCount(0.899f));
        assertEquals(4, SocketableRoller.effectCount(0.9f));
    }

    @Test
    void aPoolSmallerThanTheRollIsUsedUpWithoutFailing() throws Exception {
        SocketableDefinitions.register(new org.json.JSONArray().put(SocketableFixtures.row("tiny", "subroutine", "HULL_MULT:4:6")));

        for (long seed = 0; seed < 50; seed++) {
            assertEquals(1, SocketableRoller.roll(SocketableDefinitions.get("tiny"), seed).size());
        }
    }
}
