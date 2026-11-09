package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;

import java.util.List;

public final class KillWatch implements DamageListener {

    public interface KillReaction {
        void onKill(ShipAPI victim);
    }

    private final ShipAPI watchedShip;
    private boolean killed;

    KillWatch(ShipAPI watchedShip) {
        this.watchedShip = watchedShip;
    }

    public static void watch(ShipAPI attacker, CombatEntityAPI target) {
        if (target instanceof ShipAPI targetShip && countsAsKill(attacker, targetShip)) {
            SkillEffectSupport.ensureListener(targetShip, KillWatch.class, KillWatch::new);
        }
    }

    public static boolean countsAsKill(ShipAPI killer, ShipAPI victim) {
        return !victim.isHulk() && !victim.isFighter() && victim.getParentStation() == null && CombatQueries.isHostile(killer, victim);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (killed || watchedShip.getHitpoints() > 0f) {
            return;
        }
        killed = true;
        if (source instanceof ShipAPI killer && CombatQueries.isHostile(killer, watchedShip)) {
            List<KillReaction> reactions = killer.getListeners(KillReaction.class);
            if (reactions != null) {
                for (KillReaction reaction : reactions) {
                    reaction.onKill(watchedShip);
                }
            }
        }
    }
}
