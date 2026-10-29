package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.skills.skilleffect.SkillEffect;

public final class BallisticAsEnergy {

    public static final String KEY = "exiledLib_ballisticCountsAsEnergy";

    private BallisticAsEnergy() {
    }

    public static boolean active(MutableShipStatsAPI stats) {
        return stats != null && stats.getDynamic().getMod(KEY).computeEffective(0f) > 0f;
    }

    public static boolean countsAsEnergy(WeaponAPI weapon) {
        if (weapon == null) {
            return false;
        }
        if (weapon.getType() == WeaponAPI.WeaponType.ENERGY) {
            return true;
        }
        ShipAPI ship = weapon.getShip();
        return weapon.getType() == WeaponAPI.WeaponType.BALLISTIC && ship != null && active(ship.getMutableStats());
    }

    public static ScopedWeaponEffect ballisticCounterpart(SkillEffect effect) {
        if (!(effect instanceof ScopedWeaponEffect weaponEffect)
                || (weaponEffect.scope() != WeaponScope.ENERGY && weaponEffect.scope() != WeaponScope.NON_BEAM_ENERGY)) {
            return null;
        }
        return ScopedWeaponEffect.find(weaponEffect.family(), WeaponScope.BALLISTIC, weaponEffect.mode());
    }
}
