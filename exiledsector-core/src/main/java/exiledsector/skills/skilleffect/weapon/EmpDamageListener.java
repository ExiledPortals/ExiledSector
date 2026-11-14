package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.EmpOnlyHits;
import exiledsector.skills.skilleffect.HitDamageRestorer;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import exiledsector.skills.skilleffect.ShipFramePoint;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class EmpDamageListener extends HitSideEffectListener implements AdvanceableListener, EmpOnlyHits.Handler, EmpOnlyHits.EmpBonus {

    static final String EMP_INCREASED_PERCENT_KEY = "exiledSector_empDamageIncreasedPercent";
    static final String EMP_MORE_PERCENT_KEY = "exiledSector_empDamagePercent";

    private record ExtraEmpHit(ShipFramePoint point, float emp) {
    }

    private final List<ExtraEmpHit> extraEmpHits = new ArrayList<>();

    EmpDamageListener(ShipAPI ownerShip) {
        super(ownerShip);
        EmpOnlyHits.registerEmpBonus(ownerShip, this);
        EmpOnlyHits.register(ownerShip, this);
    }

    static float empMult(float increasedPercent, float morePercent) {
        return Math.max(0f, SkillEffectSupport.multFrom(increasedPercent)) * Math.max(0f, SkillEffectSupport.multFrom(morePercent));
    }

    @Override
    public float empMult() {
        return empMult(magnitude(EMP_INCREASED_PERCENT_KEY), magnitude(EMP_MORE_PERCENT_KEY));
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI) || !(damage.getFluxComponent() > 0f)) {
            return;
        }
        float empMult = empMult();
        if (empMult != 1f) {
            HitDamageRestorer.scaleEmpForThisHit(damage, empMult, target);
        }
    }

    @Override
    public void onEmpOnlyHit(EmpOnlyHits.EmpOnlyHit hit) {
        if (!hit.shieldHit() && hit.emp() > hit.baseEmp()) {
            extraEmpHits.add(new ExtraEmpHit(ShipFramePoint.of(hit.target(), hit.point()), hit.emp() - hit.baseEmp()));
        }
    }

    @Override
    public void advance(float amount) {
        if (extraEmpHits.isEmpty()) {
            return;
        }
        List<ExtraEmpHit> hits = new ArrayList<>(extraEmpHits);
        extraEmpHits.clear();
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        for (ExtraEmpHit hit : hits) {
            ShipAPI target = hit.point().ship();
            if (CombatQueries.isAliveNotHulk(target) && !target.isPhased()) {
                engine.applyDamage(target, hit.point().worldPoint(), 0f, DamageType.ENERGY, hit.emp(), true, false, ownerShip);
            }
        }
    }
}
