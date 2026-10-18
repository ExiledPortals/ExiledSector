package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import org.apache.log4j.Logger;

public class ChartRunScript implements EveryFrameScript {

    static final float OBJECTIVE_CHECK_SECONDS = 1f;
    private static final Logger LOG = Logger.getLogger(ChartRunScript.class);

    private String cachedRunId;
    private StarSystemAPI cachedPocket;
    private boolean closeArmed;
    private boolean failureReported;
    private float secondsSinceObjectiveCheck;

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
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null) {
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
        StarSystemAPI pocket = pocketFor(sector, run);
        boolean inTransition = playerFleet.isInHyperspaceTransition();
        boolean inside = pocket != null && playerFleet.getContainingLocation() == pocket;
        if (inside) {
            blockEscapeAbilities(playerFleet);
        }
        if (run.runState() == ChartRunState.CLOSED) {
            ChartRuns.delete(run.runId());
            forget();
            return;
        }
        if (pocket == null && run.runState().isLive() && !inTransition) {
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
                } else if (run.recordLeftPocket() == ChartRunState.OPEN) {
                    ChartRunCloser.clearPlayerTargetAndCourse(new ChartRunCloser.CloseTarget(sector, run.runId(), pocket));
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

    private StarSystemAPI pocketFor(SectorAPI sector, ChartRun run) {
        if (!run.runId().equals(cachedRunId)) {
            cachedRunId = run.runId();
            cachedPocket = sector.getStarSystem(run.pocketSystemId());
            closeArmed = false;
        }
        return cachedPocket;
    }

    private void forget() {
        cachedRunId = null;
        cachedPocket = null;
        closeArmed = false;
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
