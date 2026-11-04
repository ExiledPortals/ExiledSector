package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.CombatGate;

final class DamagedShipDamageListener extends StrikecraftTargetDamageListener {

    static final String DAMAGE_PERCENT_KEY = "exiledLib_strikecraftDamageToDamagedShipsPercent";
    static final String DAMAGE_MOD_ID = "exiledLib_strikecraftDamageToDamagedShips";

    DamagedShipDamageListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, DAMAGE_PERCENT_KEY, DAMAGE_MOD_ID);
    }

    @Override
    boolean qualifies(ShipAPI target) {
        return !target.isFighter() && CombatGate.LOW_HULL.holds(target);
    }
}
