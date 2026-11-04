package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.defense.ArmorCells;

final class PhaseEntryRepairListener extends ShipCombatListener implements AdvanceableListener {

    static final String REPAIR_PERCENT_KEY = "exiledSector_phaseEntryArmorRepairPercent";
    static final float BATTLE_CAP_PERCENT = 50f;
    static final String REPAIRED_KEY_PREFIX = "exiledSector_phaseEntryArmorRepaired_";

    private boolean wasPhased;

    PhaseEntryRepairListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        boolean phased = ownerShip.isPhased();
        if (phased && !wasPhased) {
            ArmorCells.repairWithinBattleCap(ownerShip, REPAIRED_KEY_PREFIX, magnitude(REPAIR_PERCENT_KEY), BATTLE_CAP_PERCENT);
        }
        wasPhased = phased;
    }
}
