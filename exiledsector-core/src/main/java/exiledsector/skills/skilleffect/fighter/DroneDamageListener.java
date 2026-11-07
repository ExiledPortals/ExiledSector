package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.skills.skilleffect.weapon.TargetDamageBonusListener;

final class DroneDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_strikecraftDamageToDronesPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_strikecraftDamageToDrones_";

    DroneDamageListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
    }

    static boolean isDrone(ShipAPI target) {
        if (!target.isFighter()) {
            return false;
        }
        ShipHullSpecAPI hullSpec = target.getHullSpec();
        return hullSpec != null && hullSpec.getMinCrew() <= 0f;
    }

    @Override
    protected boolean qualifies(ShipAPI target) {
        return isDrone(target);
    }
}
