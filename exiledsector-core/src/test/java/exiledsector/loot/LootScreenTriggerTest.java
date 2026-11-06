package exiledsector.loot;

import com.fs.starfarer.api.campaign.CoreUITabId;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootScreenTriggerTest {

    private final AtomicReference<CoreUITabId> shownTab = new AtomicReference<>();
    private final AtomicInteger closedCount = new AtomicInteger();
    private final LootScreenTrigger.ClosingWatch watch = new LootScreenTrigger.ClosingWatch(shownTab::get, closedCount::incrementAndGet);

    @Test
    void theLootCountsAsTakenOnceTheOpenedLootScreenCloses() {
        shownTab.set(CoreUITabId.CARGO);
        watch.advance(0f);
        watch.advance(0f);

        assertEquals(0, closedCount.get());
        assertFalse(watch.isDone());

        shownTab.set(null);
        watch.advance(0f);
        watch.advance(0f);

        assertEquals(1, closedCount.get());
        assertTrue(watch.isDone());
    }

    @Test
    void aLootScreenThatNeverOpensIsGivenUpOnWithoutReporting() {
        for (int frame = 0; frame <= LootScreenTrigger.MAX_UNOPENED_FRAMES; frame++) {
            watch.advance(0f);
        }

        assertEquals(0, closedCount.get());
        assertTrue(watch.isDone());
    }
}
