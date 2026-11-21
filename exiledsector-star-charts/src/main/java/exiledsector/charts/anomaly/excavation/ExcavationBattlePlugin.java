package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import exiledsector.charts.run.ChartRuns;
import org.apache.log4j.Logger;

import java.util.List;

public class ExcavationBattlePlugin extends BaseEveryFrameCombatPlugin {

    static final float CHECK_SECONDS = 1f;
    static final String CAVERN_KEY = "exiledSector_shaftCavern";
    static final String SIGHT_MOD_ID = "exiledSector_shaftDarkness";
    private static final Logger LOG = Logger.getLogger(ExcavationBattlePlugin.class);

    private CombatEngineAPI engine;
    private float secondsSinceCheck;
    private int maxShipSize = ExcavationRules.MAX_SHIP_SIZE;
    private float sightRadiusPercent;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = isExcavationBattle(engine) ? engine : null;
        secondsSinceCheck = 0f;
        if (this.engine == null) {
            return;
        }
        if (!onCavernMap(engine)) {
            LOG.warn("[ExiledSector] A shaft fight started without the cavern map; another battle plugin was used, so cruisers and capitals are held back here");
        }
        ExcavationTuning tuning = liveTuning();
        maxShipSize = ExcavationRules.maxShipSize(tuning);
        sightRadiusPercent = tuning.sightRadiusPercent();
        checkField(engine);
    }

    static ExcavationTuning liveTuning() {
        return ExcavationTuning.ofRun(ChartRuns.live());
    }

    private void checkField(CombatEngineAPI engine) {
        holdLargeHullsAbove(engine, maxShipSize);
        darken(engine, sightRadiusPercent);
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
        checkField(engine);
    }

    static boolean isExcavationBattle(CombatEngineAPI engine) {
        BattleCreationContext context = engine == null ? null : engine.getContext();
        return context != null && ExcavationEncounters.isExcavationBattle(context.getOtherFleet());
    }

    static boolean onCavernMap(CombatEngineAPI engine) {
        return engine.getCustomData() != null && engine.getCustomData().containsKey(CAVERN_KEY);
    }

    static int holdLargeHullsAbove(CombatEngineAPI engine, int maxShipSize) {
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
                if (holdsAbove(member, maxShipSize)) {
                    manager.removeFromReserves(member);
                    held++;
                }
            }
        }
        return held;
    }

    static boolean holdsAbove(FleetMemberAPI member, int maxShipSize) {
        return member != null && member.getHullSpec() != null && ExcavationRules.holdsAbove(member.getHullSpec().getHullSize(), maxShipSize);
    }

    static int darken(CombatEngineAPI engine, float sightRadiusPercent) {
        if (engine == null || sightRadiusPercent == 0f || engine.getShips() == null) {
            return 0;
        }
        int darkened = 0;
        for (ShipAPI ship : engine.getShips()) {
            if (ship != null && ship.getMutableStats() != null) {
                ship.getMutableStats().getSightRadiusMod().modifyPercent(SIGHT_MOD_ID, sightRadiusPercent);
                darkened++;
            }
        }
        return darkened;
    }
}
