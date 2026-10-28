package exiledsector.charts.anomaly.heist;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeistRulesTest {

    private static final float EPSILON = 1e-5f;

    @Test
    void suspicionBuildsInContactDrainsOutOfItAndStaysBetweenNoneAndFull() {
        assertEquals(HeistRules.SUSPICION_PER_DAY, HeistRules.suspicionAfter(0f, true, 1f), EPSILON);
        assertEquals(0.5f - HeistRules.SUSPICION_DRAIN_PER_DAY, HeistRules.suspicionAfter(0.5f, false, 1f), EPSILON);
        assertEquals(1f, HeistRules.suspicionAfter(0.9f, true, 10f));
        assertEquals(0f, HeistRules.suspicionAfter(0.1f, false, 10f));
    }

    @Test
    void theVaultPurgesAShareADayDownToItsFloor() {
        assertEquals(1f - HeistRules.PURGE_PER_DAY, HeistRules.vaultAfter(1f, 1f, 0f), EPSILON);
        assertEquals(HeistRules.VAULT_FLOOR, HeistRules.vaultAfter(1f, 100f, 0f), EPSILON);
        assertEquals(0.1f, HeistRules.vaultAfter(0.1f, 1f, 0f), EPSILON);
    }

    @Test
    void thePurgeLineSlowsThePurgeAndNeverReversesIt() {
        assertEquals(1f - HeistRules.PURGE_PER_DAY / 2f, HeistRules.vaultAfter(1f, 1f, 50f), EPSILON);
        assertEquals(1f, HeistRules.vaultAfter(1f, 1f, 150f), EPSILON);
        assertEquals(HeistRules.PURGE_PER_DAY * 1.5f, HeistRules.purgeRate(-50f), EPSILON);
    }

    @Test
    void picketCountsStayInRangeAndTheExtraLineIsCapped() {
        Random random = new Random(5L);
        for (int roll = 0; roll < 200; roll++) {
            int count = HeistRules.picketCount(random, 0);
            assertTrue(count >= HeistRules.MIN_PICKETS && count <= HeistRules.MAX_PICKETS, String.valueOf(count));
        }
        assertTrue(HeistRules.picketCount(random, 100) <= HeistRules.MAX_PICKETS + HeistRules.MAX_EXTRA_PICKETS);
        assertTrue(HeistRules.picketCount(random, -4) >= HeistRules.MIN_PICKETS);
    }

    @Test
    void theVaultGrowsWithTierAndQuantity() {
        assertEquals(HeistRules.LOOT_STEPS, HeistRules.lootSteps(1, 1f), EPSILON);
        assertTrue(HeistRules.lootSteps(10, 1f) > HeistRules.lootSteps(1, 1f));
        assertEquals(HeistRules.lootSteps(5, 1f) * 2f, HeistRules.lootSteps(5, 2f), EPSILON);
        assertEquals(0f, HeistRules.itemMult(-1f));
    }
}
