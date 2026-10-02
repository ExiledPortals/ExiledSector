package exiledsector.skills.tags;

import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.skills.ShipSystemCharges;

import java.util.EnumSet;
import java.util.Set;

public record ShipProfile(HullSize hullSize, ShieldAPI.ShieldType shieldType, int fighterBays,
                          Set<WeaponKind> weaponKinds, boolean flagship, float baseArmor, boolean phaseHull,
                          boolean limitedSystemCharges) {

    public ShipProfile {
        weaponKinds = weaponKinds == null ? Set.of() : Set.copyOf(weaponKinds);
    }

    public ShipProfile(HullSize hullSize, ShieldAPI.ShieldType shieldType, int fighterBays, Set<WeaponKind> weaponKinds,
                       boolean flagship, float baseArmor, boolean phaseHull) {
        this(hullSize, shieldType, fighterBays, weaponKinds, flagship, baseArmor, phaseHull, false);
    }

    public static ShipProfile of(FleetMemberAPI member) {
        ShipHullSpecAPI hullSpec = member.getHullSpec();
        return new ShipProfile(hullSpec.getHullSize(), hullSpec.getShieldType(), fighterBays(member, hullSpec),
                fittedWeaponKinds(member.getVariant()), member.isFlagship(), hullSpec.getArmorRating(),
                hullSpec.isPhase(), ShipSystemCharges.limited(hullSpec));
    }

    private static int fighterBays(FleetMemberAPI member, ShipHullSpecAPI hullSpec) {
        return member.getStats() != null ? member.getNumFlightDecks() : hullSpec.getFighterBays();
    }

    private static Set<WeaponKind> fittedWeaponKinds(ShipVariantAPI variant) {
        Set<WeaponKind> kinds = EnumSet.noneOf(WeaponKind.class);
        if (variant == null) {
            return kinds;
        }
        for (String slotId : variant.getFittedWeaponSlots()) {
            WeaponSpecAPI spec = variant.getWeaponSpec(slotId);
            if (spec != null) {
                addKinds(kinds, spec);
            }
        }
        return kinds;
    }

    private static void addKinds(Set<WeaponKind> kinds, WeaponSpecAPI spec) {
        WeaponType type = spec.getType();
        if (type != WeaponType.BALLISTIC && type != WeaponType.MISSILE && type != WeaponType.ENERGY) {
            return;
        }
        boolean beam = spec.isBeam();
        if (type == WeaponType.BALLISTIC) {
            kinds.add(WeaponKind.BALLISTIC);
        } else if (type == WeaponType.MISSILE) {
            kinds.add(WeaponKind.MISSILE);
        } else {
            kinds.add(WeaponKind.ENERGY);
            if (!beam) {
                kinds.add(WeaponKind.NON_BEAM_ENERGY);
            }
        }
        if (beam) {
            kinds.add(WeaponKind.BEAM);
            if (!isPointDefense(spec)) {
                kinds.add(WeaponKind.OFFENSIVE_BEAM);
            }
        }
    }

    private static boolean isPointDefense(WeaponSpecAPI spec) {
        Set<AIHints> hints = spec.getAIHints();
        return hints != null && (hints.contains(AIHints.PD) || hints.contains(AIHints.PD_ONLY));
    }
}
