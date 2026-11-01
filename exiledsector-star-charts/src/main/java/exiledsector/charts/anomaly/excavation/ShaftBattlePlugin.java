package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;

import java.util.List;

public class ShaftBattlePlugin extends BaseEveryFrameCombatPlugin {

    static final float CHECK_SECONDS = 1f;
    static final String CAVERN_KEY = "exiledSector_shaftCavern";
    private static final Logger LOG = Logger.getLogger(ShaftBattlePlugin.class);

    private CombatEngineAPI engine;
    private float secondsSinceCheck;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = isShaftBattle(engine) ? engine : null;
        secondsSinceCheck = 0f;
        if (this.engine == null) {
            return;
        }
        if (!onCavernMap(engine)) {
            LOG.warn("[ExiledSector] A shaft fight started without the cavern map; another battle plugin was used, so cruisers and capitals are held back here");
        }
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

    static boolean isShaftBattle(CombatEngineAPI engine) {
        BattleCreationContext context = engine == null ? null : engine.getContext();
        return context != null && ShaftEncounters.isShaftBattle(context.getOtherFleet());
    }

    static boolean onCavernMap(CombatEngineAPI engine) {
        return engine.getCustomData() != null && engine.getCustomData().containsKey(CAVERN_KEY);
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
