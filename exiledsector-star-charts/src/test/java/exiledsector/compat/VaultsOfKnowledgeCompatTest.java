package exiledsector.compat;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultsOfKnowledgeCompatTest {

    @Test
    void theDefenceRollStaysInsideAoTDsRangeAndReachesBothEnds() {
        Random random = new Random(3L);
        Set<Integer> rolled = new HashSet<>();
        for (int roll = 0; roll < 2000; roll++) {
            int points = VaultsOfKnowledgeCompat.defenceFleetPoints(VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MIN,
                    VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MAX, random);
            assertTrue(points >= VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MIN && points <= VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MAX,
                    String.valueOf(points));
            rolled.add(points);
        }
        assertTrue(rolled.contains(VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MIN));
        assertTrue(rolled.contains(VaultsOfKnowledgeCompat.DEFAULT_DEFENCE_MAX));
    }

    @Test
    void aSwappedOrNegativeRangeStillRolls() {
        int swapped = VaultsOfKnowledgeCompat.defenceFleetPoints(200, 100, new Random(1L));
        assertTrue(swapped >= 100 && swapped <= 200, String.valueOf(swapped));
        assertEquals(0, VaultsOfKnowledgeCompat.defenceFleetPoints(-5, -10, new Random(1L)));
    }
}
