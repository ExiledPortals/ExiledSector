package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

final class HeldObjective {

    static final float RANGE = 1500f;

    private HeldObjective() {
    }

    static boolean isNearby(ShipAPI ship) {
        return isNearby(ship, Global.getCombatEngine());
    }

    static boolean isNearby(ShipAPI ship, CombatEngineAPI engine) {
        if (engine == null || engine.getObjectives() == null) {
            return false;
        }
        for (BattleObjectiveAPI objective : engine.getObjectives()) {
            if (objective.getOwner() == ship.getOwner()
                    && Misc.getDistanceSq(objective.getLocation(), ship.getLocation()) <= RANGE * RANGE) {
                return true;
            }
        }
        return false;
    }
}
