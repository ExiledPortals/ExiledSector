package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.skills.skilleffect.DeploymentRepairTally;
import exiledsector.skills.skilleffect.NearbyWrecks;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.defense.ArmorCells;

final class WreckPlatingListener extends ShipCombatListener implements AdvanceableListener {

    static final String REPAIR_CAP_PERCENT_KEY = "exiledLib_nearbyDestructionArmorRepairCapPercent";
    static final float RANGE = 1000f;
    static final float REPAIR_PERCENT_PER_WRECK = 5f;
    private static final float CHECK_SECONDS = 0.25f;

    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final NearbyWrecks wrecks;
    private final DeploymentRepairTally repairTally = new DeploymentRepairTally();

    WreckPlatingListener(ShipAPI ownerShip) {
        super(ownerShip);
        this.wrecks = new NearbyWrecks(ownerShip, other -> WreckReclaimListener.isEnemyShip(ownerShip, other));
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        float capPercent = magnitude(REPAIR_CAP_PERCENT_KEY);
        if (capPercent <= 0f) {
            return;
        }
        int destroyed = wrecks.newWithin(RANGE).size();
        if (destroyed > 0) {
            ArmorCells.repairWithinCap(ownerShip, repairTally, destroyed * REPAIR_PERCENT_PER_WRECK, capPercent);
        }
    }
}
