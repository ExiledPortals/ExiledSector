package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

import java.util.IdentityHashMap;
import java.util.Map;

final class RepairCoordinatorListener extends ShipCombatListener implements AuraField.Source {

    static final String ARMOR_REPAIR_PERCENT_KEY = "exiledLib_repairCoordinatorArmorPercent";

    private final Map<ShipAPI, Float> armorRepairedByAlly = new IdentityHashMap<>();
    private ShipAPI repairTarget;
    private int repairCellX;
    private int repairCellY;

    RepairCoordinatorListener(ShipAPI ownerShip) {
        super(ownerShip);
        RepairCoordinatorField.register(this);
    }

    @Override
    public ShipAPI source() {
        return ownerShip;
    }

    float armorRepairPercentPerSecond() {
        if (ownerShip.isRetreating() || ownerShip.getFluxTracker() == null || ownerShip.getFluxTracker().isOverloadedOrVenting()) {
            return 0f;
        }
        return magnitude(ARMOR_REPAIR_PERCENT_KEY);
    }

    void workOn(ShipAPI ally, int cellX, int cellY) {
        repairTarget = ally;
        repairCellX = cellX;
        repairCellY = cellY;
    }

    void stopWork() {
        repairTarget = null;
    }

    Vector2f workPoint() {
        ShipAPI ally = repairTarget;
        if (ally == null || !CombatQueries.isAliveNotHulk(ally) || ally.getArmorGrid() == null) {
            return null;
        }
        return ally.getArmorGrid().getLocation(repairCellX, repairCellY);
    }

    float armorRepaired(ShipAPI ally) {
        return armorRepairedByAlly.getOrDefault(ally, 0f);
    }

    void recordRepair(ShipAPI ally, float armorPoints) {
        armorRepairedByAlly.merge(ally, armorPoints, Float::sum);
    }
}
