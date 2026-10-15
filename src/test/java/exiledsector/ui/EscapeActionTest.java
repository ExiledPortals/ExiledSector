package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeActionTest {

    @Test
    void modesWithTheirOwnKeyHandlingLeaveEscapeAlone() {
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.MODAL, true, true, true, true));
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.HYPERSPACE, false, true, true, true));
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.ROOT_CHOICE, false, false, false, false));
    }

    @Test
    void theWorkbenchAlwaysStepsBackThroughSocketPlacement() {
        assertEquals(EscapeAction.SOCKET_PLACEMENT, EscapeAction.resolve(CanvasMode.WORKBENCH, true, true, true, true));
    }

    @Test
    void aRunningAllocationIsCancelledFirst() {
        assertEquals(EscapeAction.CANCEL_ALLOCATION, EscapeAction.resolve(CanvasMode.ALLOCATION_RUN, true, true, true, false));
        assertEquals(EscapeAction.CANCEL_ALLOCATION, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, true, true, true, true));
    }

    @Test
    void socketPlacementOutranksAnOpenDropdown() {
        assertEquals(EscapeAction.SOCKET_PLACEMENT, EscapeAction.resolve(CanvasMode.TREE, false, true, true, false));
        assertEquals(EscapeAction.SOCKET_PLACEMENT, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, false, true, true, true));
    }

    @Test
    void anOpenDropdownClosesBeforeTheTreeOrTheFleetViewDoesAnythingElse() {
        assertEquals(EscapeAction.CLOSE_DROPDOWN, EscapeAction.resolve(CanvasMode.TREE, false, false, true, false));
        assertEquals(EscapeAction.CLOSE_DROPDOWN, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, false, false, true, true));
    }

    @Test
    void followingTheFleetClosesTheFrameworkPanelBeforeStoppingTheFollow() {
        assertEquals(EscapeAction.CLOSE_FRAMEWORK_PANEL, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, false, false, false, true));
        assertEquals(EscapeAction.STOP_FOLLOWING, EscapeAction.resolve(CanvasMode.FLEET_FOLLOW, false, false, false, false));
    }

    @Test
    void anIdleTreePassesEscapeOnToTheSearchFieldAndThePanel() {
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.TREE, false, false, false, false));
        assertEquals(EscapeAction.NONE, EscapeAction.resolve(CanvasMode.TREE, false, false, false, true));
    }
}
