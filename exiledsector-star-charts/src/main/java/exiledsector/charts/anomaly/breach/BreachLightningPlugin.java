package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.ModSounds;
import exiledsector.PlayerFleet;
import exiledsector.charts.effect.ChartCombatPlugins;
import exiledsector.charts.layout.ChartGeometry;
import exiledsector.compat.FirstFailureLog;
import exiledsector.skills.skilleffect.CombatQueries;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class BreachLightningPlugin extends BaseEveryFrameCombatPlugin {

    static final int PLAYER_OWNER = 0;
    static final int ENEMY_OWNER = 1;
    static final float ORIGIN_DISTANCE = 400f;
    static final float ARC_RANGE = 100000f;
    static final float ARC_THICKNESS = 30f;
    static final String IMPACT_SOUND = "tachyon_lance_emp_impact";
    static final Color ARC_FRINGE = new Color(170, 90, 255, 255);
    static final Color ARC_CORE = new Color(240, 225, 255, 255);
    private static final FirstFailureLog FAILURES = FirstFailureLog.errors(Logger.getLogger(BreachLightningPlugin.class));

    private final Random random;
    private CombatEngineAPI engine;
    private boolean active;
    private float secondsToStrike;

    public BreachLightningPlugin() {
        this(new Random());
    }

    BreachLightningPlugin(Random random) {
        this.random = random;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        secondsToStrike = BreachRules.LIGHTNING_INTERVAL_SECONDS;
        try {
            active = ChartCombatPlugins.isCampaignBattle(engine) && playerInLightningBreach();
        } catch (RuntimeException | LinkageError e) {
            active = false;
            FAILURES.report("[ExiledSector] Checking for hyperspace lightning in a Spatial Breach failed; it is off for this battle", e);
        }
    }

    static boolean playerInLightningBreach() {
        CampaignFleetAPI player = PlayerFleet.orNull();
        BreachTerrainPlugin breach = player == null ? null : SpatialBreach.breachIn(player.getContainingLocation());
        return breach != null && breach.holds(player) && BreachLines.on(BreachLines.contextIn(player.getContainingLocation()), BreachLines.LIGHTNING_KEY);
    }

    boolean isActive() {
        return active;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!active || engine == null || engine.isPaused() || engine.isCombatOver() || amount <= 0f) {
            return;
        }
        secondsToStrike -= amount;
        if (secondsToStrike > 0f) {
            return;
        }
        secondsToStrike += BreachRules.LIGHTNING_INTERVAL_SECONDS;
        try {
            strike(pickTarget(engine.getShips(), random));
        } catch (RuntimeException | LinkageError e) {
            active = false;
            FAILURES.report("[ExiledSector] The Spatial Breach's hyperspace lightning failed; it is off for this battle", e);
        }
    }

    static ShipAPI pickTarget(List<ShipAPI> ships, Random random) {
        List<ShipAPI> targets = ships.stream().filter(BreachLightningPlugin::canBeStruck).toList();
        return targets.isEmpty() ? null : targets.get(random.nextInt(targets.size()));
    }

    static boolean canBeStruck(ShipAPI ship) {
        return ship != null && (ship.getOwner() == PLAYER_OWNER || ship.getOwner() == ENEMY_OWNER) && ship.isAlive() && !ship.isHulk()
                && !ship.isFighter() && !ship.isShuttlePod() && !ship.isPhased() && ship.getParentStation() == null && !CombatQueries.isAfterimage(ship);
    }

    private void strike(ShipAPI target) {
        if (target == null) {
            return;
        }
        Vector2f origin = ChartGeometry.pointAtRandomAngle(target.getLocation(), target.getCollisionRadius() + ORIGIN_DISTANCE, random);
        engine.spawnEmpArcPierceShields(null, origin, null, target, DamageType.ENERGY, BreachRules.LIGHTNING_DAMAGE, BreachRules.LIGHTNING_EMP, ARC_RANGE,
                ModSounds.orSilent(IMPACT_SOUND), ARC_THICKNESS, ARC_FRINGE, ARC_CORE);
    }
}
