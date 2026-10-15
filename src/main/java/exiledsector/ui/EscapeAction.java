package exiledsector.ui;

enum EscapeAction {
    NONE, SOCKET_PLACEMENT, CANCEL_ALLOCATION, CLOSE_DROPDOWN, CLOSE_FRAMEWORK_PANEL, STOP_FOLLOWING, EXIT_TEMPLATE_MODE;

    static EscapeAction resolve(CanvasMode mode, boolean allocationRunning, boolean socketEngaged, boolean dropdownOpen,
                                boolean frameworkPanelOpen, boolean planning) {
        return switch (mode) {
            case MODAL, HYPERSPACE, ROOT_CHOICE -> NONE;
            case WORKBENCH -> SOCKET_PLACEMENT;
            case TREE, ALLOCATION_RUN, FLEET_FOLLOW -> {
                if (allocationRunning) {
                    yield CANCEL_ALLOCATION;
                }
                if (socketEngaged) {
                    yield SOCKET_PLACEMENT;
                }
                if (dropdownOpen) {
                    yield CLOSE_DROPDOWN;
                }
                if (mode == CanvasMode.FLEET_FOLLOW) {
                    yield frameworkPanelOpen ? CLOSE_FRAMEWORK_PANEL : STOP_FOLLOWING;
                }
                yield planning ? EXIT_TEMPLATE_MODE : NONE;
            }
        };
    }
}
