package exiledsector.compat;

import java.util.List;

public final class RandomAssortmentCompat {

    public static final String MOD_ID = "assortment_of_things";
    public static final List<String> TESTED_VERSIONS = List.of("3.3.1");
    public static final List<String> REQUIRED_HULL_MOD_IDS = List.of("rat_exogrid", "rat_hypatia_hullmod", "rat_abyssal_grid",
            "rat_abyssal_core", "rat_seraphs_grace", "rat_raphaels_grace");
    public static final CompatTarget TARGET = new CompatTarget("Random Assortment of Things areas", List.of(MOD_ID), TESTED_VERSIONS,
            () -> CompatTarget.missingHullMods(REQUIRED_HULL_MOD_IDS));

    private RandomAssortmentCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }
}
