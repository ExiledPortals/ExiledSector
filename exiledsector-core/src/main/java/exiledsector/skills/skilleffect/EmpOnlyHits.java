package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.compat.PrivateField;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class EmpOnlyHits implements DamageListener {

    static final String ENGINE_ARC_CLASS = "com.fs.starfarer.combat.systems.EmpArcEntity";
    static final PrivateField<Object> ARC_EMP = PrivateField.named(ENGINE_ARC_CLASS, "empDamAmount", float.class,
            "EMP arcs no longer expose their EMP, so EMP-only arcs reach EMP effects only through the EMP they apply to hulls");

    private record LateEmpHit(ShipAPI dealer, ShipFramePoint point, float emp) {
    }

    private record ModifiedHit(ShipAPI target, Object param) {
    }

    private final Set<ShipAPI> dealers = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<LateEmpHit> lateEmpHits = new ArrayList<>();
    private final List<ModifiedHit> modifiedHits = new ArrayList<>();
    private float modifiedHitsTime = -1f;
    private final EmpArcLandings arcLandings;

    EmpOnlyHits() {
        this(EmpArcLandings.ArcState::of);
    }

    EmpOnlyHits(Function<EmpArcEntityAPI, EmpArcLandings.ArcState> arcStates) {
        arcLandings = new EmpArcLandings(this, arcStates);
    }

    public static void register(ShipAPI dealer) {
        EmpOnlyHits router = routerInCombat();
        if (router != null) {
            router.addDealer(dealer);
        }
    }

    private static EmpOnlyHits routerInCombat() {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT || Global.getCombatEngine() == null) {
            return null;
        }
        return SkillEffectSupport.ensureEngineListener(EmpOnlyHits.class, EmpOnlyHits::inCombat);
    }

    private static EmpOnlyHits inCombat() {
        EmpOnlyHits router = new EmpOnlyHits();
        Global.getCombatEngine().addLayeredRenderingPlugin(router.arcLandings);
        return router;
    }

    EmpArcLandings arcLandings() {
        return arcLandings;
    }

    void addDealer(ShipAPI dealer) {
        if (dealers.add(dealer)) {
            dealer.addListener(new ModifiedHitRecorder(dealer));
        }
    }

    boolean landsArcsOf(ShipAPI dealer) {
        return dealers.contains(dealer);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (!(target instanceof ShipAPI targetShip) || !(source instanceof ShipAPI dealer)) {
            return;
        }
        if (!dealers.contains(dealer)) {
            return;
        }
        Object param = targetShip.getParamAboutToApplyDamage();
        if (forgetModifiedHit(targetShip, param) || arcLandings.handledHere(param)) {
            return;
        }
        Vector2f point = hitPoint(param);
        float baseEmp = hullEmp(targetShip, param, result);
        if (point == null || baseEmp <= 0f) {
            return;
        }
        float extraEmp = baseEmp * (HitMix.resolveEmpOnly(dealer, param, targetShip, point, false, baseEmp).empMult() - 1f);
        if (extraEmp > 0f) {
            lateEmpHits.add(new LateEmpHit(dealer, ShipFramePoint.of(targetShip, point), extraEmp));
        }
    }

    private static float hullEmp(ShipAPI target, Object param, ApplyDamageResultAPI result) {
        float empTakenMult = target.getMutableStats().getEmpDamageTakenMult().getModifiedValue();
        float appliedEmp = result.getEmpDamage();
        if (empTakenMult <= 0f || appliedEmp <= 0f) {
            return 0f;
        }
        float carriedEmp = hitEmp(param);
        return carriedEmp > 0f ? carriedEmp : appliedEmp / empTakenMult;
    }

    void applyLateEmpHits(CombatEngineAPI engine) {
        if (lateEmpHits.isEmpty()) {
            return;
        }
        List<LateEmpHit> hits = new ArrayList<>(lateEmpHits);
        lateEmpHits.clear();
        for (LateEmpHit hit : hits) {
            ShipAPI target = hit.point().ship();
            if (CombatQueries.isAliveNotHulk(target) && !target.isPhased()) {
                DealerComponentMults.applyDuring(hit.dealer(), target, () -> engine.applyDamage(target, hit.point().worldPoint(), 0f, DamageType.ENERGY,
                        hit.emp(), true, false, hit.dealer()));
            }
        }
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

    static boolean spawnsArcsOnHit(Object param) {
        if (param instanceof MissileAPI missile) {
            return missile.getSpec() != null && missile.getSpec().getOnHitEffect() != null;
        }
        return param instanceof DamagingProjectileAPI projectile && projectile.getProjectileSpec() != null
                && projectile.getProjectileSpec().getOnHitEffect() != null;
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

    private final class ModifiedHitRecorder implements DamageDealtModifier {

        private final ShipAPI dealer;

        ModifiedHitRecorder(ShipAPI dealer) {
            this.dealer = dealer;
        }

        // java:S3516: the engine reads a null return as "leave the damage modifier unchanged"; this pass notes hits and lends arcs' stats
        @SuppressWarnings("java:S3516")
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (target instanceof ShipAPI targetShip) {
                modifiedHitsThisFrame().add(new ModifiedHit(targetShip, param));
                if (arcLandings.isLanding(param)) {
                    lendDealerStats(damage);
                } else if (spawnsArcsOnHit(param) && landsArcsOf(dealer)) {
                    arcLandings.watch(targetShip);
                }
            }
            return null;
        }

        private void lendDealerStats(DamageAPI damage) {
            if (damage.getStats() == null) {
                damage.setStats(dealer.getMutableStats());
            }
        }
    }
}
