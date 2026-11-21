package exiledsector.harness;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.json.JSONObject;

final class RecordStatsStep implements HarnessStep {

    private final String shipSelector;
    private final String dataName;

    RecordStatsStep(String shipSelector, String dataName) {
        this.shipSelector = shipSelector;
        this.dataName = dataName;
    }

    @Override
    public String name() {
        return "recordStats " + shipSelector + " as " + dataName;
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        CombatEngineAPI engine = SimulatorAccess.combat();
        if (engine == null) {
            return StepOutcome.failed("recordStats only works in combat; run a simulate step first");
        }
        ShipAPI ship = CombatShips.find(engine, shipSelector);
        if (ship == null) {
            return new StepOutcome(false, false, "no ship in the combat matches \"" + shipSelector + "\"; use player, enemy, a hull id, a variant id or a ship name");
        }
        JSONObject stats = ShipStatsDump.of(ship);
        context.results().addData(dataName, stats);
        return StepOutcome.passed(CombatShips.describe(ship) + " has " + stats.getInt("modifiedStatCount") + " modified stats");
    }
}
