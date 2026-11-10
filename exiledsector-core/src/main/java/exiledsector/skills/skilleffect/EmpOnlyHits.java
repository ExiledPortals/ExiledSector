package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.compat.PrivateField;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class EmpOnlyHits implements DamageDealtModifier, DamageListener {

    static final String ENGINE_ARC_CLASS = "com.fs.starfarer.combat.systems.EmpArcEntity";
    static final PrivateField<Object> ARC_EMP = PrivateField.named(ENGINE_ARC_CLASS, "empDamAmount", float.class,
            "EMP arcs no longer expose their EMP, so EMP-only arcs reach EMP effects only through the EMP they apply to hulls");

    public interface Handler {
        void onEmpOnlyHit(EmpOnlyHit hit);
    }

    public interface EmpBonus {
        float empMult();
    }

    public record EmpOnlyHit(ShipAPI target, Vector2f point, float baseEmp, float emp, boolean shieldHit) {
    }

    private record ModifiedHit(ShipAPI target, Object param) {
    }

    private final Map<ShipAPI, List<Handler>> handlersByDealer = new IdentityHashMap<>();
    private final Map<ShipAPI, EmpBonus> bonusByDealer = new IdentityHashMap<>();
    private final List<ModifiedHit> modifiedHits = new ArrayList<>();
    private float modifiedHitsTime = -1f;

    public static void register(ShipAPI dealer, Handler handler) {
        EmpOnlyHits router = routerInCombat();
        if (router != null) {
            router.addHandler(dealer, handler);
        }
    }

    public static void registerEmpBonus(ShipAPI dealer, EmpBonus bonus) {
        EmpOnlyHits router = routerInCombat();
        if (router != null) {
            router.addEmpBonus(dealer, bonus);
        }
    }

    private static EmpOnlyHits routerInCombat() {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT || Global.getCombatEngine() == null) {
            return null;
        }
        return SkillEffectSupport.ensureEngineListener(EmpOnlyHits.class, EmpOnlyHits::new);
    }

    void addHandler(ShipAPI dealer, Handler handler) {
        handlersByDealer.computeIfAbsent(dealer, ignored -> new ArrayList<>()).add(handler);
    }

    void addEmpBonus(ShipAPI dealer, EmpBonus bonus) {
        bonusByDealer.put(dealer, bonus);
    }

    // java:S3516: the engine reads a null return as "leave the damage modifier unchanged"; this pass only notes which hits it saw
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target instanceof ShipAPI targetShip) {
            modifiedHitsThisFrame().add(new ModifiedHit(targetShip, param));
        }
        return null;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (!(target instanceof ShipAPI targetShip)) {
            return;
        }
        Object param = targetShip.getParamAboutToApplyDamage();
        if (forgetModifiedHit(targetShip, param) || !(source instanceof ShipAPI dealer)) {
            return;
        }
        List<Handler> handlers = handlersByDealer.get(dealer);
        EmpOnlyHit hit = handlers == null ? null : describe(dealer, targetShip, param, result);
        if (hit == null) {
            return;
        }
        for (Handler handler : new ArrayList<>(handlers)) {
            handler.onEmpOnlyHit(hit);
        }
    }

    private EmpOnlyHit describe(ShipAPI dealer, ShipAPI target, Object param, ApplyDamageResultAPI result) {
        Vector2f point = hitPoint(param);
        float empTakenMult = target.getMutableStats().getEmpDamageTakenMult().getModifiedValue();
        if (point == null || empTakenMult <= 0f) {
            return null;
        }
        float appliedEmp = result.getEmpDamage();
        boolean shieldHit = appliedEmp <= 0f;
        float carriedEmp = hitEmp(param);
        float baseEmp = carriedEmp > 0f || shieldHit ? carriedEmp : appliedEmp / empTakenMult;
        if (baseEmp <= 0f) {
            return null;
        }
        EmpBonus bonus = bonusByDealer.get(dealer);
        float emp = bonus == null ? baseEmp : baseEmp * bonus.empMult();
        return new EmpOnlyHit(target, new Vector2f(point), baseEmp, emp, shieldHit);
    }

    static Vector2f hitPoint(Object param) {
        if (param instanceof EmpArcEntityAPI arc) {
            return arc.getTargetLocation();
        }
        if (param instanceof DamagingProjectileAPI projectile) {
            return projectile.getLocation();
        }
        return param instanceof BeamAPI beam ? beam.getTo() : null;
    }

    static float hitEmp(Object param) {
        if (param instanceof EmpArcEntityAPI arc) {
            return arcEmp(arc);
        }
        DamageAPI damage = null;
        if (param instanceof DamagingProjectileAPI projectile) {
            damage = projectile.getDamage();
        } else if (param instanceof BeamAPI beam) {
            damage = beam.getDamage();
        }
        return damage == null ? 0f : damage.computeFluxDealt(damage.getDpsDuration());
    }

    static float arcEmp(EmpArcEntityAPI arc) {
        return isEngineArc(arc) && ARC_EMP.read(arc) instanceof Float emp ? emp : 0f;
    }

    static boolean isEngineArc(EmpArcEntityAPI arc) {
        for (Class<?> type = arc.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals(ENGINE_ARC_CLASS)) {
                return true;
            }
        }
        return false;
    }

    private List<ModifiedHit> modifiedHitsThisFrame() {
        CombatEngineAPI engine = Global.getCombatEngine();
        float now = engine == null ? 0f : engine.getTotalElapsedTime(false);
        if (now != modifiedHitsTime) {
            modifiedHits.clear();
            modifiedHitsTime = now;
        }
        return modifiedHits;
    }

    private boolean forgetModifiedHit(ShipAPI target, Object param) {
        List<ModifiedHit> hits = modifiedHitsThisFrame();
        for (int i = hits.size() - 1; i >= 0; i--) {
            ModifiedHit hit = hits.get(i);
            if (hit.target() == target && hit.param() == param) {
                hits.remove(i);
                return true;
            }
        }
        return false;
    }
}
