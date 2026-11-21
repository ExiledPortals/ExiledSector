package exiledsector.compat;

import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.List;

public final class SymbioticVoidCreaturesCompat {

    public static final String MOD_ID = "symbiotic_void_creatures";
    public static final List<String> TESTED_VERSIONS = List.of("0.6.2a");
    public static final String CARAPACE_HULL_MOD_ID = "BGECarapace";
    public static final CompatTarget TARGET = new CompatTarget("Symbiotic Void Creatures", List.of(MOD_ID), TESTED_VERSIONS,
            SymbioticVoidCreaturesCompat::missingFeatures);

    private SymbioticVoidCreaturesCompat() {
    }

    public static boolean stripsOtherHullMods(ShipVariantAPI variant) {
        return variant != null && variant.hasHullMod(CARAPACE_HULL_MOD_ID);
    }

    static List<String> missingFeatures() {
        return CompatTarget.missingHullMods(List.of(CARAPACE_HULL_MOD_ID));
    }
}
