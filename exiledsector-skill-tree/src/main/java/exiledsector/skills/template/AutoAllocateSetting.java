package exiledsector.skills.template;

import exiledsector.ModSettings;

public final class AutoAllocateSetting {

    public static final String FREE_ALLOCATIONS_ONLY_FIELD_ID = "exiledSector_autoAllocateFreeAllocationsOnly";

    public static final boolean DEFAULT_FREE_ALLOCATIONS_ONLY = false;

    private static volatile boolean freeAllocationsOnly = DEFAULT_FREE_ALLOCATIONS_ONLY;

    private AutoAllocateSetting() {
    }

    public static void reloadSettings() {
        freeAllocationsOnly = ModSettings.booleanOr(FREE_ALLOCATIONS_ONLY_FIELD_ID, DEFAULT_FREE_ALLOCATIONS_ONLY);
    }

    public static boolean freeAllocationsOnly() {
        return freeAllocationsOnly;
    }
}
