package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChartObjectives {

    static final Set<FleetDespawnReason> DEFEAT_REASONS = Set.of(FleetDespawnReason.DESTROYED_BY_BATTLE, FleetDespawnReason.NO_MEMBERS);
    private static final Logger LOG = Logger.getLogger(ChartObjectives.class);

    private ChartObjectives() {
    }

    static ChartRun trackedRun() {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || !run.runState().isLive() || run.isObjectiveComplete() || run.objective() == null) {
            return null;
        }
        return run;
    }

    public static boolean fleetGone(SectorAPI sector, CampaignFleetAPI fleet, FleetDespawnReason reason) {
        ChartRun run = trackedRun();
        if (run == null || fleet == null || !DEFEAT_REASONS.contains(reason) || !ChartIds.belongsToRun(fleet, run.runId())) {
            return false;
        }
        if (!run.recordTargetGone(fleet.getId())) {
            return false;
        }
        progressed(sector, run);
        return true;
    }

    public static boolean reconcile(SectorAPI sector) {
        ChartRun run = trackedRun();
        if (run == null) {
            return false;
        }
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        if (system == null) {
            return false;
        }
        boolean changed = false;
        Set<String> remainingFleetIds = new HashSet<>();
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (!fleet.isDespawning() && !fleet.isEmpty()) {
                remainingFleetIds.add(fleet.getId());
            }
        }
        List<String> defeated = run.defeatedTargetIds();
        for (String targetId : run.objectiveTargetIds()) {
            if (defeated.contains(targetId)) {
                continue;
            }
            if (!remainingFleetIds.contains(targetId) && run.recordTargetGone(targetId)) {
                changed = true;
            }
        }
        if (changed) {
            progressed(sector, run);
        }
        return changed;
    }

    private static void progressed(SectorAPI sector, ChartRun run) {
        updateIntel(sector, run);
        if (!run.markObjectiveComplete()) {
            return;
        }
        try {
            ChartRewards.grant(sector, run);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Delivering the Star Chart reward for run " + run.runId() + " failed", e);
        }
        updateIntel(sector, run);
    }

    private static void updateIntel(SectorAPI sector, ChartRun run) {
        if (sector.getIntelManager() == null) {
            return;
        }
        for (IntelInfoPlugin intel : sector.getIntelManager().getIntel(ChartObjectiveIntel.class)) {
            if (intel instanceof ChartObjectiveIntel objectiveIntel && objectiveIntel.belongsToRun(run.runId())) {
                try {
                    objectiveIntel.sendUpdateIfPlayerHasIntel(null, false);
                } catch (RuntimeException e) {
                    LOG.debug("Updating the Star Chart objective intel failed", e);
                }
            }
        }
    }
}
