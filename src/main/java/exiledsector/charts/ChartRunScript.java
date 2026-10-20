package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import exiledsector.i18n.Translation;
import org.apache.log4j.Logger;

public class ChartRunScript implements EveryFrameScript {

    static final float OBJECTIVE_CHECK_SECONDS = 1f;
    private static final Logger LOG = Logger.getLogger(ChartRunScript.class);

    private String cachedRunId;
    private StarSystemAPI cachedSystem;
    private boolean closeArmed;
    private boolean failureReported;
    private float secondsSinceObjectiveCheck;
    private String effectsRunId;
    private ChartGenContext cachedContext = ChartGenContext.none();
    private boolean sensorRangeApplied;
    private String sensorSourceText;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (ChartBattlePlugin.hasCommanderBonuses()) {
            ChartBattlePlugin.liftCommanderBonuses();
        }
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null) {
            if (sensorRangeApplied || ChartFleetConditions.isActive()) {
                liftRunEffects(Global.getSector().getPlayerFleet());
            }
            forget();
            return;
        }
        try {
            SectorAPI sector = Global.getSector();
            watch(sector, run);
            checkObjective(sector, run, amount);
        } catch (RuntimeException | LinkageError e) {
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] The Star Chart watchdog failed; the run will be checked again next frame", e);
            }
        }
    }

    void checkObjective(SectorAPI sector, ChartRun run, float amount) {
        secondsSinceObjectiveCheck += amount;
        if (secondsSinceObjectiveCheck < OBJECTIVE_CHECK_SECONDS) {
            return;
        }
        secondsSinceObjectiveCheck = 0f;
        if (run.runState() == ChartRunState.INSIDE && !run.isObjectiveComplete() && run.objective() != null) {
            ChartObjectives.reconcile(sector);
        }
    }

    void watch(SectorAPI sector, ChartRun run) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet == null || playerFleet.getContainingLocation() == null) {
            return;
        }
        StarSystemAPI system = systemFor(sector, run);
        boolean inTransition = playerFleet.isInHyperspaceTransition();
        boolean inside = system != null && playerFleet.getContainingLocation() == system;
        if (inside) {
            blockEscapeAbilities(playerFleet);
        }
        applyRunEffects(playerFleet, run, inside && run.runState().keepsRunEffects());
        if (run.runState() == ChartRunState.CLOSED) {
            ChartRuns.delete(run.runId());
            forget();
            return;
        }
        if (system == null && run.runState().isLive() && !inTransition) {
            run.beginClosing();
        }
        if (inTransition) {
            closeArmed = false;
            return;
        }
        switch (run.runState()) {
            case OPEN -> {
                if (inside && run.recordArrival()) {
                    ChartRunCloser.removeOuterWormhole(sector, run);
                }
            }
            case INSIDE -> {
                if (inside) {
                    run.cancelReturn();
                } else if (run.recordLeftSystem() == ChartRunState.OPEN) {
                    ChartRunCloser.clearPlayerTargetAndCourse(new ChartRunCloser.CloseTarget(sector, run.runId(), system));
                    if (sector.getEntityById(run.outerWormholeId()) == null) {
                        run.beginClosing();
                    }
                }
            }
            case CLOSING -> closeWhenClear(sector, run, inside);
            default -> {
            }
        }
    }

    private void closeWhenClear(SectorAPI sector, ChartRun run, boolean inside) {
        if (inside || sector.getCampaignUI() != null && sector.getCampaignUI().isShowingDialog()) {
            closeArmed = false;
            return;
        }
        if (!closeArmed) {
            closeArmed = true;
            return;
        }
        closeArmed = false;
        ChartRunCloser.close(sector, run);
        forget();
    }

    private StarSystemAPI systemFor(SectorAPI sector, ChartRun run) {
        if (!run.runId().equals(cachedRunId)) {
            cachedRunId = run.runId();
            cachedSystem = sector.getStarSystem(run.systemId());
            closeArmed = false;
        }
        return cachedSystem;
    }

    void applyRunEffects(CampaignFleetAPI playerFleet, ChartRun run, boolean inside) {
        if (!inside) {
            if (sensorRangeApplied || ChartFleetConditions.isActive()) {
                liftRunEffects(playerFleet);
            }
            return;
        }
        ChartGenContext context = contextFor(run);
        float sensorRangeMult = context.sensorRangeMult();
        if (sensorRangeMult != 1f) {
            ChartRunEffects.applySensorRange(playerFleet, sensorRangeMult, sensorSource());
            sensorRangeApplied = true;
        }
        ChartFleetConditions.set(context.fleetStatMults(), playerFleet);
    }

    private void liftRunEffects(CampaignFleetAPI playerFleet) {
        if (sensorRangeApplied) {
            ChartRunEffects.removeFrom(playerFleet);
            sensorRangeApplied = false;
        }
        ChartFleetConditions.clear(playerFleet);
    }

    private ChartGenContext contextFor(ChartRun run) {
        if (!run.runId().equals(effectsRunId)) {
            effectsRunId = run.runId();
            cachedContext = ChartRunEffects.contextOf(run);
        }
        return cachedContext;
    }

    private String sensorSource() {
        if (sensorSourceText == null) {
            sensorSourceText = Translation.gameText("chart.effect.sensorSource");
        }
        return sensorSourceText;
    }

    private void forget() {
        cachedRunId = null;
        cachedSystem = null;
        closeArmed = false;
        effectsRunId = null;
        cachedContext = ChartGenContext.none();
    }

    static void blockEscapeAbilities(CampaignFleetAPI playerFleet) {
        for (String abilityId : ChartBlockedAbilities.abilityIds()) {
            try {
                block(playerFleet.getAbility(abilityId));
            } catch (RuntimeException | LinkageError e) {
                ChartBlockedAbilities.reportBlockFailure(abilityId, e);
            }
        }
    }

    private static void block(AbilityPlugin ability) {
        if (ability == null) {
            return;
        }
        if (ability.isActiveOrInProgress()) {
            ability.deactivate();
        }
        ability.forceDisable();
    }
}
