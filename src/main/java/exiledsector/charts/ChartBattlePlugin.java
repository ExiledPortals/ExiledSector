package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ChartBattlePlugin extends BaseEveryFrameCombatPlugin {

    public static final String MOD_ID = ChartFleetConditions.MOD_ID;
    static final float REFRESH_SECONDS = 1f;
    static final int ENEMY_OWNER = 1;
    private static final Logger LOG = Logger.getLogger(ChartBattlePlugin.class);
    private static final List<PersonAPI> MODIFIED_COMMANDERS = new ArrayList<>();
    private static boolean failureReported;

    private CombatEngineAPI engine;
    private boolean active;
    private boolean started;
    private float secondsSinceRefresh;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        started = false;
        secondsSinceRefresh = 0f;
        active = engine != null && engine.isInCampaign() && !engine.isSimulation() && !engine.isInCampaignSim() && hasBattleConditions();
    }

    static boolean hasBattleConditions() {
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
        boolean firstFrame = !started;
        started = true;
        try {
            refresh(firstFrame);
        } catch (RuntimeException | LinkageError e) {
            active = false;
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] Applying the Star Chart battle drawbacks failed; they are off for this battle", e);
            }
        }
    }

    void refresh(boolean firstFrame) {
        if (engine.isCombatOver()) {
            liftCommanderBonuses();
            active = false;
            return;
        }
        refreshDeploymentPoints(FleetSide.PLAYER, ChartFleetConditions.mult(ChartFleetStat.DEPLOYMENT_POINTS), firstFrame);
        refreshDeploymentPoints(FleetSide.ENEMY, ChartFleetConditions.mult(ChartFleetStat.ENEMY_DEPLOYMENT_POINTS), firstFrame);
        refreshCommandPointRate(ChartFleetConditions.mult(ChartFleetStat.COMMAND_POINT_RATE));
        refreshEnemyObjectiveCaptureRate(ChartFleetConditions.mult(ChartFleetStat.ENEMY_OBJECTIVE_CAPTURE_RATE));
    }

    void refreshDeploymentPoints(FleetSide side, float mult, boolean firstFrame) {
        if (mult == 1f) {
            return;
        }
        CombatFleetManagerAPI manager = engine.getFleetManager(side);
        PersonAPI commander = manager == null ? null : manager.getFleetCommanderPreferPlayer();
        int battleSize = Global.getSettings().getBattleSize();
        int currentMax = manager == null ? 0 : manager.getMaxStrength();
        if (commander == null || battleSize <= 0 || currentMax <= 0) {
            return;
        }
        StatBonus fractionBonus = commander.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_FRACTION_OF_BATTLE_SIZE_BONUS_MOD);
        StatMod ours = fractionBonus.getFlatBonus(MOD_ID);
        int ourPoints = ours == null ? 0 : Math.round(battleSize * ours.getValue());
        int maxWithoutOurs = Math.max(1, currentMax - ourPoints);
        int wantedPoints = Math.max(1 - maxWithoutOurs, Math.round(maxWithoutOurs * (mult - 1f)));
        if (ours != null && wantedPoints == ourPoints) {
            return;
        }
        fractionBonus.modifyFlat(MOD_ID, wantedPoints / (float) battleSize);
        rememberCommander(commander);
        if (firstFrame) {
            manager.setMaxStrength(maxWithoutOurs + wantedPoints);
        }
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

    private static void rememberCommander(PersonAPI commander) {
        for (PersonAPI known : MODIFIED_COMMANDERS) {
            if (known == commander) {
                return;
            }
        }
        MODIFIED_COMMANDERS.add(commander);
    }

    public static boolean hasCommanderBonuses() {
        return !MODIFIED_COMMANDERS.isEmpty();
    }

    public static void liftCommanderBonuses() {
        for (PersonAPI commander : MODIFIED_COMMANDERS) {
            liftFrom(commander);
        }
        MODIFIED_COMMANDERS.clear();
    }

    public static void liftFrom(PersonAPI person) {
        if (person != null && person.getStats() != null) {
            person.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).unmodify(MOD_ID);
        }
    }
}
