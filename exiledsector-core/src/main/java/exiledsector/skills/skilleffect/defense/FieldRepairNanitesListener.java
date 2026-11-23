package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.HullMend;
import exiledsector.skills.skilleffect.ParameterBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class FieldRepairNanitesListener extends ShipCombatListener implements AdvanceableListener {

    static final String REGEN_PERCENT_KEY = "exiledSector_fieldRepairNanitesRegenPercent";
    static final String HULL_PER_CR_BONUS_KEY = "exiledSector_fieldRepairNanitesHullPerCrBonus";
    static final float MAX_HULL_PER_SECOND = 100f;
    static final float MIN_CR_PERCENT = 15f;
    static final float CR_LOST_PERCENT = 1f;
    static final float BASE_HULL_PERCENT_PER_CR_LOST = 3f;

    private float repairedSinceLastCrLoss;

    FieldRepairNanitesListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    static float hullPercentPerCrLost(float bonus) {
        return ParameterBonus.plusFlat(BASE_HULL_PERCENT_PER_CR_LOST, bonus);
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || !ownerIsAliveNotHulk() || ownerShip.getCurrentCR() <= MIN_CR_PERCENT / 100f) {
            return;
        }
        float regenPercent = magnitude(REGEN_PERCENT_KEY);
        if (regenPercent <= 0f) {
            return;
        }
        float healed = HullMend.heal(ownerShip, regenPercent, amount, MAX_HULL_PER_SECOND * amount);
        if (healed > 0f) {
            payCombatReadiness(healed);
        }
    }

    private void payCombatReadiness(float healed) {
        float repairPerCrLost = ownerShip.getMaxHitpoints() * hullPercentPerCrLost(magnitude(HULL_PER_CR_BONUS_KEY)) / 100f;
        if (repairPerCrLost <= 0f) {
            return;
        }
        repairedSinceLastCrLoss += healed;
        while (repairedSinceLastCrLoss >= repairPerCrLost) {
            repairedSinceLastCrLoss -= repairPerCrLost;
            ownerShip.setCurrentCR(Math.max(0f, ownerShip.getCurrentCR() - CR_LOST_PERCENT / 100f));
        }
    }
}
