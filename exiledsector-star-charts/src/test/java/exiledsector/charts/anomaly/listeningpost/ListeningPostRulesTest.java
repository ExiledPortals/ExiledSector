package exiledsector.charts.anomaly.listeningpost;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListeningPostRulesTest {

    private static final float EPSILON = 1e-5f;

    @Test
    void suspicionBuildsInContactDrainsOutOfItAndStaysBetweenNoneAndFull() {
        assertEquals(ListeningPostRules.SUSPICION_PER_DAY, ListeningPostRules.suspicionAfter(0f, true, 1f), EPSILON);
        assertEquals(0.5f - ListeningPostRules.SUSPICION_DRAIN_PER_DAY, ListeningPostRules.suspicionAfter(0.5f, false, 1f), EPSILON);
        assertEquals(1f, ListeningPostRules.suspicionAfter(0.9f, true, 10f));
        assertEquals(0f, ListeningPostRules.suspicionAfter(0.1f, false, 10f));
    }

    @Test
    void theVaultPurgesAShareADayDownToItsFloor() {
        assertEquals(1f - ListeningPostRules.PURGE_PER_DAY, ListeningPostRules.vaultAfter(1f, 1f, 0f), EPSILON);
        assertEquals(ListeningPostRules.VAULT_FLOOR, ListeningPostRules.vaultAfter(1f, 100f, 0f), EPSILON);
        assertEquals(0.1f, ListeningPostRules.vaultAfter(0.1f, 1f, 0f), EPSILON);
    }

    @Test
    void thePurgeLineSlowsThePurgeAndNeverReversesIt() {
        assertEquals(1f - ListeningPostRules.PURGE_PER_DAY / 2f, ListeningPostRules.vaultAfter(1f, 1f, 50f), EPSILON);
        assertEquals(1f, ListeningPostRules.vaultAfter(1f, 1f, 150f), EPSILON);
        assertEquals(ListeningPostRules.PURGE_PER_DAY * 1.5f, ListeningPostRules.purgeRate(-50f), EPSILON);
    }

    @Test
    void picketCountsStayInRangeAndTheExtraLineIsCapped() {
        Random random = new Random(5L);
        for (int roll = 0; roll < 200; roll++) {
            int count = ListeningPostRules.picketCount(random, 0);
            assertTrue(count >= ListeningPostRules.MIN_PICKETS && count <= ListeningPostRules.MAX_PICKETS, String.valueOf(count));
        }
        assertTrue(ListeningPostRules.picketCount(random, 100) <= ListeningPostRules.MAX_PICKETS + ListeningPostRules.MAX_EXTRA_PICKETS);
        assertTrue(ListeningPostRules.picketCount(random, -4) >= ListeningPostRules.MIN_PICKETS);
    }

    @Test
    void theVaultGrowsWithTierAndQuantity() {
        assertEquals(ListeningPostRules.LOOT_STEPS, VaultLoot.LOOT.steps(1, 1f), EPSILON);
        assertTrue(VaultLoot.LOOT.steps(10, 1f) > VaultLoot.LOOT.steps(1, 1f));
        assertEquals(VaultLoot.LOOT.steps(5, 1f) * 2f, VaultLoot.LOOT.steps(5, 2f), EPSILON);
        assertEquals(0f, VaultLoot.LOOT.items(-1f));
    }
}
