package exiledsector.compat;

import com.fs.starfarer.api.Global;

import java.util.ArrayList;
import java.util.List;

public final class NightcrossCompat {

    public static final String MOD_ID = "Nightcross";
    public static final List<String> TESTED_VERSIONS = List.of("2.1.5", "2.1.6");
    public static final List<String> REQUIRED_HULL_MOD_IDS = List.of("nightcrosstargeting", "na_gravitydrag");
    public static final CompatTarget TARGET = new CompatTarget("Nightcross Armory area", List.of(MOD_ID), TESTED_VERSIONS,
            NightcrossCompat::missingFeatures);

    private NightcrossCompat() {
    }

    static List<String> missingFeatures() {
        List<String> missing = new ArrayList<>(CompatTarget.missingHullMods(REQUIRED_HULL_MOD_IDS));
        for (String hullId : NightcrossUnlocks.PROTOTYPE_HULL_IDS.stream().sorted().toList()) {
            if (!CompatTarget.specExists(() -> Global.getSettings().getHullSpec(hullId))) {
                missing.add("the Ancient Prototype hull " + hullId + ", used to unlock Candle's Grace Replica");
            }
        }
        return missing;
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }
}
