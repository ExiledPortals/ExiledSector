package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.skills.tags.ShipProfile;

import java.util.function.Predicate;

public record ShipFacts(HullSize hullSize, ShieldType hullShieldType, boolean phaseHull, float baseArmor,
                        Predicate<String> hasHullMod) {

    public static ShipFacts of(ShipHullSpecAPI hullSpec, Predicate<String> hasHullMod) {
        return new ShipFacts(hullSpec.getHullSize(), hullSpec.getShieldType(), hullSpec.isPhase(), hullSpec.getArmorRating(),
                hasHullMod);
    }

    public static ShipFacts of(ShipProfile profile, Predicate<String> hasHullMod) {
        return new ShipFacts(profile.hullSize(), profile.shieldType(), profile.phaseHull(),
                profile.baseArmor(), hasHullMod);
    }
}
