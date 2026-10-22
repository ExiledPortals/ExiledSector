package exiledsector.compat;

import com.fs.starfarer.api.Global;
import exiledlib.compat.CompatTarget;

import java.util.List;

public final class TaniaCompat {

    public static final String MOD_ID = "navarchy_of_tania";
    public static final String FACTION_ID = "navarchy_of_tania";
    public static final List<String> TESTED_VERSIONS = List.of("1.3.0");
    public static final CompatTarget TARGET = new CompatTarget("Navarchy of Tania area", List.of(MOD_ID), TESTED_VERSIONS, TaniaCompat::missingFeatures);

    private TaniaCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    static List<String> missingFeatures() {
        return CompatTarget.specExists(() -> Global.getSettings().getFactionSpec(FACTION_ID)) ? List.of() : List.of("the faction " + FACTION_ID);
    }
}
