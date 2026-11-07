package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public final class HitDamageRestorer implements DamageListener {

    private final List<PendingRestore> pending = new ArrayList<>();
    private int sideDamageDepth;

    public static void reduceForThisHit(DamageAPI damage, float dealtMult, CombatEntityAPI target) {
        track(damage, target);
        damage.setDamage(damage.getBaseDamage() * dealtMult);
    }

    public static void moveToEmpForThisHit(DamageAPI damage, float empShare, CombatEntityAPI target) {
        track(damage, target);
        float baseDamage = damage.getBaseDamage();
        damage.setDamage(baseDamage * (1f - empShare));
        damage.setFluxComponent(damage.getFluxComponent() + baseDamage * empShare);
    }

    public static void addEmpForThisHit(DamageAPI damage, float extraEmp, CombatEntityAPI target) {
        track(damage, target);
        damage.setFluxComponent(damage.getFluxComponent() + extraEmp);
    }

    public static void scaleEmpForThisHit(DamageAPI damage, float empMult, CombatEntityAPI target) {
        track(damage, target);
        damage.setFluxComponent(damage.getFluxComponent() * empMult);
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
        restorer.sideDamageDepth++;
        try {
            return dealSideDamage.get();
        } finally {
            restorer.sideDamageDepth--;
        }
    }

    public static float originalHitDamage(DamageAPI damage) {
        HitDamageRestorer restorer = SkillEffectSupport.existingEngineListener(HitDamageRestorer.class);
        PendingRestore entry = restorer == null ? null : restorer.entryFor(damage);
        if (entry == null) {
            return CombatQueries.hitDamage(damage);
        }
        float hitDamage = entry.baseDamage() * damage.getModifier().getModifiedValue() * damage.getMultiplier();
        return damage.isDps() ? hitDamage * damage.getDpsDuration() : hitDamage;
    }

    private static void track(DamageAPI damage, CombatEntityAPI target) {
        HitDamageRestorer restorer = SkillEffectSupport.ensureEngineListener(HitDamageRestorer.class, HitDamageRestorer::new);
        boolean tracked = false;
        for (Iterator<PendingRestore> iterator = restorer.pending.iterator(); iterator.hasNext(); ) {
            PendingRestore entry = iterator.next();
            if (entry.target() != target) {
                continue;
            }
            if (entry.damage() == damage) {
                tracked = true;
            } else if (entry.depth() >= restorer.sideDamageDepth) {
                entry.restore();
                iterator.remove();
            }
        }
        if (!tracked) {
            restorer.pending.add(new PendingRestore(damage, damage.getBaseDamage(), damage.getFluxComponent(), damage.getType(), target,
                    restorer.sideDamageDepth));
        }
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
        if (pending.isEmpty()) {
            return;
        }
        pending.removeIf(restore -> restore.restoreIfFor(target, sideDamageDepth));
    }

    private record PendingRestore(DamageAPI damage, float baseDamage, float empDamage, DamageType type, CombatEntityAPI target, int depth) {

        private void restore() {
            damage.setDamage(baseDamage);
            damage.setFluxComponent(empDamage);
            if (damage.getType() != type) {
                damage.setType(type);
            }
        }

        private boolean restoreIfFor(CombatEntityAPI hitTarget, int currentDepth) {
            if (hitTarget != target || depth < currentDepth) {
                return false;
            }
            restore();
            return true;
        }
    }
}
