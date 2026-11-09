package exiledsector.ui.node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrationHoldTest {

    private static final String NODE_ID = "bulkheads_1";

    @Test
    void aShortClickIsNotClaimedSoItStillActsAsAClick() {
        IntegrationHold hold = new IntegrationHold();
        hold.begin(NODE_ID, true);

        assertEquals(IntegrationHold.Step.NONE, hold.advance(IntegrationHold.CLICK_SECONDS / 2f));

        assertFalse(hold.release());
    }

    @Test
    void holdingPastAClickClaimsTheReleaseAndGrowsTheCircuit() {
        IntegrationHold hold = new IntegrationHold();
        hold.begin(NODE_ID, true);

        assertEquals(IntegrationHold.Step.CLAIMED, hold.advance(IntegrationHold.CLICK_SECONDS));
        assertEquals(IntegrationHold.Step.NONE, hold.advance(IntegrationHold.HOLD_SECONDS / 2f - IntegrationHold.CLICK_SECONDS));

        assertEquals(0.5f, hold.progressFor(NODE_ID), 0.001f);
        assertEquals(0f, hold.progressFor("other_1"));
        assertTrue(hold.release());
    }

    @Test
    void holdingTheFullTimeCompletesOnceAndTheReleaseStaysClaimed() {
        IntegrationHold hold = new IntegrationHold();
        hold.begin(NODE_ID, true);
        hold.advance(IntegrationHold.CLICK_SECONDS);

        assertEquals(IntegrationHold.Step.COMPLETED, hold.advance(IntegrationHold.HOLD_SECONDS));
        assertEquals(IntegrationHold.Step.NONE, hold.advance(1f));

        assertNull(hold.heldNodeId());
        assertTrue(hold.release());
    }

    @Test
    void anUnaffordableHoldWarnsButNeverGrowsOrCompletes() {
        IntegrationHold hold = new IntegrationHold();
        hold.begin(NODE_ID, false);

        assertEquals(IntegrationHold.Step.CLAIMED, hold.advance(IntegrationHold.CLICK_SECONDS));
        hold.warn("Not enough");
        assertEquals(IntegrationHold.Step.NONE, hold.advance(IntegrationHold.HOLD_SECONDS * 2f));

        assertEquals(0f, hold.progressFor(NODE_ID));
        assertEquals("Not enough", hold.warningFor(NODE_ID));
        assertTrue(hold.release());
        assertNull(hold.warningFor(NODE_ID));
    }

    @Test
    void releasingEarlyRetractsTheCircuit() {
        IntegrationHold hold = new IntegrationHold();
        hold.begin(NODE_ID, true);
        hold.advance(IntegrationHold.HOLD_SECONDS / 2f);
        hold.cancel();

        hold.advance(IntegrationHold.RETRACT_SECONDS / 4f);
        assertEquals(0.25f, hold.progressFor(NODE_ID), 0.001f);

        hold.advance(IntegrationHold.RETRACT_SECONDS);
        assertEquals(0f, hold.progressFor(NODE_ID));
        assertNull(hold.heldNodeId());
    }
}
