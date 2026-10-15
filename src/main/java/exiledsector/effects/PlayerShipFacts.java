package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.compat.SecondInCommandCompat;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.ShipFacts;

public final class PlayerShipFacts {

    private PlayerShipFacts() {
    }

    public static ShipFacts of(FleetMemberAPI member, ShipVariantAPI variant) {
        return ShipFacts.of(member.getHullSpec(), hullModId -> hasHullMod(variant, hullModId), hullModId -> hasFixedHullMod(variant, hullModId));
    }

    public static boolean hasHullMod(ShipVariantAPI variant, String hullModId) {
        return InstalledHullMods.hasHullModOfItsOwn(variant, hullModId) || SecondInCommandCompat.hasDeactivatedSMod(variant, hullModId);
    }

    static boolean hasFixedHullMod(ShipVariantAPI variant, String hullModId) {
        return InstalledHullMods.hasFixedHullModOfItsOwn(variant, hullModId) || SecondInCommandCompat.hasDeactivatedSMod(variant, hullModId);
    }
}
