package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;

final class SlowerShipDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_damageToSlowerShipsPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_slowerShipDamage_";

    SlowerShipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
    }

    static float currentTopSpeed(ShipAPI ship) {
        ShipEngineControllerAPI engines = ship.getEngineController();
        if (engines != null && engines.isFlamedOut()) {
            return 0f;
        }
        return ship.getMaxSpeed() * ship.getMutableStats().getTimeMult().getModifiedValue();
    }

    @Override
    protected boolean qualifies(ShipAPI target) {
        return !target.isFighter() && currentTopSpeed(target) < currentTopSpeed(ownerShip);
    }
}
