package exiledsector.skills.tags;

import exiledsector.skills.layout.TreeTags;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class SkillTags {

    public static final List<String> THEME = List.of(
            "flux", "shield", "armour", "hull", "speed", "logistics", "missile", "fighter", "energy", "ballistic",
            "phase", "beam", "range", "ammo", "dmgtypekinetic", "dmgtypehighexplosive", "dmgtypefragmentation",
            "dmgtypeenergy", "weapons", "point_defense", "combat_readiness", "repair", "fleet_support", "d_mods");

    public static final String CORE_VOLUME = "core";
    public static final String ABYSS_VOLUME = "abyss";
    public static final String EXOTECH_VOLUME = "exotech";
    public static final String REDACTED_VOLUME = "REDACTED";
    public static final String OMEGA_VOLUME = "omega";
    public static final String THREAT_VOLUME = "threat";
    public static final String SHROUDED_VOLUME = "shrouded";
    public static final Set<String> ABYSSAL_VOLUMES = Set.of(ABYSS_VOLUME, THREAT_VOLUME, SHROUDED_VOLUME);
    public static final Set<String> NO_EXOSHIP_ANCHOR_VOLUMES = Set.of(ABYSS_VOLUME, THREAT_VOLUME, SHROUDED_VOLUME, REDACTED_VOLUME, OMEGA_VOLUME);
    public static final Set<String> JANUS_EXEMPT_VOLUMES = Set.of(EXOTECH_VOLUME, OMEGA_VOLUME);

    public static final List<String> FACTION_VOLUMES = List.of(
            "luddic", "tritachyon", "hegemony", "sindrian_dictat", "pirate", REDACTED_VOLUME, OMEGA_VOLUME, "persean_league");

    public static final List<String> VOLUME = List.of(
            CORE_VOLUME, "luddic", "tritachyon", "hegemony", "sindrian_dictat", "pirate", REDACTED_VOLUME, OMEGA_VOLUME, "persean_league", "enigma", "kesteven",
            EXOTECH_VOLUME, ABYSS_VOLUME, "nightcross", "stargazer", THREAT_VOLUME, "tania", SHROUDED_VOLUME);

    public static final List<String> HULL_REQUIREMENT = NodeRequirements.HULL_TAGS;

    public static final List<String> REQUIREMENT = NodeRequirements.TAGS;

    public static final List<String> GATE = List.of("gate_singularity_defeated", "gate_pascal_visited", "gate_existential_threat_defeated",
            "gate_ancient_prototype_found", "gate_frozen_heart_visited", "gate_threat_fought", "gate_remnant_fought", "gate_dweller_fought", "gate_omega_found",
            "gate_blood_trail_defeated", "gate_frost_fire_defeated", "gate_last_strive_completed");

    public static final List<String> RULE = List.of("logistics_cap");

    public static final List<String> ALL = Stream.of(THEME, VOLUME, REQUIREMENT, GATE, RULE).flatMap(List::stream).toList();

    private SkillTags() {
    }

    public static TreeTags treeTags(List<String> tags) {
        return TreeTags.of(tags, SkillTags::isVolume);
    }

    public static boolean isVolume(String tag) {
        return tag != null && VOLUME.contains(tag);
    }

    public static boolean isRequirement(String tag) {
        return NodeRequirements.isRequirement(tag);
    }

    public static boolean isHullRequirement(String tag) {
        return NodeRequirements.isHullRequirement(tag);
    }
}
