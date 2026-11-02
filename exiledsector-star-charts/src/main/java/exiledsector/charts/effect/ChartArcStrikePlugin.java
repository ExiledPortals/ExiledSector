package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.charts.content.ChartSites;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class ChartArcStrikePlugin extends BaseEveryFrameCombatPlugin {

    static final int INTERVAL_SECONDS = 8;
    static final float ORIGIN_DISTANCE = 400f;
    static final float ARC_RANGE = 100000f;
    static final float ARC_THICKNESS = 30f;
    static final String IMPACT_SOUND = "tachyon_lance_emp_impact";
    static final Color ARC_FRINGE = new Color(110, 130, 255, 255);
    static final Color ARC_CORE = new Color(235, 240, 255, 255);
    private static final Logger LOG = Logger.getLogger(ChartArcStrikePlugin.class);
    private static boolean failureReported;

    private final Random random;
    private CombatEngineAPI engine;
    private float emp;
    private float secondsToStrike;

    public ChartArcStrikePlugin() {
        this(new Random());
    }

    ChartArcStrikePlugin(Random random) {
        this.random = random;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        secondsToStrike = INTERVAL_SECONDS;
        boolean campaignBattle = engine != null && engine.isInCampaign() && !engine.isSimulation() && !engine.isInCampaignSim();
        emp = campaignBattle ? ChartPlayerModifiers.arcStrikeEmp() : 0f;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (emp <= 0f || engine.isPaused() || engine.isCombatOver() || amount <= 0f) {
            return;
        }
        secondsToStrike -= amount;
        if (secondsToStrike > 0f) {
            return;
        }
        secondsToStrike += INTERVAL_SECONDS;
        try {
            strike(pickTarget(engine.getShips(), random));
        } catch (RuntimeException | LinkageError e) {
            emp = 0f;
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] The Star Chart's EMP arc strikes failed; they are off for this battle", e);
            }
        }
    }

    static ShipAPI pickTarget(List<ShipAPI> ships, Random random) {
        List<ShipAPI> targets = ships.stream().filter(ChartArcStrikePlugin::canBeStruck).toList();
        return targets.isEmpty() ? null : targets.get(random.nextInt(targets.size()));
    }

    static boolean canBeStruck(ShipAPI ship) {
        return ship != null && ship.getOwner() == ChartBattlePlugin.PLAYER_OWNER && ship.isAlive() && !ship.isHulk() && !ship.isFighter()
                && !ship.isShuttlePod() && !ship.isPhased() && ship.getParentStation() == null;
    }

    private void strike(ShipAPI target) {
        if (target == null) {
            return;
        }
        Vector2f origin = ChartSites.pointAround(target.getLocation(), random.nextDouble() * Math.PI * 2d,
                target.getCollisionRadius() + ORIGIN_DISTANCE);
        engine.spawnEmpArcPierceShields(null, origin, null, target, DamageType.ENERGY, 0f, emp, ARC_RANGE, IMPACT_SOUND, ARC_THICKNESS,
                ARC_FRINGE, ARC_CORE);
    }
}
