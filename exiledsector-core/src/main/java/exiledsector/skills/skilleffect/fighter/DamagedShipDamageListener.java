package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.CombatGate;
import exiledsector.skills.skilleffect.weapon.TargetDamageBonusListener;

final class DamagedShipDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_strikecraftDamageToDamagedShipsPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_strikecraftDamageToDamagedShips_";

    DamagedShipDamageListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
    }

    @Override
    protected boolean qualifies(ShipAPI target) {
        return !target.isFighter() && CombatGate.LOW_HULL.holds(target);
    }
}
