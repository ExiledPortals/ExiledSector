package exiledsector.ui;

enum CanvasMode {
    ROOT_CHOICE, MODAL, WORKBENCH, HYPERSPACE, ALLOCATION_RUN, TREE;

    enum Chrome { STATS_TOGGLE, READOUTS, SEARCH, TEMPLATE_BAR, STORAGE_BUTTON }

    static CanvasMode resolve(boolean rootChoiceLocked, boolean modalBlocking, boolean workbenchOpen, boolean hyperspaceActive,
                              boolean allocationRunning) {
        if (rootChoiceLocked) {
            return ROOT_CHOICE;
        }
        if (modalBlocking) {
            return MODAL;
        }
        if (workbenchOpen) {
            return WORKBENCH;
        }
        if (hyperspaceActive) {
            return HYPERSPACE;
        }
        return allocationRunning ? ALLOCATION_RUN : TREE;
    }

    boolean shows(Chrome chrome) {
        return switch (chrome) {
            case READOUTS -> true;
            case SEARCH -> this != HYPERSPACE && this != ROOT_CHOICE;
            case STATS_TOGGLE, TEMPLATE_BAR, STORAGE_BUTTON -> this != HYPERSPACE;
        };
    }

    boolean enables(Chrome chrome) {
        return switch (this) {
            case TREE, ALLOCATION_RUN -> true;
            case ROOT_CHOICE -> chrome == Chrome.STATS_TOGGLE || chrome == Chrome.READOUTS;
            case HYPERSPACE -> chrome == Chrome.READOUTS;
            case WORKBENCH -> chrome == Chrome.STORAGE_BUTTON;
            case MODAL -> false;
        };
    }

    boolean showsChromeTooltips() {
        return this != MODAL && this != WORKBENCH;
    }

    boolean hoversTree() {
        return this == TREE || this == ALLOCATION_RUN || this == ROOT_CHOICE;
    }

    boolean closesStorage() {
        return this == ROOT_CHOICE || this == MODAL || this == HYPERSPACE;
    }

    boolean entersHyperspaceOnScrollOut() {
        return this == TREE;
    }
}
