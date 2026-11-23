package exiledsector.compat;

import java.util.List;

public final class EmergentThreatsCompat {

    public static final String MOD_ID = "EmergentThreats_Vice";
    public static final List<String> TESTED_VERSIONS = List.of("1.4.0", "1.5.0");
    public static final List<String> HULL_MOD_IDS = List.of("vice_attuned_emitter_diodes", "vice_field_repair_nanites",
            "vice_modular_bolt_coherer", "vice_modular_fleet_override", "vice_unified_targeting_core",
            "vice_ai_subsystem_integration", "vice_adaptive_drive_field", "vice_adaptive_emitter_diodes",
            "vice_adaptive_flight_command", "vice_adaptive_neural_net", "vice_adaptive_phase_coils",
            "vice_adaptive_pulse_resonator", "vice_adaptive_reactor_chamber", "vice_adaptive_tactical_core",
            "vice_adaptive_temporal_shell", "vice_adaptive_thruster_control", "vice_adaptive_trajectory_analyzer",
            "vice_adaptive_entropy_arrester", "vice_adaptive_entropy_projector_abyssal", "vice_adaptive_flux_dissipator",
            "vice_adaptive_metastatic_growth");
    public static final CompatTarget TARGET = new CompatTarget("Emergent Threats", List.of(MOD_ID), TESTED_VERSIONS,
            () -> CompatTarget.missingHullMods(HULL_MOD_IDS));

    private EmergentThreatsCompat() {
    }
}
