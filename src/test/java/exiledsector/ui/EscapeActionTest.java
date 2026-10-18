package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeActionTest {

    @Test
    void modesWithTheirOwnKeyHandlingLeaveEscapeAlone() {
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.MODAL, true, true, true, true, false));
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.HYPERSPACE, false, true, true, true, false));
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.ROOT_CHOICE, false, false, false, false, false));
    }

    @Test
    void theWorkbenchAlwaysStepsBackThroughSocketPlacement() {
        assertEquals(EscapeAction.SOCKET_PLACEMENT, EscapeAction.resolve(CanvasMode.WORKBENCH, true, true, true, true, false));
    }

    @Test
    void aRunningAllocationIsCancelledFirst() {
        assertEquals(EscapeAction.CANCEL_ALLOCATION, EscapeAction.resolve(CanvasMode.ALLOCATION_RUN, true, true, true, false, false));
        assertEquals(EscapeAction.CANCEL_ALLOCATION, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, true, true, true, true, false));
    }

    @Test
    void socketPlacementOutranksAnOpenDropdown() {
        assertEquals(EscapeAction.SOCKET_PLACEMENT, EscapeAction.resolve(CanvasMode.TREE, false, true, true, false, false));
        assertEquals(EscapeAction.SOCKET_PLACEMENT, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, false, true, true, true, false));
    }

    @Test
    void anOpenDropdownClosesBeforeTheTreeOrTheFleetViewDoesAnythingElse() {
        assertEquals(EscapeAction.CLOSE_DROPDOWN, EscapeAction.resolve(CanvasMode.TREE, false, false, true, false, false));
        assertEquals(EscapeAction.CLOSE_DROPDOWN, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, false, false, true, true, false));
    }

    @Test
    void followingTheFleetClosesTheSectionPanelBeforeStoppingTheFollow() {
        assertEquals(EscapeAction.CLOSE_HULL_UPGRADES_PANEL, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, false, false, false, true, false));
        assertEquals(EscapeAction.STOP_FOLLOWING, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, false, false, false, false, false));
    }

    @Test
    void viewingTheExoshipClosesADropdownFirstAndThenLeavesTheShip() {
        assertEquals(EscapeAction.CLOSE_DROPDOWN, EscapeAction.resolve(CanvasMode.EXOSHIP_VIEW, false, false, true, false, false));
        assertEquals(EscapeAction.STOP_FOLLOWING, EscapeAction.resolve(CanvasMode.EXOSHIP_VIEW, false, false, false, false, false));
        assertEquals(EscapeAction.CANCEL_ALLOCATION, EscapeAction.resolve(CanvasMode.EXOSHIP_VIEW, true, false, false, false, false));
    }

    @Test
    void anIdleTreePassesEscapeOnToTheSearchFieldAndThePanel() {
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.TREE, false, false, false, false, false));
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.TREE, false, false, false, true, false));
    }

    @Test
    void templateModeLeavesOnEscapeOnceNothingCloserIsOpen() {
        assertEquals(EscapeAction.EXIT_TEMPLATE_MODE, EscapeAction.resolve(CanvasMode.TREE, false, false, false, false, true));
        assertEquals(EscapeAction.CLOSE_DROPDOWN, EscapeAction.resolve(CanvasMode.TREE, false, false, true, false, true));
        assertEquals(EscapeAction.STOP_FOLLOWING, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, false, false, false, false, true));
    }
}
