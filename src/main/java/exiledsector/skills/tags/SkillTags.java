package exiledsector.skills.tags;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class SkillTags {

    public static final List<String> THEME = List.of(
            "flux", "shield", "armour", "hull", "speed", "logistics", "missile", "fighter", "energy", "ballistic",
            "phase", "beam", "range", "ammo", "dmgtypekinetic", "dmgtypehighexplosive", "dmgtypefragmentation",
            "dmgtypeenergy", "weapons", "point_defense", "combat_readiness", "repair", "fleet_support", "d_mods");

    public static final String CORE_REGION = "core";
    public static final String ABYSS_REGION = "abyss";
    public static final String EXOTECH_REGION = "exotech";
    public static final String REDACTED_REGION = "REDACTED";
    public static final String THREAT_REGION = "threat";
    public static final Set<String> ABYSSAL_REGIONS = Set.of(ABYSS_REGION, THREAT_REGION);

    public static final List<String> FACTION_VOLUMES = List.of(
            "luddic", "tritachyon", "hegemony", "sindrian_dictat", "pirate", "REDACTED", "persean_league");

    public static final List<String> REGION = List.of(
            "core", "luddic", "tritachyon", "hegemony", "sindrian_dictat", "pirate", "REDACTED", "persean_league", "enigma", "kesteven", "exotech", "abyss",
            "nightcross", "stargazer", "threat", "tania");

    public static final List<String> HULL_REQUIREMENT = List.of("req_civilian_hull", "req_non_phase_hull", "req_system_charges", "req_modules");

    public static final List<String> REQUIREMENT = Stream.of(List.of(
            "req_shields", "req_no_shields", "req_phase", "req_fighter_bays", "req_no_fighter_bays", "req_ballistic",
            "req_missile", "req_energy", "req_beam", "req_offensive_beam", "req_non_beam_energy", "req_flagship",
            "campaign_only", "player_only"), HULL_REQUIREMENT).flatMap(List::stream).toList();

    public static final List<String> GATE = List.of("gate_singularity_defeated", "gate_pascal_visited", "gate_lunar_fleet_defeated",
            "gate_ancient_prototype_found", "gate_frozen_heart_visited", "gate_threat_fought", "gate_remnant_fought");

    public static final List<String> ALL = Stream.of(THEME, REGION, REQUIREMENT, GATE).flatMap(List::stream).toList();

    private SkillTags() {
    }

    public static boolean isRegion(String tag) {
        return tag != null && REGION.contains(tag);
    }

    public static boolean isRequirement(String tag) {
        return tag != null && REQUIREMENT.contains(tag);
    }

    public static boolean isHullRequirement(String tag) {
        return tag != null && HULL_REQUIREMENT.contains(tag);
    }
}
