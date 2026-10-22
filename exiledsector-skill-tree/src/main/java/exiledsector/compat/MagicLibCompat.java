package exiledsector.compat;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

public final class MagicLibCompat {

    public static final String MOD_ID = "MagicLib";
    public static final String WARNING_HULLMOD_ID = "ML_incompatibleHullmodWarning";
    public static final List<String> TESTED_VERSIONS = List.of("1.5.6");
    public static final CompatTarget TARGET = new CompatTarget("MagicLib", List.of(MOD_ID), TESTED_VERSIONS,
            MagicLibCompat::missingFeatures);

    private MagicLibCompat() {
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>(CompatTarget.missingHullMods(List.of(WARNING_HULLMOD_ID)));
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            lookup.findStatic(MagicIncompatibleHullmods.class, "getReason", MethodType.methodType(List.class, ShipVariantAPI.class));
            lookup.findStatic(MagicIncompatibleHullmods.class, "removeHullmodWithWarning",
                    MethodType.methodType(void.class, ShipVariantAPI.class, String.class, String.class));
        } catch (ReflectiveOperationException | LinkageError e) {
            missing.add("MagicIncompatibleHullmods.getReason and removeHullmodWithWarning, used to resolve hull mod conflicts");
        }
        return missing;
    }
}
