package exiledsector.skills.tags;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import exiledsector.skills.ShipFacts;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public final class NodeRequirements {

    public static final String CREWED = "req_crewed";
    public static final String AUTOMATED = "req_automated";
    public static final String UNSCRIPTED_HULL = "req_unscripted_hull";

    public static final List<String> HULL_TAGS = List.of("req_civilian_hull", "req_non_phase_hull", "req_system_charges", "req_modules");

    public static final List<String> TAGS = Stream.of(List.of(
            "req_shields", "req_no_shields", "req_phase", "req_fighter_bays", "req_no_fighter_bays", "req_ballistic",
            "req_missile", "req_energy", "req_beam", "req_offensive_beam", "req_non_beam_energy", "req_flagship",
            CREWED, AUTOMATED, UNSCRIPTED_HULL, "campaign_only", "player_only"), HULL_TAGS).flatMap(List::stream).toList();

    private NodeRequirements() {
    }

    public static String firstUnmet(Collection<String> tags, ShipProfile profile) {
        for (String tag : tags) {
            if (isRequirement(tag) && !isHullRequirement(tag) && !isMet(tag, profile)) {
                return tag;
            }
        }
        return null;
    }

    public static boolean isRequirement(String tag) {
        return tag != null && TAGS.contains(tag);
    }

    public static boolean isHullRequirement(String tag) {
        return tag != null && HULL_TAGS.contains(tag);
    }

    public static boolean isSatisfiedBy(Collection<String> tags, ShipProfile profile) {
        return firstUnmet(tags, profile) == null;
    }

    public static String firstUnmetHullRequirement(Collection<String> tags, ShipFacts shipFacts) {
        for (String tag : tags) {
            if (isHullRequirement(tag) && !isHullRequirementMet(tag, shipFacts)) {
                return tag;
            }
        }
        return null;
    }

    private static boolean isHullRequirementMet(String requirement, ShipFacts shipFacts) {
        return switch (requirement) {
            case "req_civilian_hull" -> shipFacts.hasHullMod().test(HullMods.CIVGRADE);
            case "req_non_phase_hull" -> !shipFacts.phaseHull();
            case "req_system_charges" -> shipFacts.limitedSystemCharges();
            case "req_modules" -> shipFacts.hasModules();
            default -> false;
        };
    }

    private static boolean isMet(String requirement, ShipProfile profile) {
        ShieldType shieldType = profile.shieldType();
        return switch (requirement) {
            case "req_shields" -> shieldType == ShieldType.FRONT || shieldType == ShieldType.OMNI;
            case "req_no_shields" -> shieldType == ShieldType.NONE;
            case "req_phase" -> shieldType == ShieldType.PHASE;
            case "req_fighter_bays" -> profile.fighterBays() > 0;
            case "req_no_fighter_bays" -> profile.fighterBays() <= 0;
            case "req_ballistic" -> profile.weaponKinds().contains(WeaponKind.BALLISTIC);
            case "req_missile" -> profile.weaponKinds().contains(WeaponKind.MISSILE);
            case "req_energy" -> profile.weaponKinds().contains(WeaponKind.ENERGY);
            case "req_beam" -> profile.weaponKinds().contains(WeaponKind.BEAM);
            case "req_offensive_beam" -> profile.weaponKinds().contains(WeaponKind.OFFENSIVE_BEAM);
            case "req_non_beam_energy" -> profile.weaponKinds().contains(WeaponKind.NON_BEAM_ENERGY);
            case "req_flagship" -> profile.flagship();
            case CREWED -> !profile.automated();
            case AUTOMATED -> profile.automated();
            case UNSCRIPTED_HULL -> !profile.scriptedHull();
            default -> false;
        };
    }
}
