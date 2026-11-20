package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlagshipFormationTest {

    private static final float RADIUS = 130f;
    private static final float DELTA = 0.001f;

    @Test
    void everyEscortGetsASlotAndNoneWithoutEscorts() {
        for (int escorts = 0; escorts <= 8; escorts++) {
            assertEquals(escorts, FlagshipFormation.slots(escorts, RADIUS).size());
        }
    }

    @Test
    void anOddFormationOfThreeOrMoreSendsOneEscortAheadOnTheCentreline() {
        for (int escorts : new int[] {3, 5, 7}) {
            FlagshipFormation.Slot lead = FlagshipFormation.slots(escorts, RADIUS).get(0);

            assertEquals(0f, lead.lateral(), DELTA);
            assertEquals(RADIUS * FlagshipFormation.LEAD_FORWARD_SHARE, lead.forward(), DELTA);
        }
    }

    @Test
    void aLoneEscortFlanksTheFlagshipInsteadOfLeadingIt() {
        FlagshipFormation.Slot only = FlagshipFormation.slots(1, RADIUS).get(0);

        assertTrue(only.lateral() > 0f);
        assertEquals(RADIUS * FlagshipFormation.FLANK_FORWARD_SHARE, only.forward(), DELTA);
    }

    @Test
    void flankersComeInMirroredPairsThatWidenAndFallBackRankByRank() {
        List<FlagshipFormation.Slot> slots = FlagshipFormation.slots(6, RADIUS);

        for (int pair = 0; pair < 3; pair++) {
            FlagshipFormation.Slot left = slots.get(pair * 2);
            FlagshipFormation.Slot right = slots.get(pair * 2 + 1);
            assertEquals(left.forward(), right.forward(), DELTA);
            assertEquals(left.lateral(), -right.lateral(), DELTA);
            assertTrue(left.lateral() > 0f);
            if (pair > 0) {
                FlagshipFormation.Slot ahead = slots.get(pair * 2 - 2);
                assertTrue(left.forward() < ahead.forward());
                assertTrue(left.lateral() > ahead.lateral());
            }
        }
    }

    @Test
    void evenTheLargestFormationStaysInsideTheNodeAndClearOfTheFlagshipsFlanks() {
        for (FlagshipFormation.Slot slot : FlagshipFormation.slots(7, RADIUS)) {
            assertTrue(slot.reach() <= RADIUS, "reach " + slot.reach());
            boolean ahead = slot.lateral() == 0f && slot.forward() >= RADIUS * FlagshipFormation.LEAD_FORWARD_SHARE;
            assertTrue(ahead || Math.abs(slot.lateral()) >= RADIUS * FlagshipFormation.FLANK_LATERAL_SHARE, "slot " + slot);
        }
    }

    @Test
    void aSlotTurnsWithTheFlagshipAndFacesTheSameWay() {
        FlagshipFormation.Slot slot = new FlagshipFormation.Slot(10f, 4f);

        assertEquals(10f, slot.xAt(0f, 0f), DELTA);
        assertEquals(4f, slot.yAt(0f, 0f), DELTA);
        assertEquals(-4f, slot.xAt(0f, 90f), DELTA);
        assertEquals(10f, slot.yAt(0f, 90f), DELTA);
        assertEquals(slot.reach(), (float) Math.hypot(slot.xAt(0f, 37f), slot.yAt(0f, 37f)), DELTA);
        assertEquals(37f, slot.facingAt(5f, 37f), DELTA);
        assertEquals(EscortPath.CRUISING_ENGINE_LEVEL, slot.engineLevelAt(3f), DELTA);
    }
}
