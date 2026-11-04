package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

import java.util.IdentityHashMap;
import java.util.Map;

final class HullDamageRegrowthListener extends ShipCombatListener {

    static final String FRAGMENTS_PER_STEP_KEY = "exiledSector_hullDamageFragmentRegrowth";
    static final float HULL_DAMAGE_PER_STEP = 300f;

    private float fragmentsOwed;

    HullDamageRegrowthListener(ShipAPI ownerShip) {
        super(ownerShip);
        DealtHullDamageRouter.register(this);
    }

    void onHullDamageDealt(ShipAPI target, float hullDamage) {
        if (target.isHulk() || !CombatQueries.isHostile(ownerShip, target) || !ownerIsAliveNotHulk()) {
            return;
        }
        RoilingSwarmEffect swarm = FragmentSwarms.swarmOf(ownerShip);
        if (swarm == null) {
            return;
        }
        fragmentsOwed += fragmentsEarned(hullDamage, magnitude(FRAGMENTS_PER_STEP_KEY));
        int whole = (int) fragmentsOwed;
        if (whole > 0) {
            fragmentsOwed -= whole;
            FragmentSwarms.regrow(swarm, whole);
        }
    }

    static float fragmentsEarned(float hullDamage, float fragmentsPerStep) {
        return hullDamage <= 0f || fragmentsPerStep <= 0f ? 0f : hullDamage * fragmentsPerStep / HULL_DAMAGE_PER_STEP;
    }

    static final class DealtHullDamageRouter implements DamageListener {

        private final Map<ShipAPI, HullDamageRegrowthListener> listenersByDealer = new IdentityHashMap<>();

        static void register(HullDamageRegrowthListener listener) {
            if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT || Global.getCombatEngine() == null) {
                return;
            }
            SkillEffectSupport.ensureEngineListener(DealtHullDamageRouter.class, DealtHullDamageRouter::new)
                    .listenersByDealer.put(listener.ownerShip, listener);
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (source instanceof ShipAPI dealer && target instanceof ShipAPI targetShip && result.getDamageToHull() > 0f) {
                HullDamageRegrowthListener listener = listenersByDealer.get(dealer);
                if (listener != null) {
                    listener.onHullDamageDealt(targetShip, result.getDamageToHull());
                }
            }
        }
    }
}
