package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.defense.ArmorCells;

final class PhaseEntryRepairListener extends PhaseTransitionListener {

    static final String REPAIR_PERCENT_KEY = "exiledLib_phaseEntryArmorRepairPercent";
    static final float BATTLE_CAP_PERCENT = 50f;
    static final String REPAIRED_KEY_PREFIX = "exiledLib_phaseEntryArmorRepaired_";

    PhaseEntryRepairListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    void onPhaseEntry() {
        ArmorCells.repairWithinBattleCap(ownerShip, REPAIRED_KEY_PREFIX, magnitude(REPAIR_PERCENT_KEY), BATTLE_CAP_PERCENT);
    }
}
