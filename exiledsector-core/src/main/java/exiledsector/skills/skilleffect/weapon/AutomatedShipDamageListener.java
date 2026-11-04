package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

final class AutomatedShipDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_damageToAutomatedShipsPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_automatedShipDamage_";

    AutomatedShipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
    }

    @Override
    boolean qualifies(ShipAPI target) {
        return Misc.isAutomated(target);
    }
}
