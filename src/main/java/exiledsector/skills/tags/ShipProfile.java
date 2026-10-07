package exiledsector.skills.tags;

import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSystemCharges;

import java.util.EnumSet;
import java.util.Set;

public record ShipProfile(HullSize hullSize, ShieldAPI.ShieldType shieldType, int fighterBays,
                          Set<WeaponKind> weaponKinds, boolean flagship, float baseArmor, boolean phaseHull,
                          boolean limitedSystemCharges, boolean onlyBuiltInWings) {

    public ShipProfile {
        weaponKinds = weaponKinds == null ? Set.of() : Set.copyOf(weaponKinds);
    }

    public ShipProfile(HullSize hullSize, ShieldAPI.ShieldType shieldType, int fighterBays, Set<WeaponKind> weaponKinds,
                       boolean flagship, float baseArmor, boolean phaseHull) {
        this(hullSize, shieldType, fighterBays, weaponKinds, flagship, baseArmor, phaseHull, false);
    }

    public ShipProfile(HullSize hullSize, ShieldAPI.ShieldType shieldType, int fighterBays, Set<WeaponKind> weaponKinds,
                       boolean flagship, float baseArmor, boolean phaseHull, boolean limitedSystemCharges) {
        this(hullSize, shieldType, fighterBays, weaponKinds, flagship, baseArmor, phaseHull, limitedSystemCharges, false);
    }

    public static ShipProfile of(FleetMemberAPI member) {
        ShipHullSpecAPI hullSpec = member.getHullSpec();
        return new ShipProfile(hullSpec.getHullSize(), hullSpec.getShieldType(), fighterBays(member, hullSpec),
                fittedWeaponKinds(member.getVariant()), member.isFlagship(), hullSpec.getArmorRating(),
                hullSpec.isPhase(), ShipSystemCharges.limited(hullSpec), ShipFacts.onlyBuiltInWings(hullSpec));
    }

    private static int fighterBays(FleetMemberAPI member, ShipHullSpecAPI hullSpec) {
        return member.getStats() != null ? member.getNumFlightDecks() : hullSpec.getFighterBays();
    }

    private static Set<WeaponKind> fittedWeaponKinds(ShipVariantAPI variant) {
        Set<WeaponKind> fittedKinds = EnumSet.noneOf(WeaponKind.class);
        if (variant == null) {
            return fittedKinds;
        }
        for (String slotId : variant.getFittedWeaponSlots()) {
            WeaponSpecAPI weaponSpec = variant.getWeaponSpec(slotId);
            if (weaponSpec != null) {
                addKinds(fittedKinds, weaponSpec);
            }
        }
        return fittedKinds;
    }

    private static void addKinds(Set<WeaponKind> fittedKinds, WeaponSpecAPI weaponSpec) {
        WeaponType weaponType = weaponSpec.getType();
        if (weaponType != WeaponType.BALLISTIC && weaponType != WeaponType.MISSILE && weaponType != WeaponType.ENERGY) {
            return;
        }
        boolean isBeam = weaponSpec.isBeam();
        if (weaponType == WeaponType.BALLISTIC) {
            fittedKinds.add(WeaponKind.BALLISTIC);
        } else if (weaponType == WeaponType.MISSILE) {
            fittedKinds.add(WeaponKind.MISSILE);
        } else {
            fittedKinds.add(WeaponKind.ENERGY);
            if (!isBeam) {
                fittedKinds.add(WeaponKind.NON_BEAM_ENERGY);
            }
        }
        if (isBeam) {
            fittedKinds.add(WeaponKind.BEAM);
            if (!isPointDefense(weaponSpec)) {
                fittedKinds.add(WeaponKind.OFFENSIVE_BEAM);
            }
        }
    }

    private static boolean isPointDefense(WeaponSpecAPI weaponSpec) {
        Set<AIHints> aiHints = weaponSpec.getAIHints();
        return aiHints != null && (aiHints.contains(AIHints.PD) || aiHints.contains(AIHints.PD_ONLY));
    }
}
