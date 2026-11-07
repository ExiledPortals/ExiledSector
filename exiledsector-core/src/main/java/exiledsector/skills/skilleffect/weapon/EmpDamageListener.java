package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class EmpDamageListener extends ShipCombatListener implements DamageDealtModifier {

    static final String EMP_INCREASED_PERCENT_KEY = "exiledSector_empDamageIncreasedPercent";
    static final String EMP_MORE_PERCENT_KEY = "exiledSector_empDamagePercent";

    EmpDamageListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    static float empMult(float increasedPercent, float morePercent) {
        return Math.max(0f, 1f + increasedPercent / 100f) * Math.max(0f, 1f + morePercent / 100f);
    }

    // java:S3516: the engine reads a null return as "leave the damage modifier unchanged"; the EMP is rescaled in place instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI) || !(damage.getFluxComponent() > 0f)) {
            return null;
        }
        float increasedPercent = magnitude(EMP_INCREASED_PERCENT_KEY);
        float morePercent = magnitude(EMP_MORE_PERCENT_KEY);
        if (increasedPercent != 0f || morePercent != 0f) {
            HitDamageRestorer.scaleEmpForThisHit(damage, empMult(increasedPercent, morePercent), target);
        }
        return null;
    }
}
