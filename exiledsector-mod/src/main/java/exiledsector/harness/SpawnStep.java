package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class SpawnStep implements HarnessStep {

    static final float DEFAULT_DISTANCE = 2500f;
    static final float SPACING = 500f;
    static final float FACING_UP = 90f;
    static final float FACING_DOWN = 270f;
    private static final Vector2f SIMULATOR_PLAYER_START = new Vector2f(0f, -1500f);

    private final String variantId;
    private final FleetSide side;
    private final float distance;
    private final int count;

    SpawnStep(String variantId, FleetSide side, float distance, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("spawn needs a count of at least 1, not " + count);
        }
        this.variantId = variantId;
        this.side = side;
        this.distance = distance;
        this.count = count;
    }

    static FleetSide side(String sideName) {
        return switch (sideName.toLowerCase(Locale.ROOT)) {
            case CombatShips.ENEMY -> FleetSide.ENEMY;
            case CombatShips.PLAYER, "ally" -> FleetSide.PLAYER;
            default -> throw new IllegalArgumentException("spawn side must be enemy, player or ally, not \"" + sideName + "\"");
        };
    }

    @Override
    public String name() {
        return "spawn " + count + " " + variantId + " for the " + side.name().toLowerCase(Locale.ROOT) + " side";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        CombatEngineAPI engine = SimulatorAccess.combat();
        if (engine == null) {
            return StepOutcome.failed("spawn only works in combat; run a simulate step first");
        }
        if (!Global.getSettings().doesVariantExist(variantId)) {
            return StepOutcome.failed("no ship variant \"" + variantId + "\"");
        }
        ShipAPI playerShip = engine.getPlayerShip();
        Vector2f centre = new Vector2f(playerShip == null ? SIMULATOR_PLAYER_START : playerShip.getLocation());
        boolean enemySide = side == FleetSide.ENEMY;
        List<String> spawned = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Vector2f location = spawnLocation(centre, enemySide, distance, index, count);
            ShipAPI ship = engine.getFleetManager(side).spawnShipOrWing(variantId, location, enemySide ? FACING_DOWN : FACING_UP);
            if (ship == null) {
                return StepOutcome.failed("the game spawned nothing for " + variantId + (spawned.isEmpty() ? "" : " after " + String.join(", ", spawned)));
            }
            spawned.add(CombatShips.describe(ship));
        }
        return StepOutcome.passed("spawned " + String.join(", ", spawned));
    }

    static Vector2f spawnLocation(Vector2f centre, boolean enemySide, float distance, int index, int count) {
        float sideways = (index - (count - 1) / 2f) * SPACING;
        return enemySide ? new Vector2f(centre.x + sideways, centre.y + distance) : new Vector2f(centre.x + distance + sideways, centre.y);
    }
}
