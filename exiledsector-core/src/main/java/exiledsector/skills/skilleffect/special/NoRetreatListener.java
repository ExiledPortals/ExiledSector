package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class NoRetreatListener extends ShipCombatListener implements AdvanceableListener {

    NoRetreatListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk() || !ownerShip.isRetreating()) {
            return;
        }
        ownerShip.setRetreating(false, false);
        cancelRetreatOrder();
    }

    private void cancelRetreatOrder() {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatFleetManagerAPI fleetManager = engine == null ? null : engine.getFleetManager(ownerShip.getOwner());
        if (fleetManager == null) {
            return;
        }
        CombatTaskManagerAPI taskManager = fleetManager.getTaskManager(ownerShip.isAlly());
        DeployedFleetMemberAPI deployed = fleetManager.getDeployedFleetMember(ownerShip);
        if (taskManager != null && deployed != null) {
            taskManager.orderSearchAndDestroy(deployed, false);
        }
    }
}
