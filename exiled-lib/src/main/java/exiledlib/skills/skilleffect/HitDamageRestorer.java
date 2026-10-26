package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class HitDamageRestorer implements DamageListener {

    private final List<PendingRestore> pending = new ArrayList<>();

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

    public static void scaleEmpForThisHit(DamageAPI damage, float empMult, CombatEntityAPI target) {
        track(damage, target);
        damage.setFluxComponent(damage.getFluxComponent() * empMult);
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
            } else {
                entry.restore();
                iterator.remove();
            }
        }
        if (!tracked) {
            restorer.pending.add(new PendingRestore(damage, damage.getBaseDamage(), damage.getFluxComponent(), target));
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
        pending.removeIf(restore -> restore.restoreIfFor(target));
    }

    private record PendingRestore(DamageAPI damage, float baseDamage, float empDamage, CombatEntityAPI target) {

        private void restore() {
            damage.setDamage(baseDamage);
            damage.setFluxComponent(empDamage);
        }

        private boolean restoreIfFor(CombatEntityAPI hitTarget) {
            if (hitTarget != target) {
                return false;
            }
            restore();
            return true;
        }
    }
}
