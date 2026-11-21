package exiledsector.items.pattern;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public final class HullTypes {

    private HullTypes() {
    }

    public static String of(FleetMemberAPI member) {
        return member == null ? null : of(member.getHullSpec());
    }

    public static String of(ShipHullSpecAPI hullSpec) {
        ShipHullSpecAPI undamaged = undamaged(hullSpec);
        return undamaged == null ? null : undamaged.getBaseHullId();
    }

    public static String displayName(String hullType) {
        ShipHullSpecAPI hullSpec = hullType == null ? null : hullSpecOrNull(hullType);
        return hullSpec == null ? String.valueOf(hullType) : hullSpec.getHullName();
    }

    private static ShipHullSpecAPI hullSpecOrNull(String hullId) {
        try {
            return Global.getSettings().getHullSpec(hullId);
        } catch (RuntimeException missingHull) {
            return null;
        }
    }

    private static ShipHullSpecAPI undamaged(ShipHullSpecAPI hullSpec) {
        if (hullSpec == null || !hullSpec.isDHull()) {
            return hullSpec;
        }
        ShipHullSpecAPI parent = hullSpec.getDParentHull();
        if (parent != null) {
            return parent;
        }
        return hullSpec.getBaseHull() == null ? hullSpec : hullSpec.getBaseHull();
    }
}
