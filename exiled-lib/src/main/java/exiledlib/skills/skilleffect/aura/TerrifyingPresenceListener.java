package exiledlib.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledlib.skills.skilleffect.ShipCombatListener;

final class TerrifyingPresenceListener extends ShipCombatListener implements AuraField.Source {

    static final String ACCURACY_PENALTY_PERCENT_KEY = "exiledSector_terrifyingPresenceAccuracyPenaltyPercent";

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
}
