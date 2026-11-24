package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.EmpOnlyHits;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

final class EmpDamageListener extends HitSideEffectListener implements HitMix.Resolver {

    static final String EMP_INCREASED_PERCENT_KEY = "exiledSector_empDamageIncreasedPercent";
    static final String EMP_MORE_PERCENT_KEY = "exiledSector_empDamagePercent";

    EmpDamageListener(ShipAPI ownerShip) {
        super(ownerShip);
        EmpOnlyHits.register(ownerShip);
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        HitMix.resolve(ownerShip, this, param, target, damage, point, shieldHit);
    }

    @Override
    public void resolve(HitMix.Hit hit) {
        if (hit.target() instanceof ShipAPI) {
            hit.increaseEmp(magnitude(EMP_INCREASED_PERCENT_KEY));
            hit.moreEmp(SkillEffectSupport.multFrom(magnitude(EMP_MORE_PERCENT_KEY)));
        }
    }
}
