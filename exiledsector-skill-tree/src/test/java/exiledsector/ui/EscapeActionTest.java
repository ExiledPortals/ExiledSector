package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeActionTest {

    @Test
    void modesWithTheirOwnKeyHandlingLeaveEscapeAlone() {
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.MODAL, true, true, false));
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.HYPERSPACE, true, true, false));
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.ROOT_CHOICE, false, false, false));
    }

    @Test
    void theWorkbenchAlwaysStepsBackThroughSocketPlacement() {
        assertEquals(EscapeAction.SOCKET_PLACEMENT, EscapeAction.resolve(CanvasMode.WORKBENCH, true, true, false));
    }

    @Test
    void socketPlacementOutranksAnOpenDropdown() {
        assertEquals(EscapeAction.SOCKET_PLACEMENT, EscapeAction.resolve(CanvasMode.TREE, true, true, false));
        assertEquals(EscapeAction.SOCKET_PLACEMENT, EscapeAction.resolve(CanvasMode.EXOSHIP_VIEW, true, true, false));
    }

    @Test
    void viewingTheExoshipClosesADropdownFirstAndThenLeavesTheShip() {
        assertEquals(EscapeAction.CLOSE_DROPDOWN, EscapeAction.resolve(CanvasMode.EXOSHIP_VIEW, false, true, false));
        assertEquals(EscapeAction.STOP_FOLLOWING, EscapeAction.resolve(CanvasMode.EXOSHIP_VIEW, false, false, false));
        assertEquals(EscapeAction.STOP_FOLLOWING, EscapeAction.resolve(CanvasMode.EXOSHIP_VIEW, false, false, true));
    }

    @Test
    void anIdleTreePassesEscapeOnToTheSearchFieldAndThePanel() {
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.TREE, false, false, false));
    }

    @Test
    void templateModeLeavesOnEscapeOnceNothingCloserIsOpen() {
        assertEquals(EscapeAction.EXIT_TEMPLATE_MODE, EscapeAction.resolve(CanvasMode.TREE, false, false, true));
        assertEquals(EscapeAction.CLOSE_DROPDOWN, EscapeAction.resolve(CanvasMode.TREE, false, true, true));
    }
}
