package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.SkillEffectSupport;

import java.util.IdentityHashMap;
import java.util.Map;

final class PristineShipDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_damageToPristineShipsPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_pristineShipDamage_";

    private final Map<ShipAPI, Boolean> pristineByTarget = new IdentityHashMap<>();

    PristineShipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
    }

    @Override
    protected boolean qualifies(ShipAPI target) {
        return !target.isFighter() && pristineByTarget.computeIfAbsent(target, ship -> !SkillEffectSupport.hasDMods(ship.getVariant()));
    }
}
