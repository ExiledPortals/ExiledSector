package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.compat.PrivateField;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

final class EmpArcLandings extends BaseCombatLayeredRenderingPlugin {

    private static final String UNREADABLE_ARC_WARNING = "EMP arcs no longer expose what they will hit, so EMP-only arcs keep their EMP unconverted and their EMP bonuses "
            + "land on the next frame instead";
    static final PrivateField<Object> ARC_SOURCE = PrivateField.namedOfType(EmpOnlyHits.ENGINE_ARC_CLASS, "source",
            "com.fs.starfarer.combat.entities.Ship", UNREADABLE_ARC_WARNING);
    static final PrivateField<Object> ARC_TARGET = PrivateField.named(EmpOnlyHits.ENGINE_ARC_CLASS, "empTargetEntity", CombatEntityAPI.class,
            UNREADABLE_ARC_WARNING);
    static final PrivateField<Object> ARC_DAMAGE = PrivateField.named(EmpOnlyHits.ENGINE_ARC_CLASS, "damAmount", float.class,
            UNREADABLE_ARC_WARNING);
    static final PrivateField<Object> ARC_LANDED = PrivateField.named(EmpOnlyHits.ENGINE_ARC_CLASS, "didDamage", boolean.class,
            UNREADABLE_ARC_WARNING);
    static final float LANDING_REACH_MARGIN = 50f;

    record ArcState(ShipAPI dealer, CombatEntityAPI target, float damage, float emp, boolean landed) {

        static ArcState of(EmpArcEntityAPI arc) {
            if (!EmpOnlyHits.isEngineArc(arc) || !(ARC_SOURCE.read(arc) instanceof ShipAPI dealer) || !(ARC_DAMAGE.read(arc) instanceof Float damage)
                    || !(ARC_LANDED.read(arc) instanceof Boolean landed)) {
                return null;
            }
            CombatEntityAPI target = ARC_TARGET.read(arc) instanceof CombatEntityAPI entity ? entity : null;
            return new ArcState(dealer, target, damage, EmpOnlyHits.arcEmp(arc), landed);
        }
    }

    private record PendingArc(EmpArcEntityAPI arc, ShipAPI dealer, ShipAPI target, Vector2f point, float emp, boolean shieldHit) {
    }

    private final EmpOnlyHits router;
    private final Function<EmpArcEntityAPI, ArcState> arcStates;
    private final Set<ShipAPI> watchedTargets = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<EmpArcEntityAPI> handledArcs = Collections.newSetFromMap(new IdentityHashMap<>());
    private EmpArcEntityAPI arcBeingLanded;

    EmpArcLandings(EmpOnlyHits router, Function<EmpArcEntityAPI, ArcState> arcStates) {
        this.router = router;
        this.arcStates = arcStates;
    }

    void watch(ShipAPI target) {
        watchedTargets.add(target);
    }

    boolean handledHere(Object param) {
        return param instanceof EmpArcEntityAPI arc && handledArcs.contains(arc);
    }

    boolean isLanding(Object param) {
        return param != null && param == arcBeingLanded;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.noneOf(CombatEngineLayers.class);
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        handledArcs.removeIf(arc -> !engine.isEntityInPlay(arc));
        router.applyLateEmpHits(engine);
        if (watchedTargets.isEmpty()) {
            return;
        }
        List<ShipAPI> targets = new ArrayList<>(watchedTargets);
        watchedTargets.clear();
        for (PendingArc pending : pendingArcsNear(engine, targets)) {
            land(engine, pending);
        }
    }

    private List<PendingArc> pendingArcsNear(CombatEngineAPI engine, List<ShipAPI> targets) {
        Set<EmpArcEntityAPI> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<PendingArc> pendingArcs = new ArrayList<>();
        for (ShipAPI target : targets) {
            float searchSize = 2f * landingReach(target);
            Iterator<Object> nearby = engine.getAllObjectGrid().getCheckIterator(target.getLocation(), searchSize, searchSize);
            while (nearby.hasNext()) {
                if (nearby.next() instanceof EmpArcEntityAPI arc && seen.add(arc) && !handledArcs.contains(arc)) {
                    PendingArc pending = pendingArc(arc);
                    if (pending != null) {
                        pendingArcs.add(pending);
                    }
                }
            }
        }
        return pendingArcs;
    }

    private PendingArc pendingArc(EmpArcEntityAPI arc) {
        ArcState state = arcStates.apply(arc);
        if (state == null || state.landed() || state.damage() > 0f || state.emp() <= 0f || !router.landsArcsOf(state.dealer())) {
            return null;
        }
        Vector2f point = arc.getTargetLocation();
        if (point == null || !(state.target() instanceof ShipAPI target) || !CombatQueries.isAliveNotHulk(target)
                || Vector2f.sub(point, target.getLocation(), null).length() > landingReach(target)) {
            return null;
        }
        return new PendingArc(arc, state.dealer(), target, new Vector2f(point), state.emp(), arc.isShieldHit());
    }

    private static float landingReach(ShipAPI target) {
        ShieldAPI shield = target.getShield();
        float shieldRadius = shield == null ? 0f : shield.getRadius();
        return Math.max(target.getCollisionRadius(), shieldRadius) + LANDING_REACH_MARGIN;
    }

    private void land(CombatEngineAPI engine, PendingArc pending) {
        HitMix.EmpOnlyResult hit = HitMix.resolveEmpOnly(pending.dealer(), pending.arc(), pending.target(), pending.point(), pending.shieldHit(),
                pending.emp());
        boolean arcStillLands = pending.shieldHit() || hit.movedDamage().isEmpty() && hit.emp() >= pending.emp();
        if (!arcStillLands && !ARC_LANDED.write(pending.arc(), true)) {
            return;
        }
        handledArcs.add(pending.arc());
        if (pending.shieldHit() && !shieldStillCovers(pending.target(), pending.point())) {
            return;
        }
        float empToApply;
        if (pending.shieldHit()) {
            empToApply = 0f;
        } else {
            empToApply = arcStillLands ? hit.emp() - pending.emp() : hit.emp();
        }
        if (hit.movedDamage().isEmpty() && empToApply <= 0f) {
            return;
        }
        arcBeingLanded = pending.arc();
        try {
            applyHits(engine, pending, hit.movedDamage(), empToApply);
        } finally {
            arcBeingLanded = null;
        }
    }

    private static boolean shieldStillCovers(ShipAPI target, Vector2f point) {
        ShieldAPI shield = target.getShield();
        return shield != null && shield.isOn() && shield.isWithinArc(point);
    }

    private static void applyHits(CombatEngineAPI engine, PendingArc pending, Map<DamageType, Float> movedDamage, float emp) {
        boolean bypassShields = !pending.shieldHit();
        if (emp > 0f) {
            DealerComponentMults.applyDuring(pending.dealer(), pending.target(), () -> engine.applyDamage(pending.arc(), pending.target(),
                    pending.point(), 0f, DamageType.ENERGY, emp, bypassShields, false, pending.dealer(), false));
        }
        HitMix.applyConverted(pending.arc(), () -> {
            for (Map.Entry<DamageType, Float> moved : movedDamage.entrySet()) {
                engine.applyDamage(pending.arc(), pending.target(), pending.point(), moved.getValue(), moved.getKey(), 0f, bypassShields, false,
                        pending.dealer(), false);
            }
        });
    }
}
