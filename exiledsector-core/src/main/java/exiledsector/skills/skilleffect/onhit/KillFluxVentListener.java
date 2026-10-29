package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

final class KillFluxVentListener extends ShipCombatListener implements DamageDealtModifier {

    static final String MAX_FLUX_PERCENT_KEY = "exiledSector_killFluxVentPercent";

    KillFluxVentListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (target instanceof ShipAPI targetShip && countsAsKill(ownerShip, targetShip)) {
            SkillEffectSupport.ensureListener(targetShip, KillWatch.class, KillWatch::new);
        }
        return null;
    }

    static boolean countsAsKill(ShipAPI killer, ShipAPI victim) {
        return !victim.isHulk() && !victim.isFighter() && victim.getParentStation() == null && CombatQueries.isHostile(killer, victim);
    }

    static void vent(ShipAPI killer) {
        FluxTrackerAPI flux = killer.getFluxTracker();
        float percent = killer.getMutableStats().getDynamic().getValue(MAX_FLUX_PERCENT_KEY, 0f);
        if (flux != null && percent > 0f && CombatQueries.isAliveNotHulk(killer)) {
            flux.decreaseFlux(flux.getMaxFlux() * Math.min(percent, 100f) / 100f);
        }
    }

    static final class KillWatch implements DamageListener {

        private final ShipAPI watchedShip;
        private boolean killed;

        KillWatch(ShipAPI watchedShip) {
            this.watchedShip = watchedShip;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (killed || watchedShip.getHitpoints() > 0f) {
                return;
            }
            killed = true;
            if (source instanceof ShipAPI killer && CombatQueries.isHostile(killer, watchedShip)) {
                vent(killer);
            }
        }
    }
}
