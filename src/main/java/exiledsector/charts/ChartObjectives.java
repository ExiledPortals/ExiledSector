package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
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
        return run.recordTargetGone(fleet.getId()) && progressed(sector, run);
    }

    public static boolean lootShown(SectorAPI sector, SectorEntityToken salvaged) {
        ChartRun run = trackedRun();
        if (run == null || salvaged == null || run.vaultEntityId() == null || !run.vaultEntityId().equals(salvaged.getId())) {
            return false;
        }
        return run.recordTargetGone(salvaged.getId()) && progressed(sector, run);
    }

    public static boolean reconcile(SectorAPI sector) {
        ChartRun run = trackedRun();
        if (run == null) {
            return false;
        }
        StarSystemAPI pocket = sector.getStarSystem(run.pocketSystemId());
        if (pocket == null) {
            return false;
        }
        boolean changed = false;
        Set<String> remainingFleetIds = new HashSet<>();
        for (CampaignFleetAPI fleet : pocket.getFleets()) {
            if (!fleet.isDespawning() && !fleet.isEmpty()) {
                remainingFleetIds.add(fleet.getId());
            }
        }
        List<String> defeated = run.defeatedTargetIds();
        for (String targetId : run.objectiveTargetIds()) {
            if (defeated.contains(targetId)) {
                continue;
            }
            boolean gone = targetId.equals(run.vaultEntityId()) ? isSalvaged(pocket.getEntityById(targetId)) : !remainingFleetIds.contains(targetId);
            if (gone && run.recordTargetGone(targetId)) {
                changed = true;
            }
        }
        return changed && progressed(sector, run);
    }

    static boolean isSalvaged(SectorEntityToken vault) {
        return vault == null || vault.isExpired() || vault.hasTag(Tags.FADING_OUT_AND_EXPIRING);
    }

    private static boolean progressed(SectorAPI sector, ChartRun run) {
        updateIntel(sector, run);
        if (!run.markObjectiveComplete()) {
            return true;
        }
        try {
            ChartRewards.grant(sector, run);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Delivering the Star Chart reward for run " + run.runId() + " failed", e);
        }
        updateIntel(sector, run);
        return true;
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
