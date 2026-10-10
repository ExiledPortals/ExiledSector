package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolEntryTest {

    private static PoolEntry range(float min, float max) {
        return new PoolEntry("HULL_MULT", min, max, 1f);
    }

    @Test
    void everyWholeNumberInARangeIsEquallyLikelyIncludingTheEnds() {
        Random random = new Random(5);
        int[] counts = new int[3];
        for (int i = 0; i < 30000; i++) {
            counts[(int) range(4f, 6f).roll(random) - 4]++;
        }
        for (int count : counts) {
            assertEquals(1 / 3.0, count / 30000.0, 0.02);
        }
    }

    @Test
    void decimalRangesRollInStepsOfTheirFinestDecimalPlace() {
        Random random = new Random(5);
        Set<Float> seen = new TreeSet<>();
        for (int i = 0; i < 2000; i++) {
            float rolled = range(0.15f, 0.25f).roll(random);
            assertTrue(rolled >= 0.15f - 0.0001f && rolled <= 0.25f + 0.0001f, String.valueOf(rolled));
            seen.add(Math.round(rolled * 100f) / 100f);
        }
        assertEquals(11, seen.size());
        float halfStep = range(-12.5f, -11.5f).roll(random);
        assertTrue(halfStep >= -12.5f && halfStep <= -11.5f);
        assertEquals(halfStep, Math.round(halfStep * 10f) / 10f, 0.0001f);
    }

    @Test
    void theStepCountMatchesWhatARollCanLandOn() {
        assertEquals(3, range(4f, 6f).stepCount());
        assertEquals(11, range(0.15f, 0.25f).stepCount());
        assertEquals(3, range(0.01f, 0.03f).stepCount());
        assertEquals(6, range(4f, 4.5f).stepCount());
        assertEquals(11, range(-12.5f, -11.5f).stepCount());
        assertEquals(1, range(1f, 1f).stepCount());
    }

    @Test
    void decimalRangesThatStayBetweenTwoWholeNumbersCanStillVary() {
        assertTrue(range(0.15f, 0.25f).canVary());
        assertTrue(range(0.01f, 0.03f).canVary());
        assertTrue(range(1.5f, 2f).canVary());
        assertFalse(range(1f, 1f).canVary());
        assertFalse(range(-2.5f, -2.5f).canVary());
    }

    @Test
    void aFixedValueRollsToItselfAndStillDrawsFromTheRandomSoLaterRollsDoNotShift() {
        Random fixedRandom = new Random(9);
        Random referenceRandom = new Random(9);

        assertEquals(1f, range(1f, 1f).roll(fixedRandom));
        referenceRandom.nextInt(1);
        assertEquals(referenceRandom.nextInt(), fixedRandom.nextInt());
    }

    @Test
    void theAverageIsTheMiddleOfTheRangeRoundedAwayFromZeroToAWholeNumber() {
        assertEquals(5f, range(2f, 8f).average());
        assertEquals(5f, range(3f, 6f).average());
        assertEquals(-5f, range(-6f, -3f).average());
        assertEquals(0f, range(-2f, 2f).average());
        assertEquals(7f, range(7f, 7f).average());
    }

    @Test
    void decimalRangesAverageToTheirOwnPrecision() {
        assertEquals(0.18f, range(0.13f, 0.22f).average(), 1e-6f);
        assertEquals(0.02f, range(0.01f, 0.03f).average(), 1e-6f);
        assertEquals(1.8f, range(1.5f, 2f).average(), 1e-6f);
        assertEquals(5f, PoolEntry.parse("ARMOR_FLAT:5/10/15/20").average());
    }

    @Test
    void hullSizeEntriesAreFourFixedValuesThatNeverVary() {
        PoolEntry armor = PoolEntry.parse("ARMOR_FLAT:5/10/15/20:2");

        assertEquals(List.of(5f, 10f, 15f, 20f), armor.hullValues());
        assertEquals(2f, armor.weight());
        assertFalse(armor.canVary());
        assertEquals(5f, armor.roll(new Random(1)));
        assertEquals(15f, armor.valueFor(3f, HullSize.CRUISER));
        assertEquals(3f, armor.valueFor(3f, HullSize.FIGHTER));
        assertTrue(armor.listsEveryHullValueFor(null));
        assertFalse(armor.listsEveryHullValueFor(HullSize.CAPITAL_SHIP));
        assertFalse(range(4f, 6f).listsEveryHullValueFor(null));
        assertEquals(5f, range(4f, 6f).valueFor(5f, HullSize.CRUISER));
    }

    @Test
    void parsingOrdersTheBoundsAndDefaultsTheWeight() {
        assertEquals(new PoolEntry("HULL_MULT", -15f, -10f, 1f), PoolEntry.parse("HULL_MULT:-10:-15"));
        assertEquals(new PoolEntry("HULL_MULT", 4f, 6f, 2.5f), PoolEntry.parse("HULL_MULT:4:6:2.5"));
        assertThrows(IllegalArgumentException.class, () -> PoolEntry.parse("HULL_MULT:4"));
        assertThrows(IllegalArgumentException.class, () -> PoolEntry.parse("HULL_MULT:4:6:0"));
        assertThrows(IllegalArgumentException.class, () -> PoolEntry.parse("ARMOR_FLAT:5/10/15"));
    }

    @Test
    void tooltipRangesRunFromTheBoundNearestZero() {
        PoolEntry reduction = range(-15f, -10f);

        assertEquals(-10f, reduction.boundNearestZero());
        assertEquals(-15f, reduction.boundFarthestFromZero());
        assertEquals(4f, range(4f, 6f).boundNearestZero());
        assertEquals(6f, range(4f, 6f).boundFarthestFromZero());
    }
}
