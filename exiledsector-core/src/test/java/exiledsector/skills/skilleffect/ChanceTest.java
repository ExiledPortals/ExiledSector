package exiledsector.skills.skilleffect;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChanceTest {

    @Test
    void aRollSucceedsWhenTheRandomFallsBelowTheChance() {
        Random random = mock(Random.class);
        when(random.nextFloat()).thenReturn(0.25f);

        assertTrue(Chance.roll(random, 30f));
        assertFalse(Chance.roll(random, 25f));
        assertTrue(Chance.roll(random, 100f));
    }

    @Test
    void noChanceNeverRolls() {
        Random random = mock(Random.class);

        assertFalse(Chance.roll(random, 0f));
        assertFalse(Chance.roll(random, -5f));
        verify(random, never()).nextFloat();
    }
}
