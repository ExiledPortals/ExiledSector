package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.compat.CombatGlow;
import exiledsector.charts.run.ChartRun;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public class StormBattlePlugin extends BaseEveryFrameCombatPlugin {

    static final float ARC_RANGE = 100000f;
    static final float ARC_THICKNESS = 30f;
    static final String IMPACT_SOUND = "tachyon_lance_emp_impact";
    static final String THUNDER_SOUND = "terrain_hyperspace_lightning";
    static final float LIGHT_SIZE = 600f;
    static final float LIGHT_INTENSITY = 1f;
    static final float LIGHT_SECONDS = 0.5f;
    static final Color ARC_FRINGE = new Color(110, 130, 255, 255);
    static final Color ARC_CORE = new Color(235, 240, 255, 255);
    private static final Logger LOG = Logger.getLogger(StormBattlePlugin.class);
    private static boolean failureReported;

    private CombatEngineAPI engine;
    private boolean active;
    private int tier;
    private float strikeSeconds;
    private float secondsToStrike;
    private Random random;
    private StormOverlay overlay;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        active = false;
        overlay = null;
        try {
            if (engine == null || !engine.isInCampaign() || engine.isSimulation() || engine.isInCampaignSim()) {
                return;
            }
            StormFrontTerrainPlugin front = StormBattles.playerFront();
            if (front == null) {
                return;
            }
            ChartRun run = StormBattles.runOf(front);
            tier = run == null ? 1 : run.tier();
            strikeSeconds = StormRules.strikeSeconds(StormBattles.contextOf(run).anomalyMult(StormFront.STRIKE_KEY));
            secondsToStrike = strikeSeconds;
            random = new Random();
            overlay = new StormOverlay(engine.getMapWidth(), engine.getMapHeight(), random.nextLong());
            engine.addLayeredRenderingPlugin(overlay);
            active = true;
        } catch (RuntimeException | LinkageError e) {
            reportFailure(e);
        }
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!active || engine == null || engine.isPaused() || amount <= 0f) {
            return;
        }
        secondsToStrike -= amount;
        if (secondsToStrike > 0f) {
            return;
        }
        secondsToStrike += strikeSeconds;
        try {
            strike();
        } catch (RuntimeException | LinkageError e) {
            active = false;
            reportFailure(e);
        }
    }

    void strike() {
        ShipAPI target = StormStrikes.pickTarget(engine.getShips(), random);
        if (target == null) {
            return;
        }
        Vector2f origin = StormStrikes.origin(target.getLocation(), random);
        engine.spawnEmpArc(null, origin, null, target, DamageType.ENERGY, StormRules.strikeDamage(tier), StormRules.strikeEmp(tier), ARC_RANGE,
                IMPACT_SOUND, ARC_THICKNESS, ARC_FRINGE, ARC_CORE);
        Global.getSoundPlayer().playSound(THUNDER_SOUND, 1f, 1f, origin, new Vector2f());
        overlay.flash(target.getLocation());
        CombatGlow.flash(new Vector2f(target.getLocation()), null, ARC_FRINGE, LIGHT_SIZE, LIGHT_INTENSITY, LIGHT_SECONDS);
    }

    private static void reportFailure(Throwable failure) {
        if (!failureReported) {
            failureReported = true;
            LOG.error("[ExiledSector] The Storm Front's battle strikes failed; they are off for this battle", failure);
        }
    }
}
