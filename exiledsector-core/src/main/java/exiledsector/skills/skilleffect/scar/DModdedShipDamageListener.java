package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import exiledsector.skills.skilleffect.weapon.TargetDamageBonusListener;

import java.util.IdentityHashMap;
import java.util.Map;

final class DModdedShipDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_scarDamageToDModdedShipsPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_scarDModdedShipDamage_";

    private final Map<ShipAPI, Boolean> dModdedByTarget = new IdentityHashMap<>();

    DModdedShipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
    }

    @Override
    protected boolean qualifies(ShipAPI target) {
        return !target.isFighter() && dModdedByTarget.computeIfAbsent(target, ship -> SkillEffectSupport.hasDMods(ship.getVariant()));
    }
}
