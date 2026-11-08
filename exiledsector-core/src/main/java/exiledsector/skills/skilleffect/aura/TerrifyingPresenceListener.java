package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ParameterBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class TerrifyingPresenceListener extends ShipCombatListener implements AuraField.Source {

    static final String ACCURACY_PENALTY_PERCENT_KEY = "exiledSector_terrifyingPresenceAccuracyPenaltyPercent";
    static final String RANGE_PERCENT_KEY = "exiledSector_terrifyingPresenceRangePercent";

    TerrifyingPresenceListener(ShipAPI ownerShip) {
        super(ownerShip);
        TerrifyingPresenceField.register(this);
    }

    @Override
    public ShipAPI source() {
        return ownerShip;
    }

    float accuracyPenalty() {
        return ownerShip.isRetreating() ? 0f : magnitude(ACCURACY_PENALTY_PERCENT_KEY) / 100f;
    }

    float range() {
        return range(magnitude(RANGE_PERCENT_KEY));
    }

    static float range(float rangeIncreasePercent) {
        return ParameterBonus.increasedBy(TerrifyingPresenceField.RANGE, rangeIncreasePercent);
    }
}
