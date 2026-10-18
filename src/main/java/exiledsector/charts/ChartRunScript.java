package exiledsector.charts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import org.apache.log4j.Logger;

public class ChartRunScript implements EveryFrameScript {

    private static final Logger LOG = Logger.getLogger(ChartRunScript.class);

    private String cachedRunId;
    private StarSystemAPI cachedPocket;
    private boolean closeArmed;
    private boolean failureReported;

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
            watch(Global.getSector(), run);
        } catch (RuntimeException | LinkageError e) {
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] The Star Chart watchdog failed; the run will be checked again next frame", e);
            }
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
            blockTransverseJump(playerFleet);
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
                } else if (run.recordLeftPocket() == ChartRunState.OPEN && sector.getEntityById(run.outerWormholeId()) == null) {
                    run.beginClosing();
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

    static void blockTransverseJump(CampaignFleetAPI playerFleet) {
        AbilityPlugin transverseJump = playerFleet.getAbility(Abilities.TRANSVERSE_JUMP);
        if (transverseJump == null) {
            return;
        }
        if (transverseJump.isActiveOrInProgress()) {
            transverseJump.deactivate();
        }
        transverseJump.forceDisable();
    }
}
