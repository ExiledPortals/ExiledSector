package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

final class DroneDamageListener extends StrikecraftTargetDamageListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_strikecraftDamageToDronesPercent";
    static final String DAMAGE_MOD_ID = "exiledSector_strikecraftDamageToDrones";

    DroneDamageListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, DAMAGE_PERCENT_KEY, DAMAGE_MOD_ID);
    }

    static boolean isDrone(ShipAPI target) {
        if (!target.isFighter()) {
            return false;
        }
        ShipHullSpecAPI hullSpec = target.getHullSpec();
        return hullSpec != null && hullSpec.getMinCrew() <= 0f;
    }

    @Override
    boolean qualifies(ShipAPI target) {
        return isDrone(target);
    }
}
