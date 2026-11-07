package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.skills.skilleffect.weapon.BallisticAsEnergy;

public final class RiftStrike {

    public static final String CHANCE_PERCENT_KEY = "exiledSector_riftStrikeChancePercent";
    public static final float DAMAGE_SHARE_PERCENT = 50f;
    public static final float MIN_DAMAGE = 30f;
    public static final float MAX_DAMAGE = 500f;

    private RiftStrike() {
    }

    public static boolean firesRifts(WeaponSpecAPI weaponSpec, boolean ballisticCountsAsEnergy) {
        if (weaponSpec == null || weaponSpec.isBeam()) {
            return false;
        }
        WeaponType type = weaponSpec.getType();
        return type == WeaponType.ENERGY || ballisticCountsAsEnergy && type == WeaponType.BALLISTIC;
    }

    public static boolean firesRifts(WeaponAPI weapon) {
        return weapon != null && !weapon.isBeam() && BallisticAsEnergy.countsAsEnergy(weapon);
    }

    public static float baseRiftDamage(WeaponSpecAPI weaponSpec) {
        float baseShotDamage = weaponSpec.getDerivedStats().getDamagePerShot();
        return Math.max(MIN_DAMAGE, Math.min(MAX_DAMAGE, baseShotDamage * DAMAGE_SHARE_PERCENT / 100f));
    }

    public static float riftDamage(WeaponSpecAPI weaponSpec, float increasedRiftDamagePercent) {
        return baseRiftDamage(weaponSpec) * (1f + increasedRiftDamagePercent / 100f);
    }
}
