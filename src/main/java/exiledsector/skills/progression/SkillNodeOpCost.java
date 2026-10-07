package exiledsector.skills.progression;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.ModSettings;

public final class SkillNodeOpCost {

    public static final String FRIGATE_FIELD_ID = "exiledSector_opCostFrigate";
    public static final String DESTROYER_FIELD_ID = "exiledSector_opCostDestroyer";
    public static final String CRUISER_FIELD_ID = "exiledSector_opCostCruiser";
    public static final String CAPITAL_FIELD_ID = "exiledSector_opCostCapital";
    public static final String UNDEFINED_FIELD_ID = "exiledSector_opCostUndefined";

    public static final int DEFAULT_FRIGATE = 1;
    public static final int DEFAULT_DESTROYER = 2;
    public static final int DEFAULT_CRUISER = 3;
    public static final int DEFAULT_CAPITAL = 4;
    public static final int DEFAULT_UNDEFINED = 4;

    private SkillNodeOpCost() {
    }

    public static int perNode(ShipHullSpecAPI hullSpec) {
        return perNode(hullSpec != null ? hullSpec.getHullSize() : null);
    }

    public static int perNode(HullSize hullSize) {
        if (hullSize == HullSize.FRIGATE) return ModSettings.intOr(FRIGATE_FIELD_ID, DEFAULT_FRIGATE);
        if (hullSize == HullSize.DESTROYER) return ModSettings.intOr(DESTROYER_FIELD_ID, DEFAULT_DESTROYER);
        if (hullSize == HullSize.CRUISER) return ModSettings.intOr(CRUISER_FIELD_ID, DEFAULT_CRUISER);
        if (hullSize == HullSize.CAPITAL_SHIP) return ModSettings.intOr(CAPITAL_FIELD_ID, DEFAULT_CAPITAL);
        return ModSettings.intOr(UNDEFINED_FIELD_ID, DEFAULT_UNDEFINED);
    }
}
