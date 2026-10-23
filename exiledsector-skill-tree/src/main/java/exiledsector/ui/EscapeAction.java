package exiledsector.ui;

enum EscapeAction {
    NONE, SOCKET_PLACEMENT, CLOSE_DROPDOWN, STOP_FOLLOWING, EXIT_TEMPLATE_MODE;

    static EscapeAction resolve(CanvasMode mode, boolean socketEngaged, boolean dropdownOpen, boolean planning) {
        return switch (mode) {
            case MODAL, HYPERSPACE, ROOT_CHOICE -> NONE;
            case WORKBENCH -> SOCKET_PLACEMENT;
            case TREE, EXOSHIP_VIEW -> {
                if (socketEngaged) {
                    yield SOCKET_PLACEMENT;
                }
                if (dropdownOpen) {
                    yield CLOSE_DROPDOWN;
                }
                if (mode == CanvasMode.EXOSHIP_VIEW) {
                    yield STOP_FOLLOWING;
                }
                yield planning ? EXIT_TEMPLATE_MODE : NONE;
            }
        };
    }
}
