package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;

final class DamagedShipDamageListener extends StrikecraftTargetDamageListener {

    static final String DAMAGE_PERCENT_KEY = "exiledLib_strikecraftDamageToDamagedShipsPercent";
    static final String DAMAGE_MOD_ID = "exiledLib_strikecraftDamageToDamagedShips";
    static final float DAMAGED_HULL_LEVEL = 0.5f;

    DamagedShipDamageListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, DAMAGE_PERCENT_KEY, DAMAGE_MOD_ID);
    }

    @Override
    boolean qualifies(ShipAPI target) {
        return !target.isFighter() && target.getHullLevel() < DAMAGED_HULL_LEVEL;
    }
}
