package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.skills.tags.ShipProfile;

import java.util.function.Predicate;

public record ShipFacts(HullSize hullSize, ShieldType hullShieldType, boolean phaseHull, float baseArmor,
                        boolean limitedSystemCharges, Predicate<String> hasHullMod, boolean onlyBuiltInWings, String hullId,
                        Predicate<String> hasFixedHullMod) {

    public ShipFacts(HullSize hullSize, ShieldType hullShieldType, boolean phaseHull, float baseArmor,
                     boolean limitedSystemCharges, Predicate<String> hasHullMod, boolean onlyBuiltInWings, String hullId) {
        this(hullSize, hullShieldType, phaseHull, baseArmor, limitedSystemCharges, hasHullMod, onlyBuiltInWings, hullId, hasHullMod);
    }

    public ShipFacts(HullSize hullSize, ShieldType hullShieldType, boolean phaseHull, float baseArmor,
                     boolean limitedSystemCharges, Predicate<String> hasHullMod, boolean onlyBuiltInWings) {
        this(hullSize, hullShieldType, phaseHull, baseArmor, limitedSystemCharges, hasHullMod, onlyBuiltInWings, null);
    }

    public ShipFacts(HullSize hullSize, ShieldType hullShieldType, boolean phaseHull, float baseArmor,
                     Predicate<String> hasHullMod) {
        this(hullSize, hullShieldType, phaseHull, baseArmor, false, hasHullMod);
    }

    public ShipFacts(HullSize hullSize, ShieldType hullShieldType, boolean phaseHull, float baseArmor,
                     boolean limitedSystemCharges, Predicate<String> hasHullMod) {
        this(hullSize, hullShieldType, phaseHull, baseArmor, limitedSystemCharges, hasHullMod, false);
    }

    public static ShipFacts of(ShipHullSpecAPI hullSpec, Predicate<String> hasHullMod) {
        return of(hullSpec, hasHullMod, hasHullMod);
    }

    public static ShipFacts of(ShipHullSpecAPI hullSpec, Predicate<String> hasHullMod, Predicate<String> hasFixedHullMod) {
        return new ShipFacts(hullSpec.getHullSize(), hullSpec.getShieldType(), hullSpec.isPhase(), hullSpec.getArmorRating(),
                ShipSystemCharges.limited(hullSpec), hasHullMod, onlyBuiltInWings(hullSpec), hullSpec.getHullId(), hasFixedHullMod);
    }

    public static ShipFacts of(ShipProfile profile, Predicate<String> hasHullMod) {
        return of(profile, hasHullMod, null);
    }

    public static ShipFacts of(ShipProfile profile, Predicate<String> hasHullMod, String hullId) {
        return of(profile, hasHullMod, hullId, hasHullMod);
    }

    public static ShipFacts of(ShipProfile profile, Predicate<String> hasHullMod, String hullId, Predicate<String> hasFixedHullMod) {
        return new ShipFacts(profile.hullSize(), profile.shieldType(), profile.phaseHull(),
                profile.baseArmor(), profile.limitedSystemCharges(), hasHullMod, profile.onlyBuiltInWings(), hullId, hasFixedHullMod);
    }

    public static boolean onlyBuiltInWings(ShipHullSpecAPI hullSpec) {
        int builtInWings = hullSpec.getBuiltInWings() == null ? 0 : hullSpec.getBuiltInWings().size();
        return builtInWings > 0 && hullSpec.getFighterBays() <= builtInWings;
    }
}
