package exiledsector.ui;

enum CanvasMode {
    ROOT_CHOICE, MODAL, WORKBENCH, HYPERSPACE, EXOSHIP_VIEW, FLEET_FOLLOW, ALLOCATION_RUN, TREE;

    enum Chrome { STATS_TOGGLE, READOUTS, SEARCH, TEMPLATE_BAR, STORAGE_BUTTON }

    static CanvasMode resolve(boolean rootChoiceLocked, boolean modalBlocking, boolean workbenchOpen, boolean hyperspaceActive,
                              boolean viewingExoship, boolean followingFleet, boolean allocationRunning) {
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
        if (viewingExoship) {
            return EXOSHIP_VIEW;
        }
        if (followingFleet) {
            return FLEET_FOLLOW;
        }
        return allocationRunning ? ALLOCATION_RUN : TREE;
    }

    boolean shows(Chrome chrome) {
        return switch (chrome) {
            case READOUTS -> true;
            case SEARCH -> this != HYPERSPACE && this != ROOT_CHOICE && !followsTarget();
            case STATS_TOGGLE -> this != HYPERSPACE;
            case TEMPLATE_BAR -> this != HYPERSPACE && !followsTarget();
            case STORAGE_BUTTON -> this != HYPERSPACE && this != ROOT_CHOICE;
        };
    }

    boolean followsTarget() {
        return this == FLEET_FOLLOW || this == EXOSHIP_VIEW;
    }

    boolean hidesChromeThroughTransition() {
        return this == HYPERSPACE;
    }

    boolean enables(Chrome chrome) {
        return switch (this) {
            case TREE, ALLOCATION_RUN -> true;
            case ROOT_CHOICE -> chrome == Chrome.STATS_TOGGLE || chrome == Chrome.READOUTS;
            case HYPERSPACE -> chrome == Chrome.READOUTS;
            case FLEET_FOLLOW, EXOSHIP_VIEW -> chrome != Chrome.TEMPLATE_BAR && chrome != Chrome.SEARCH;
            case WORKBENCH -> chrome == Chrome.STORAGE_BUTTON;
            case MODAL -> false;
        };
    }

    boolean showsChromeTooltips() {
        return this != MODAL && this != WORKBENCH;
    }

    boolean hoversTree() {
        return this == TREE || this == ALLOCATION_RUN || this == ROOT_CHOICE || followsTarget();
    }

    boolean closesStorage() {
        return this == ROOT_CHOICE || this == MODAL || this == HYPERSPACE;
    }

    boolean entersHyperspaceOnScrollOut() {
        return this == TREE || this == FLEET_FOLLOW;
    }

    boolean pansOnDrag() {
        return this != FLEET_FOLLOW;
    }

    boolean letsShipCardFollowFleet() {
        return this == TREE || this == ALLOCATION_RUN || followsTarget();
    }
}
