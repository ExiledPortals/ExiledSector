package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.DeploymentRepairTally;
import exiledsector.skills.skilleffect.defense.ArmorCells;

final class PhaseEntryRepairListener extends PhaseTransitionListener {

    static final String REPAIR_PERCENT_KEY = "exiledSector_phaseEntryArmorRepairPercent";
    static final float DEPLOYMENT_CAP_PERCENT = 50f;

    private final DeploymentRepairTally repairTally = new DeploymentRepairTally();

    PhaseEntryRepairListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    void onPhaseEntry() {
        ArmorCells.repairWithinCap(ownerShip, repairTally, magnitude(REPAIR_PERCENT_KEY), DEPLOYMENT_CAP_PERCENT);
    }
}
