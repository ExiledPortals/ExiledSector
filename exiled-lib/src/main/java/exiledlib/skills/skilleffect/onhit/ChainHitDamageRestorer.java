package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledlib.skills.skilleffect.SkillEffectSupport;

import java.util.ArrayList;
import java.util.List;

final class ChainHitDamageRestorer implements DamageListener {

    private final List<PendingRestore> pending = new ArrayList<>();

    static void reduceForThisHit(DamageAPI damage, float dealtMult, CombatEntityAPI target) {
        ChainHitDamageRestorer restorer = SkillEffectSupport.ensureEngineListener(ChainHitDamageRestorer.class, ChainHitDamageRestorer::new);
        restorer.restoreStaleHits();
        float baseDamage = damage.getBaseDamage();
        restorer.pending.add(new PendingRestore(damage, baseDamage, target));
        damage.setDamage(baseDamage * dealtMult);
    }

    private void restoreStaleHits() {
        for (PendingRestore stale : pending) {
            stale.damage().setDamage(stale.baseDamage());
        }
        pending.clear();
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (pending.isEmpty()) {
            return;
        }
        pending.removeIf(restore -> restore.restoreIfFor(target));
    }

    private record PendingRestore(DamageAPI damage, float baseDamage, CombatEntityAPI target) {
        private boolean restoreIfFor(CombatEntityAPI hitTarget) {
            if (hitTarget != target) {
                return false;
            }
            damage.setDamage(baseDamage);
            return true;
        }
    }
}
