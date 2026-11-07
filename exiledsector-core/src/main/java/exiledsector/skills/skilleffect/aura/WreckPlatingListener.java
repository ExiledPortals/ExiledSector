package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.DeploymentRepairTally;
import exiledsector.skills.skilleffect.NearbyDestructionListener;
import exiledsector.skills.skilleffect.defense.ArmorCells;

import java.util.List;

final class WreckPlatingListener extends NearbyDestructionListener {

    static final String REPAIR_CAP_PERCENT_KEY = "exiledSector_nearbyDestructionArmorRepairCapPercent";
    static final float RANGE = 1000f;
    static final float REPAIR_PERCENT_PER_WRECK = 5f;

    private final DeploymentRepairTally repairTally = new DeploymentRepairTally();

    WreckPlatingListener(ShipAPI ownerShip) {
        super(ownerShip, other -> WreckReclaimListener.isEnemyShip(ownerShip, other));
    }

    @Override
    protected float activeRange() {
        return magnitude(REPAIR_CAP_PERCENT_KEY) > 0f ? RANGE : 0f;
    }

    @Override
    protected void onDestroyed(List<ShipAPI> wrecks) {
        ArmorCells.repairWithinCap(ownerShip, repairTally, wrecks.size() * REPAIR_PERCENT_PER_WRECK, magnitude(REPAIR_CAP_PERCENT_KEY));
    }
}
