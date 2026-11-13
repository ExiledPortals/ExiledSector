package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;

import java.util.IdentityHashMap;
import java.util.Map;

final class EmpDealtHardFluxVentListener extends ShipCombatListener {

    static final String VENT_PERCENT_KEY = "exiledSector_empDealtHardFluxVentPercent";

    EmpDealtHardFluxVentListener(ShipAPI ownerShip) {
        super(ownerShip);
        DealtEmpRouter.register(this);
    }

    void onEmpDealt(ShipAPI target, float emp) {
        if (target.isHulk() || !CombatQueries.isHostile(ownerShip, target) || !ownerIsAliveNotHulk()) {
            return;
        }
        FluxTrackerAPI flux = ownerShip.getFluxTracker();
        if (flux != null) {
            EmpHardFluxVentListener.ventHardFlux(flux, emp * magnitude(VENT_PERCENT_KEY) / 100f);
        }
    }

    static final class DealtEmpRouter implements DamageListener {

        private final Map<ShipAPI, EmpDealtHardFluxVentListener> listenersByDealer = new IdentityHashMap<>();

        static void register(EmpDealtHardFluxVentListener listener) {
            if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT || Global.getCombatEngine() == null) {
                return;
            }
            SkillEffectSupport.ensureEngineListener(DealtEmpRouter.class, DealtEmpRouter::new).listenersByDealer.put(listener.ownerShip, listener);
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (source instanceof ShipAPI dealer && target instanceof ShipAPI targetShip && result.getEmpDamage() > 0f) {
                EmpDealtHardFluxVentListener listener = listenersByDealer.get(dealer);
                if (listener != null) {
                    listener.onEmpDealt(targetShip, result.getEmpDamage());
                }
            }
        }
    }
}
