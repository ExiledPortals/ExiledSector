package exiledsector.ui;

enum CanvasMode {
    ROOT_CHOICE, MODAL, CRAFTING_BENCH, HYPERSPACE, EXOSHIP_VIEW, TREE;

    enum Chrome { STATS_TOGGLE, READOUTS, SEARCH, TEMPLATE_BAR, STORAGE_BUTTON, LOGISTICS_BUTTON }

    static CanvasMode resolve(boolean rootChoiceLocked, boolean modalBlocking, boolean craftingBenchOpen, boolean hyperspaceActive,
                              boolean viewingExoship) {
        if (rootChoiceLocked) {
            return ROOT_CHOICE;
        }
        if (modalBlocking) {
            return MODAL;
        }
        if (craftingBenchOpen) {
            return CRAFTING_BENCH;
        }
        if (hyperspaceActive) {
            return HYPERSPACE;
        }
        if (viewingExoship) {
            return EXOSHIP_VIEW;
        }
        return TREE;
    }

    boolean shows(Chrome chrome) {
        return switch (chrome) {
            case READOUTS -> true;
            case SEARCH -> this != HYPERSPACE && this != ROOT_CHOICE && this != EXOSHIP_VIEW;
            case STATS_TOGGLE -> this != HYPERSPACE;
            case TEMPLATE_BAR -> this != HYPERSPACE && this != EXOSHIP_VIEW;
            case STORAGE_BUTTON, LOGISTICS_BUTTON -> this != HYPERSPACE && this != ROOT_CHOICE;
        };
    }

    boolean hidesChromeThroughTransition() {
        return this == HYPERSPACE;
    }

    boolean enables(Chrome chrome) {
        return switch (this) {
            case TREE -> true;
            case ROOT_CHOICE -> chrome == Chrome.STATS_TOGGLE || chrome == Chrome.READOUTS;
            case HYPERSPACE -> chrome == Chrome.READOUTS;
            case EXOSHIP_VIEW -> chrome != Chrome.TEMPLATE_BAR && chrome != Chrome.SEARCH;
            case CRAFTING_BENCH -> chrome == Chrome.STORAGE_BUTTON;
            case MODAL -> false;
        };
    }

    boolean showsChromeTooltips() {
        return this != MODAL && this != CRAFTING_BENCH;
    }

    boolean hoversTree() {
        return this == TREE || this == ROOT_CHOICE || this == EXOSHIP_VIEW;
    }

    boolean switchesShips() {
        return this == TREE || this == ROOT_CHOICE || this == EXOSHIP_VIEW;
    }

    boolean closesStorage() {
        return this == ROOT_CHOICE || this == MODAL || this == HYPERSPACE;
    }

    boolean entersHyperspaceOnScrollOut() {
        return this == TREE;
    }
}
