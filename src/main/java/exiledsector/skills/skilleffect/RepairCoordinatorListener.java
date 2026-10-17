package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.IdentityHashMap;
import java.util.Map;

final class RepairCoordinatorListener extends ShipCombatListener {

    static final String ARMOR_REPAIR_PERCENT_KEY = "exiledSector_repairCoordinatorArmorPercent";

    private final Map<ShipAPI, Float> armorRepairedByAlly = new IdentityHashMap<>();

    RepairCoordinatorListener(ShipAPI ownerShip) {
        super(ownerShip);
        RepairCoordinatorField.register(this);
    }

    ShipAPI source() {
        return ownerShip;
    }

    float armorRepairPercentPerSecond() {
        if (ownerShip.isRetreating() || ownerShip.getFluxTracker() == null || ownerShip.getFluxTracker().isOverloadedOrVenting()) {
            return 0f;
        }
        return magnitude(ARMOR_REPAIR_PERCENT_KEY);
    }

    float armorRepaired(ShipAPI ally) {
        return armorRepairedByAlly.getOrDefault(ally, 0f);
    }

    void recordRepair(ShipAPI ally, float armorPoints) {
        armorRepairedByAlly.merge(ally, armorPoints, Float::sum);
    }
}
