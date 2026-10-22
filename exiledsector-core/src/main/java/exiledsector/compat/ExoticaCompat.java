package exiledsector.compat;

import com.fs.starfarer.api.Global;
import exiledlib.compat.CompatTarget;

import java.util.List;

public final class ExoticaCompat {

    public static final String MOD_ID = "exoticatechnologies";
    public static final List<String> TESTED_VERSIONS = List.of("1.8.7");
    static final String REFIT_BUTTON_CLASS = "exoticatechnologies.refit.RefitButtonAdder";
    public static final CompatTarget TARGET = new CompatTarget("Exotica Technologies", List.of(MOD_ID), TESTED_VERSIONS,
            ExoticaCompat::missingFeatures);

    private ExoticaCompat() {
    }

    public static boolean isModEnabled() {
        return TARGET.isEnabled();
    }

    static List<String> missingFeatures() {
        try {
            Class.forName(REFIT_BUTTON_CLASS, false, Global.getSettings().getScriptClassLoader());
            return List.of();
        } catch (ClassNotFoundException | LinkageError e) {
            return List.of("its refit Exotica button (" + REFIT_BUTTON_CLASS + "), which the Skill Tree button makes room for under Build In");
        }
    }
}
