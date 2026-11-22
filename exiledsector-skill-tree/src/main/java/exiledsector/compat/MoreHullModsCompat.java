package exiledsector.compat;

import java.util.List;

public final class MoreHullModsCompat {

    public static final String MOD_ID = "more_hullmods";
    public static final List<String> TESTED_VERSIONS = List.of("1.13.0");
    public static final List<String> HULL_MOD_IDS = List.of("mhmods_antiarmorgammunition", "mhmods_bombardmentpackage",
            "mhmods_emergencyventingsystem", "mhmods_explorationrefit", "mhmods_fluxbooster", "mhmods_fluxgenerator",
            "mhmods_fluxoverdrive", "mhmods_FuelAdditive", "mhmods_hardlightFocus", "mhmods_hullfoam", "mhmods_hyperengineupgrade",
            "mhmods_integratedarmor", "MHMods_prefabricator", "mhmods_particleaccelerator", "mhmods_reloader", "mhmods_splitChamber",
            "mhmods_stiffMounts", "mhmods_torpedospec", "mhmods_unstableshields", "mhmods_voltageregulationsystem",
            "mhmods_weaponinhibitor");
    public static final CompatTarget TARGET = new CompatTarget("More HullMods", List.of(MOD_ID), TESTED_VERSIONS,
            () -> CompatTarget.missingHullMods(HULL_MOD_IDS));

    private MoreHullModsCompat() {
    }
}
