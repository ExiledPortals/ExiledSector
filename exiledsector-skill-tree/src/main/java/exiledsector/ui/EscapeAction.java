package exiledsector.ui;

enum EscapeAction {
    NONE, SOCKET_PLACEMENT, CLOSE_CHOICE_PICKER, STOP_FOLLOWING, EXIT_TEMPLATE_MODE;

    static EscapeAction resolve(CanvasMode mode, boolean socketEngaged, boolean choicePickerOpen, boolean planning) {
        return switch (mode) {
            case MODAL, HYPERSPACE, ROOT_CHOICE -> NONE;
            case CRAFTING_BENCH -> SOCKET_PLACEMENT;
            case TREE, EXOSHIP_VIEW -> {
                if (socketEngaged) {
                    yield SOCKET_PLACEMENT;
                }
                if (choicePickerOpen) {
                    yield CLOSE_CHOICE_PICKER;
                }
                if (mode == CanvasMode.EXOSHIP_VIEW) {
                    yield STOP_FOLLOWING;
                }
                yield planning ? EXIT_TEMPLATE_MODE : NONE;
            }
        };
    }
}
