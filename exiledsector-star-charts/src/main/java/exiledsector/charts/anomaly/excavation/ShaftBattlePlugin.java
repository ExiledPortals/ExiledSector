package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;

import java.util.List;

public class ShaftBattlePlugin extends BaseEveryFrameCombatPlugin {

    static final float CHECK_SECONDS = 1f;

    private CombatEngineAPI engine;
    private float secondsSinceCheck;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        secondsSinceCheck = 0f;
        holdLargeHullsAbove(engine);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) {
            return;
        }
        secondsSinceCheck += amount;
        if (secondsSinceCheck < CHECK_SECONDS) {
            return;
        }
        secondsSinceCheck = 0f;
        holdLargeHullsAbove(engine);
    }

    static int holdLargeHullsAbove(CombatEngineAPI engine) {
        if (engine == null) {
            return 0;
        }
        int held = 0;
        for (FleetSide side : FleetSide.values()) {
            CombatFleetManagerAPI manager = engine.getFleetManager(side);
            if (manager == null) {
                continue;
            }
            for (FleetMemberAPI member : manager.getReservesCopy()) {
                if (holdsAbove(member)) {
                    manager.removeFromReserves(member);
                    held++;
                }
            }
        }
        return held;
    }

    static boolean holdsAbove(FleetMemberAPI member) {
        return member != null && member.getHullSpec() != null && ExcavationRules.holdsAbove(member.getHullSpec().getHullSize());
    }
}
