package exiledsector.compat;

import java.util.List;

public final class EmergentThreatsCompat {

    public static final String MOD_ID = "EmergentThreats_Vice";
    public static final List<String> TESTED_VERSIONS = List.of("1.4.0", "1.5.0");
    public static final List<String> HULL_MOD_IDS = List.of("vice_attuned_emitter_diodes", "vice_field_repair_nanites",
            "vice_modular_bolt_coherer", "vice_modular_fleet_override", "vice_unified_targeting_core");
    public static final CompatTarget TARGET = new CompatTarget("Emergent Threats", List.of(MOD_ID), TESTED_VERSIONS,
            () -> CompatTarget.missingHullMods(HULL_MOD_IDS));

    private EmergentThreatsCompat() {
    }
}
