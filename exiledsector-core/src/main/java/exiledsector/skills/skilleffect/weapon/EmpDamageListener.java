package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class EmpDamageListener extends ShipCombatListener implements DamageDealtModifier {

    static final String EMP_PERCENT_KEY = "exiledSector_empDamagePercent";

    EmpDamageListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    static float empMult(float percent) {
        return Math.max(0f, 1f + percent / 100f);
    }

    // java:S3516: the engine reads a null return as "leave the damage modifier unchanged"; the EMP is rescaled in place instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI) || !(damage.getFluxComponent() > 0f)) {
            return null;
        }
        float percent = magnitude(EMP_PERCENT_KEY);
        if (percent != 0f) {
            HitDamageRestorer.scaleEmpForThisHit(damage, empMult(percent), target);
        }
        return null;
    }
}
