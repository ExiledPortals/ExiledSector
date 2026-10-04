package exiledsector.skills.tags;

import java.util.List;
import java.util.stream.Stream;

public final class SkillTags {

    public static final List<String> THEME = List.of(
            "flux", "shield", "armour", "hull", "speed", "logistics", "missile", "fighter", "energy", "ballistic",
            "phase", "beam", "range", "ammo", "dmgtypekinetic", "dmgtypehighexplosive", "dmgtypefragmentation",
            "dmgtypeenergy", "weapons", "point_defense", "combat_readiness", "repair", "fleet_support", "d_mods");

    public static final List<String> REGION = List.of(
            "core", "luddic", "tritachyon", "hegemony", "sindrian_dictat", "pirate", "REDACTED", "persean_league", "enigma", "kesteven");

    public static final List<String> HULL_REQUIREMENT = List.of("req_civilian_hull", "req_non_phase_hull", "req_system_charges");

    public static final List<String> REQUIREMENT = Stream.of(List.of(
            "req_shields", "req_no_shields", "req_phase", "req_fighter_bays", "req_no_fighter_bays", "req_ballistic",
            "req_missile", "req_energy", "req_beam", "req_offensive_beam", "req_non_beam_energy", "req_flagship",
            "campaign_only", "player_only"), HULL_REQUIREMENT).flatMap(List::stream).toList();

    public static final List<String> ALL = Stream.of(THEME, REGION, REQUIREMENT).flatMap(List::stream).toList();

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
