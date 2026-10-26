package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;

import java.util.List;

public class ChartBattlePlugin extends BaseEveryFrameCombatPlugin {

    public static final String MOD_ID = ChartFleetConditions.MOD_ID;
    static final float REFRESH_SECONDS = 1f;
    static final int PLAYER_OWNER = 0;
    static final int ENEMY_OWNER = 1;
    private static final Logger LOG = Logger.getLogger(ChartBattlePlugin.class);
    private static boolean failureReported;

    private CombatEngineAPI engine;
    private boolean active;
    private boolean started;
    private float secondsSinceRefresh;
    private boolean capsFrozen;
    private int playerBaseCap;
    private int enemyBaseCap;
    private int appliedPlayerCap;
    private int appliedEnemyCap;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        started = false;
        secondsSinceRefresh = 0f;
        capsFrozen = false;
        appliedPlayerCap = -1;
        appliedEnemyCap = -1;
        active = engine != null && engine.isInCampaign() && !engine.isSimulation() && !engine.isInCampaignSim() && hasBattleConditions();
    }

    static boolean hasBattleConditions() {
        if (ChartFleetConditions.battleSizeBonus() != 0) {
            return true;
        }
        for (ChartFleetStat stat : ChartFleetConditions.active().keySet()) {
            if (!stat.appliesToShips()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!active) {
            return;
        }
        secondsSinceRefresh += amount;
        if (started && secondsSinceRefresh < REFRESH_SECONDS) {
            return;
        }
        secondsSinceRefresh = 0f;
        started = true;
        try {
            refresh();
        } catch (RuntimeException | LinkageError e) {
            active = false;
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] Applying the Star Chart battle drawbacks failed; they are off for this battle", e);
            }
        }
    }

    void refresh() {
        refreshBattleCaps(ChartFleetConditions.battleSizeBonus(), ChartFleetConditions.mult(ChartFleetStat.ENEMY_DEPLOYMENT_COST));
        refreshCommandPointRate(ChartFleetConditions.mult(ChartFleetStat.COMMAND_POINT_RATE));
        refreshEnemyObjectiveCaptureRate(ChartFleetConditions.mult(ChartFleetStat.ENEMY_OBJECTIVE_CAPTURE_RATE));
        refreshEnemyDamageDealt();
    }

    void refreshBattleCaps(int bonusPoints, float enemyCostMult) {
        if (bonusPoints == 0 && enemyCostMult == 1f || !capsFrozen && !freezeBaseCaps(bonusPoints)) {
            return;
        }
        int playerCap = Math.max(1, playerBaseCap + objectiveBonus(PLAYER_OWNER));
        int enemyCap = Math.max(1, Math.round((enemyBaseCap + objectiveBonus(ENEMY_OWNER)) / enemyCostMult));
        if (playerCap != appliedPlayerCap) {
            engine.setMaxFleetPoints(FleetSide.PLAYER, playerCap);
            appliedPlayerCap = playerCap;
        }
        if (enemyCap != appliedEnemyCap) {
            engine.setMaxFleetPoints(FleetSide.ENEMY, enemyCap);
            appliedEnemyCap = enemyCap;
        }
    }

    private boolean freezeBaseCaps(int bonusPoints) {
        CombatFleetManagerAPI playerManager = engine.getFleetManager(FleetSide.PLAYER);
        CombatFleetManagerAPI enemyManager = engine.getFleetManager(FleetSide.ENEMY);
        int playerMax = playerManager == null ? 0 : playerManager.getMaxStrength();
        int enemyMax = enemyManager == null ? 0 : enemyManager.getMaxStrength();
        if (playerMax <= 0 || enemyMax <= 0) {
            return false;
        }
        int playerShare = Math.round(bonusPoints * playerMax / (float) (playerMax + enemyMax));
        playerBaseCap = playerMax + playerShare - objectiveBonus(PLAYER_OWNER);
        enemyBaseCap = enemyMax + bonusPoints - playerShare - objectiveBonus(ENEMY_OWNER);
        capsFrozen = true;
        return true;
    }

    private int objectiveBonus(int owner) {
        List<BattleObjectiveAPI> objectives = engine.getObjectives();
        if (objectives == null || objectives.isEmpty()) {
            return 0;
        }
        int battleSize = Global.getSettings().getBattleSize();
        int bonus = 0;
        for (BattleObjectiveAPI objective : objectives) {
            if (objective.getOwner() == owner) {
                bonus += Math.round(battleSize * objective.getBattleSizeFractionBonus());
            }
        }
        return bonus;
    }

    void refreshCommandPointRate(float mult) {
        if (mult == 1f) {
            return;
        }
        CombatFleetManagerAPI manager = engine.getFleetManager(FleetSide.PLAYER);
        CombatTaskManagerAPI taskManager = manager == null ? null : manager.getTaskManager(false);
        if (taskManager == null) {
            return;
        }
        MutableStat rateModifier = taskManager.getCPRateModifier();
        StatMod ours = rateModifier.getFlatStatMod(MOD_ID);
        float rateWithoutOurs = taskManager.getCPRateMult() - (ours == null ? 0f : ours.getValue());
        rateModifier.modifyFlat(MOD_ID, rateWithoutOurs > 0f ? rateWithoutOurs * (mult - 1f) : 0f);
    }

    void refreshEnemyObjectiveCaptureRate(float mult) {
        if (mult == 1f) {
            return;
        }
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() == ENEMY_OWNER && !ship.isFighter()) {
                ship.getMutableStats().getDynamic().getStat(Stats.SHIP_OBJECTIVE_CAP_RATE_MULT).modifyMult(MOD_ID, mult);
            }
        }
    }

    void refreshEnemyDamageDealt() {
        if (!ChartEnemyDamageListener.anyActive()) {
            return;
        }
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() == ENEMY_OWNER && !ship.hasListenerOfClass(ChartEnemyDamageListener.class)) {
                ship.addListener(ChartEnemyDamageListener.INSTANCE);
            }
        }
    }
}
