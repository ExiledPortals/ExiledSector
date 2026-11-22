package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class DealtHullDamage implements DamageListener {

    @FunctionalInterface
    public interface Receiver {
        void onHullDamageDealt(ShipAPI target, float hullDamage);
    }

    private final Map<ShipAPI, List<Receiver>> receiversByDealer = new IdentityHashMap<>();

    public static void register(ShipAPI dealer, Receiver receiver) {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT || Global.getCombatEngine() == null) {
            return;
        }
        SkillEffectSupport.ensureEngineListener(DealtHullDamage.class, DealtHullDamage::new).add(dealer, receiver);
    }

    void add(ShipAPI dealer, Receiver receiver) {
        receiversByDealer.computeIfAbsent(dealer, ship -> new ArrayList<>(2)).add(receiver);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (source instanceof ShipAPI dealer && target instanceof ShipAPI targetShip && result.getDamageToHull() > 0f) {
            List<Receiver> receivers = receiversByDealer.get(dealer);
            if (receivers != null) {
                for (Receiver receiver : receivers) {
                    receiver.onHullDamageDealt(targetShip, result.getDamageToHull());
                }
            }
        }
    }
}
