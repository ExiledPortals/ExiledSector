package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PointerGestureTest {

    private enum Mode { ROOT_CHOICE, TREE }

    private static final String NODE = "node";
    private static final String OTHER_NODE = "other";

    @Test
    void releasingOnThePressedTargetCommitsIt() {
        PointerGesture<Mode> gesture = new PointerGesture<>();
        gesture.pressTarget(Mode.TREE, NODE, 100f, 100f, true, false, true);

        assertFalse(gesture.moveTo(102f, 103f));
        assertTrue(gesture.releasedOn(NODE));
        assertTrue(gesture.ctrlDown());
        assertFalse(gesture.shiftDown());
    }

    @Test
    void releasingElsewhereDoesNotCommit() {
        PointerGesture<Mode> gesture = new PointerGesture<>();
        gesture.pressTarget(Mode.TREE, NODE, 100f, 100f, false, false, true);

        assertFalse(gesture.releasedOn(OTHER_NODE));
        assertFalse(gesture.releasedOn(null));
    }

    @Test
    void movingPastTheThresholdTurnsATargetPressIntoAPan() {
        PointerGesture<Mode> gesture = new PointerGesture<>();
        gesture.pressTarget(Mode.TREE, NODE, 100f, 100f, false, false, true);

        assertFalse(gesture.moveTo(100f + PointerGesture.DRAG_THRESHOLD, 100f));
        assertTrue(gesture.moveTo(100f + PointerGesture.DRAG_THRESHOLD + 1f, 100f));

        assertTrue(gesture.isPanning());
        assertFalse(gesture.releasedOn(NODE));
        assertEquals(100f, gesture.pressX());
        assertEquals(100f, gesture.pressY());
        assertFalse(gesture.moveTo(300f, 300f));
    }

    @Test
    void aPressThatMayNotPanIsCancelledByDragging() {
        PointerGesture<Mode> gesture = new PointerGesture<>();
        gesture.pressTarget(Mode.ROOT_CHOICE, NODE, 0f, 0f, false, false, false);

        assertFalse(gesture.moveTo(0f, 20f));

        assertFalse(gesture.isActive());
        assertFalse(gesture.releasedOn(NODE));
    }

    @Test
    void anEmptySpacePressIsAPanFromTheStart() {
        PointerGesture<Mode> gesture = new PointerGesture<>();
        gesture.pressPan(Mode.TREE, 10f, 10f);

        assertTrue(gesture.isPanning());
        assertFalse(gesture.moveTo(50f, 50f));
        assertFalse(gesture.releasedOn(NODE));
    }

    @Test
    void aModeChangeMakesTheGestureStale() {
        PointerGesture<Mode> gesture = new PointerGesture<>();
        gesture.pressTarget(Mode.ROOT_CHOICE, NODE, 0f, 0f, false, false, false);

        assertFalse(gesture.isStaleIn(Mode.ROOT_CHOICE));
        assertTrue(gesture.isStaleIn(Mode.TREE));

        gesture.clear();
        assertFalse(gesture.isStaleIn(Mode.TREE));
        assertEquals(PointerGesture.Kind.NONE, gesture.kind());
    }

    @Test
    void aNewPressForgetsTheModifiersOfTheLastOne() {
        PointerGesture<Mode> gesture = new PointerGesture<>();
        gesture.pressTarget(Mode.TREE, NODE, 0f, 0f, true, true, true);
        gesture.pressPan(Mode.TREE, 0f, 0f);

        assertFalse(gesture.ctrlDown());
        assertFalse(gesture.shiftDown());
    }
}
