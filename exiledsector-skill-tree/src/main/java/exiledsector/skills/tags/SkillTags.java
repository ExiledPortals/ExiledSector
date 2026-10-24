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

    public static final List<String> HULL_REQUIREMENT = NodeRequirements.HULL_TAGS;

    public static final List<String> REQUIREMENT = NodeRequirements.TAGS;

    public static final List<String> GATE = List.of("gate_singularity_defeated", "gate_pascal_visited", "gate_existential_threat_defeated",
            "gate_ancient_prototype_found", "gate_frozen_heart_visited", "gate_threat_fought", "gate_remnant_fought",
            "gate_blood_trail_defeated", "gate_frost_fire_defeated", "gate_last_strive_completed");

    public static final List<String> ALL = Stream.of(THEME, REGION, REQUIREMENT, GATE).flatMap(List::stream).toList();

    private SkillTags() {
    }

    public static boolean isRegion(String tag) {
        return tag != null && REGION.contains(tag);
    }

    public static boolean isRequirement(String tag) {
        return NodeRequirements.isRequirement(tag);
    }

    public static boolean isHullRequirement(String tag) {
        return NodeRequirements.isHullRequirement(tag);
    }
}
