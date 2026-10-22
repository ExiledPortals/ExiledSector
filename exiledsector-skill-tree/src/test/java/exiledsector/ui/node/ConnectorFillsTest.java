package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorFillsTest {

    private static final float EPSILON = 1e-4f;

    private final ConnectorFills fills = new ConnectorFills();
    private final List<String> pulses = new ArrayList<>();

    private void advance(float seconds) {
        fills.advance(seconds, pulses::add);
    }

    @Test
    void aConnectorWithNoFillIsDrawnFullAndCostsNothingToLookUp() {
        assertSame(ConnectorFills.FillRange.FULL, fills.filledRange("a", "b"));
        assertTrue(fills.isEmpty());
    }

    @Test
    void theFillGrowsFromThePreviouslyAllocatedNodeOverTheFillDuration() {
        fills.start("old", "new");

        advance(0.25f * ConnectorFills.FILL_SECONDS);
        ConnectorFills.FillRange fromOld = fills.filledRange("old", "new");
        assertEquals(0f, fromOld.startFraction(), EPSILON);
        assertEquals(0.25f, fromOld.endFraction(), EPSILON);

        ConnectorFills.FillRange fromNew = fills.filledRange("new", "old");
        assertEquals(0.75f, fromNew.startFraction(), EPSILON);
        assertEquals(1f, fromNew.endFraction(), EPSILON);
    }

    @Test
    void aFinishedFillIsForgottenSoTheConnectorIsDrawnFull() {
        fills.start("old", "new");

        advance(ConnectorFills.FILL_SECONDS);

        assertSame(ConnectorFills.FillRange.FULL, fills.filledRange("old", "new"));
        assertTrue(fills.isEmpty());
    }

    @Test
    void theRingPulseFiresOnlyOnceTheFillHasFinished() {
        fills.start("old", "new");
        fills.schedulePulse("new");

        advance(0.75f * ConnectorFills.FILL_SECONDS);
        assertTrue(pulses.isEmpty());
        advance(0.25f * ConnectorFills.FILL_SECONDS);
        assertEquals(List.of("new"), pulses);
        advance(ConnectorFills.FILL_SECONDS);
        assertEquals(List.of("new"), pulses);
    }

    @Test
    void deallocatingANodeCancelsItsFillsAndItsPendingPulse() {
        fills.start("old", "new");
        fills.start("new", "other");
        fills.start("old", "unrelated");
        fills.schedulePulse("new");

        fills.cancel("new");
        advance(ConnectorFills.FILL_SECONDS);

        assertTrue(pulses.isEmpty());
        assertSame(ConnectorFills.FillRange.FULL, fills.filledRange("old", "new"));
        assertSame(ConnectorFills.FillRange.FULL, fills.filledRange("new", "other"));
    }

    @Test
    void fillRangesKnowWhichPartsOfTheLineAreFilled() {
        ConnectorFills.FillRange range = new ConnectorFills.FillRange(0f, 0.4f);

        assertTrue(range.contains(0f, 0.4f));
        assertFalse(range.contains(0.3f, 0.5f));
        assertFalse(range.isFull());
        assertTrue(ConnectorFills.FillRange.FULL.isFull());
    }
}
