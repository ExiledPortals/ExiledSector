package exiledsector.skills.skilleffect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpiringStacksTest {

    @Test
    void eachStackExpiresOnItsOwnTimer() {
        ExpiringStacks<Float> stacks = new ExpiringStacks<>(5, 10f);
        stacks.add(0f, 2f);
        stacks.add(4f, 3f);

        stacks.dropExpired(9.9f);
        assertEquals(2, stacks.size());
        assertEquals(5f, stacks.sum(Float::doubleValue), 1e-6f);

        stacks.dropExpired(10f);
        assertEquals(1, stacks.size());
        assertEquals(3f, stacks.sum(Float::doubleValue), 1e-6f);

        stacks.dropExpired(14f);
        assertTrue(stacks.isEmpty());
    }

    @Test
    void aStackGainedAtTheCapReplacesTheOldest() {
        ExpiringStacks<Float> stacks = new ExpiringStacks<>(2, 10f);
        stacks.add(0f, 1f);
        stacks.add(1f, 2f);
        stacks.add(2f, 4f);

        assertEquals(2, stacks.size());
        assertEquals(6f, stacks.sum(Float::doubleValue), 1e-6f);
    }

    @Test
    void addingDropsStacksThatHaveAlreadyExpired() {
        ExpiringStacks<Void> stacks = new ExpiringStacks<>(3, 5f);
        stacks.add(0f);
        stacks.add(0f);
        stacks.add(6f);

        assertEquals(1, stacks.size());
        stacks.clear();
        assertTrue(stacks.isEmpty());
    }
}
