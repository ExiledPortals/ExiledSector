package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.util.Locale;

public final class MineHits {

    private static final String MINE_MARKER = "mine";

    private MineHits() {
    }

    public static boolean isMineHit(Object param) {
        if (param instanceof MissileAPI missile && missile.isMine()) {
            return true;
        }
        if (!(param instanceof DamagingProjectileAPI projectile)) {
            return false;
        }
        WeaponAPI weapon = projectile.getWeapon();
        return namesAMine(projectile.getProjectileSpecId()) || weapon != null && namesAMine(weapon.getId());
    }

    static boolean namesAMine(String specId) {
        return specId != null && specId.toLowerCase(Locale.ROOT).contains(MINE_MARKER);
    }
}
