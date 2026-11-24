package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.DamageTypeMults.Mults;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public final class HitDamageRestorer implements DamageListener {

    private final List<PendingRestore> pending = new ArrayList<>();
    private final Deque<DamageAPI> startedHitDamage = new ArrayDeque<>();
    private final Deque<CombatEntityAPI> startedHitTargets = new ArrayDeque<>();
    private int sideDamageDepth;
    private MixedHitBlender blender;

    public static void scaleDamageForThisHit(DamageAPI damage, float mult, CombatEntityAPI target) {
        PendingRestore entry = track(damage, target);
        damage.setDamage(damage.getBaseDamage() * mult);
        entry.noteWritten();
    }

    static void setForThisHit(DamageAPI damage, float baseDamage, float emp, CombatEntityAPI target) {
        PendingRestore entry = track(damage, target);
        damage.setDamage(baseDamage);
        damage.setFluxComponent(emp);
        entry.noteWritten();
    }

    public static void scaleEmpForThisHit(DamageAPI damage, float empMult, CombatEntityAPI target) {
        PendingRestore entry = track(damage, target);
        damage.setFluxComponent(damage.getFluxComponent() * empMult);
        entry.noteWritten();
    }

    public static void retypeForThisHit(DamageAPI damage, DamageType type, CombatEntityAPI target) {
        track(damage, target);
        damage.setType(type);
    }

    public static <T> T whileDealingSideDamage(Supplier<T> dealSideDamage) {
        HitDamageRestorer restorer = SkillEffectSupport.existingEngineListener(HitDamageRestorer.class);
        if (restorer == null) {
            return dealSideDamage.get();
        }
        Mults carrierBlend = DamageTypeMults.current(MixedHitBlender.CARRIER);
        boolean blended = !DamageTypeMults.atDefaults(MixedHitBlender.CARRIER);
        if (blended) {
            DamageTypeMults.set(MixedHitBlender.CARRIER, DamageTypeMults.defaults(MixedHitBlender.CARRIER));
        }
        restorer.sideDamageDepth++;
        try {
            return dealSideDamage.get();
        } finally {
            restorer.sideDamageDepth--;
            if (blended) {
                DamageTypeMults.set(MixedHitBlender.CARRIER, carrierBlend);
            }
        }
    }

    static int sideDamageDepth() {
        HitDamageRestorer restorer = SkillEffectSupport.existingEngineListener(HitDamageRestorer.class);
        return restorer == null ? 0 : restorer.sideDamageDepth;
    }

    public static float originalHitDamage(DamageAPI damage) {
        HitDamageRestorer restorer = SkillEffectSupport.existingEngineListener(HitDamageRestorer.class);
        PendingRestore entry = restorer == null ? null : restorer.entryFor(damage);
        if (entry == null) {
            return CombatQueries.hitDamage(damage);
        }
        float hitDamage = entry.originalBaseDamage() * damage.getModifier().getModifiedValue() * damage.getMultiplier();
        return damage.isDps() ? hitDamage * damage.getDpsDuration() : hitDamage;
    }

    void attach(MixedHitBlender mixedHitBlender) {
        blender = mixedHitBlender;
    }

    void hitStarted(DamageAPI damage, CombatEntityAPI target) {
        if (target == null) {
            return;
        }
        startedHitDamage.push(damage);
        startedHitTargets.push(target);
    }

    void endFrame() {
        for (PendingRestore entry : pending) {
            entry.restore();
        }
        pending.clear();
        startedHitDamage.clear();
        startedHitTargets.clear();
    }

    private int hitDepth() {
        return Math.max(1, startedHitDamage.size());
    }

    private boolean endsStartedHit(CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (startedHitTargets.peek() != target || target == null) {
            return false;
        }
        DamageAPI startedDamage = startedHitDamage.peek();
        DamageType reportedType = result == null ? null : result.getType();
        if (reportedType != null && reportedType != startedDamage.getType()) {
            return false;
        }
        return result == null || startedDamage.getDamage() <= 0f || dealtAnything(result);
    }

    private static boolean dealtAnything(ApplyDamageResultAPI result) {
        return result.getDamageToHull() > 0f || result.getTotalDamageToArmor() > 0f || result.getDamageToShields() > 0f
                || result.getOverMaxDamageToShields() > 0f;
    }

    private boolean inOpenMixedHit(DamageAPI damage) {
        return blender != null && blender.isOpen(damage);
    }

    private static PendingRestore track(DamageAPI damage, CombatEntityAPI target) {
        HitDamageRestorer restorer = SkillEffectSupport.ensureEngineListener(HitDamageRestorer.class, HitDamageRestorer::new);
        PendingRestore tracked = null;
        for (Iterator<PendingRestore> iterator = restorer.pending.iterator(); iterator.hasNext(); ) {
            PendingRestore entry = iterator.next();
            if (entry.target() != target) {
                continue;
            }
            if (entry.damage() == damage) {
                tracked = entry;
            } else if (entry.depth() >= restorer.sideDamageDepth && entry.hitDepth() >= restorer.hitDepth() && !restorer.inOpenMixedHit(entry.damage())) {
                entry.restore();
                iterator.remove();
            }
        }
        if (tracked == null) {
            tracked = new PendingRestore(damage, target, restorer.sideDamageDepth, restorer.hitDepth());
            restorer.pending.add(tracked);
        } else {
            tracked.keepOutsideChanges();
        }
        return tracked;
    }

    private PendingRestore entryFor(DamageAPI damage) {
        for (PendingRestore entry : pending) {
            if (entry.damage() == damage) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        boolean endsStartedHit = endsStartedHit(target, result);
        int reportedHitDepth = endsStartedHit ? hitDepth() : startedHitDamage.size() + 1;
        if (endsStartedHit && blender != null) {
            blender.hitLanded(startedHitDamage.peek(), target);
        }
        if (blender != null) {
            blender.closeHit(target, result);
        }
        if (!pending.isEmpty()) {
            pending.removeIf(restore -> !inOpenMixedHit(restore.damage()) && restore.restoreIfFor(target, sideDamageDepth, reportedHitDepth));
        }
        if (endsStartedHit) {
            startedHitDamage.pop();
            startedHitTargets.pop();
        }
    }

    private static final class PendingRestore {

        private final DamageAPI damage;
        private final CombatEntityAPI target;
        private final int depth;
        private final int hitDepth;
        private final DamageType type;
        private float originalBaseDamage;
        private float originalEmp;
        private float writtenBaseDamage;
        private float writtenEmp;

        private PendingRestore(DamageAPI damage, CombatEntityAPI target, int depth, int hitDepth) {
            this.damage = damage;
            this.target = target;
            this.depth = depth;
            this.hitDepth = hitDepth;
            this.type = damage.getType();
            this.originalBaseDamage = damage.getBaseDamage();
            this.originalEmp = damage.getFluxComponent();
            noteWritten();
        }

        private DamageAPI damage() {
            return damage;
        }

        private CombatEntityAPI target() {
            return target;
        }

        private int depth() {
            return depth;
        }

        private int hitDepth() {
            return hitDepth;
        }

        private float originalBaseDamage() {
            return originalBaseDamage;
        }

        private void noteWritten() {
            writtenBaseDamage = damage.getBaseDamage();
            writtenEmp = damage.getFluxComponent();
        }

        private void keepOutsideChanges() {
            originalBaseDamage = withOutsideChange(originalBaseDamage, writtenBaseDamage, damage.getBaseDamage());
            originalEmp = withOutsideChange(originalEmp, writtenEmp, damage.getFluxComponent());
            noteWritten();
        }

        private static float withOutsideChange(float original, float written, float current) {
            if (current == written) {
                return original;
            }
            return written != 0f ? original * current / written : original + current;
        }

        private void restore() {
            keepOutsideChanges();
            damage.setDamage(originalBaseDamage);
            damage.setFluxComponent(originalEmp);
            if (damage.getType() != type) {
                damage.setType(type);
            }
        }

        private boolean restoreIfFor(CombatEntityAPI hitTarget, int currentDepth, int currentHitDepth) {
            if (hitTarget != target || depth < currentDepth || hitDepth < currentHitDepth) {
                return false;
            }
            restore();
            return true;
        }
    }
}
