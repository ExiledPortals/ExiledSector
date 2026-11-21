package exiledsector.harness;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class DamageTally implements DamageListener {

    static final class Damage {

        private float hull;
        private float armor;
        private float shields;
        private float emp;

        void add(ApplyDamageResultAPI result) {
            hull += result.getDamageToHull();
            armor += result.getTotalDamageToArmor();
            shields += result.getDamageToShields();
            emp += result.getEmpDamage();
        }

        float total() {
            return hull + armor + shields;
        }

        JSONObject toJson(float combatSeconds) throws JSONException {
            return new JSONObject().put("total", total()).put("hull", hull).put("armor", armor).put("shields", shields).put("emp", emp)
                    .put("totalPerSecond", combatSeconds > 0f ? total() / combatSeconds : 0f);
        }
    }

    private final Map<ShipAPI, Integer> sideByShip = new HashMap<>();
    private final Damage playerSideTook = new Damage();
    private final Damage enemySideTook = new Damage();
    private final Map<String, Damage> shipsTook = new LinkedHashMap<>();

    void watch(CombatEngineAPI engine) {
        for (ShipAPI ship : engine.getShips()) {
            if (!sideByShip.containsKey(ship)) {
                sideByShip.put(ship, ship.getOwner());
                ship.addListener(this);
            }
        }
    }

    void stop() {
        for (ShipAPI ship : sideByShip.keySet()) {
            ship.removeListener(this);
        }
        sideByShip.clear();
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (!(target instanceof ShipAPI ship)) {
            return;
        }
        record(sideByShip.getOrDefault(ship, ship.getOwner()), ship.isFighter() ? null : CombatShips.describe(ship), result);
    }

    void record(int owner, String shipName, ApplyDamageResultAPI result) {
        (owner == CombatShips.ENEMY_SIDE ? enemySideTook : playerSideTook).add(result);
        if (shipName != null) {
            shipsTook.computeIfAbsent(shipName, name -> new Damage()).add(result);
        }
    }

    float enemySideTotal() {
        return enemySideTook.total();
    }

    float playerSideTotal() {
        return playerSideTook.total();
    }

    JSONObject toJson(float combatSeconds) throws JSONException {
        JSONObject ships = new JSONObject();
        for (Map.Entry<String, Damage> entry : shipsTook.entrySet()) {
            ships.put(entry.getKey(), entry.getValue().toJson(combatSeconds));
        }
        return new JSONObject().put("combatSeconds", combatSeconds).put("enemySideTook", enemySideTook.toJson(combatSeconds))
                .put("playerSideTook", playerSideTook.toJson(combatSeconds)).put("shipsTook", ships);
    }
}
