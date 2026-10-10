package exiledsector.ui;

enum CanvasMode {
    ROOT_CHOICE, MODAL, WORKBENCH, HYPERSPACE, FLEET_FOLLOW, ALLOCATION_RUN, TREE;

    enum Chrome { STATS_TOGGLE, READOUTS, SEARCH, TEMPLATE_BAR, STORAGE_BUTTON }

    static CanvasMode resolve(boolean rootChoiceLocked, boolean modalBlocking, boolean workbenchOpen, boolean hyperspaceActive,
                              boolean followingFleet, boolean allocationRunning) {
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
        if (followingFleet) {
            return FLEET_FOLLOW;
        }
        return allocationRunning ? ALLOCATION_RUN : TREE;
    }

    boolean shows(Chrome chrome) {
        return switch (chrome) {
            case READOUTS -> true;
            case SEARCH -> this != HYPERSPACE && this != ROOT_CHOICE && this != FLEET_FOLLOW;
            case STATS_TOGGLE -> this != HYPERSPACE;
            case TEMPLATE_BAR -> this != HYPERSPACE && this != FLEET_FOLLOW;
            case STORAGE_BUTTON -> this != HYPERSPACE;
        };
    }

    boolean enables(Chrome chrome) {
        return switch (this) {
            case TREE, ALLOCATION_RUN -> true;
            case ROOT_CHOICE -> chrome == Chrome.STATS_TOGGLE || chrome == Chrome.READOUTS;
            case HYPERSPACE -> chrome == Chrome.READOUTS;
            case FLEET_FOLLOW -> chrome != Chrome.TEMPLATE_BAR && chrome != Chrome.SEARCH;
            case WORKBENCH -> chrome == Chrome.STORAGE_BUTTON;
            case MODAL -> false;
        };
    }

    boolean showsChromeTooltips() {
        return this != MODAL && this != WORKBENCH;
    }

    boolean hoversTree() {
        return this == TREE || this == ALLOCATION_RUN || this == ROOT_CHOICE || this == FLEET_FOLLOW;
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
        return this == TREE || this == ALLOCATION_RUN || this == FLEET_FOLLOW;
    }
}
