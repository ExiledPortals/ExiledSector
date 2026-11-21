package exiledsector.data;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomRoundingTest {

    private static final int TRIALS = 10000;

    @Test
    void theFractionRoundsUpAsOftenAsItsSize() {
        Random random = new Random(5L);
        double total = 0;
        for (int i = 0; i < TRIALS; i++) {
            int rounded = RandomRounding.round(2.25f, random);
            assertTrue(rounded == 2 || rounded == 3);
            total += rounded;
        }
        assertEquals(2.25, total / TRIALS, 0.02);
    }

    @Test
    void nothingExpectedGivesNothingWithoutADraw() {
        Random random = new Random(7L);
        long untouchedNext = new Random(7L).nextLong();

        assertEquals(0, RandomRounding.round(0f, random));
        assertEquals(0, RandomRounding.round(-1f, random));
        assertEquals(0, RandomRounding.round(Float.NaN, random));
        assertEquals(untouchedNext, random.nextLong());
    }

    @Test
    void roundingWithOneDrawAlwaysDrawsEvenWhenNothingIsExpected() {
        Random random = new Random(7L);
        Random expectedSequence = new Random(7L);
        expectedSequence.nextFloat();

        assertEquals(0, RandomRounding.roundWithOneDraw(0f, random));
        assertEquals(expectedSequence.nextLong(), random.nextLong());
    }
}
