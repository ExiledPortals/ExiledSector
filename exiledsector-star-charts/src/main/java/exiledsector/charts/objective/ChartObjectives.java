package exiledsector.charts.objective;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.atlas.AtlasProgress;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;

public final class ChartObjectives {

    static final Set<FleetDespawnReason> DEFEAT_REASONS = Set.of(FleetDespawnReason.DESTROYED_BY_BATTLE, FleetDespawnReason.NO_MEMBERS);
    private static final Logger LOG = Logger.getLogger(ChartObjectives.class);

    private ChartObjectives() {
    }

    static ChartRun trackedRun() {
        ChartRun run = ChartRuns.live();
        if (run == null || run.isObjectiveComplete() || run.isObjectiveLost() || !run.hasBoss()) {
            return null;
        }
        return run;
    }

    public static boolean fleetGone(SectorAPI sector, CampaignFleetAPI fleet, FleetDespawnReason reason) {
        ChartRun run = trackedRun();
        if (run == null || fleet == null || !DEFEAT_REASONS.contains(reason) || !ChartIds.belongsToRun(fleet, run.runId())) {
            return false;
        }
        if (fleet.getId() != null && fleet.getId().equals(run.protectedFleetId()) || run.protectedFleetId() != null && protectedFallen(sector, run)) {
            lost(sector, run);
            return true;
        }
        if (!isBossFleet(run, fleet)) {
            return false;
        }
        completed(sector, run);
        return true;
    }

    private static boolean protectedFallen(SectorAPI sector, ChartRun run) {
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        if (system == null) {
            return false;
        }
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (run.protectedFleetId().equals(fleet.getId())) {
                return fleet.isDespawning() || fleet.isEmpty();
            }
        }
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
        boolean bossStanding = false;
        boolean protectedStanding = false;
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (fleet.isDespawning() || fleet.isEmpty()) {
                continue;
            }
            if (run.protectedFleetId() != null && run.protectedFleetId().equals(fleet.getId())) {
                protectedStanding = true;
            }
            if (isBossFleet(run, fleet)) {
                run.recordBossFlagship(flagshipIdOf(fleet));
                bossStanding = holdsMember(fleet, run.bossFlagshipId());
            }
        }
        if (run.protectedFleetId() != null && !protectedStanding) {
            lost(sector, run);
            return true;
        }
        if (bossStanding) {
            return false;
        }
        completed(sector, run);
        return true;
    }

    public static String flagshipIdOf(CampaignFleetAPI fleet) {
        if (fleet == null) {
            return null;
        }
        FleetMemberAPI flagship = fleet.getFlagship();
        if (flagship == null && fleet.getFleetData() != null) {
            List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
            flagship = members == null || members.isEmpty() ? null : members.get(0);
        }
        return flagship == null ? null : flagship.getId();
    }

    private static boolean isBossFleet(ChartRun run, CampaignFleetAPI fleet) {
        return run.bossFleetId().equals(fleet.getId());
    }

    private static boolean holdsMember(CampaignFleetAPI fleet, String memberId) {
        if (memberId == null || fleet.getFleetData() == null) {
            return true;
        }
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (memberId.equals(member.getId())) {
                return true;
            }
        }
        return false;
    }

    private static void lost(SectorAPI sector, ChartRun run) {
        if (run.markObjectiveLost()) {
            updateIntel(sector, run);
        }
    }

    private static void completed(SectorAPI sector, ChartRun run) {
        if (!run.markObjectiveComplete()) {
            return;
        }
        AtlasProgress.completed(sector, run);
        try {
            ChartRewards.grant(sector, run);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Delivering the Star Chart reward for run " + run.runId() + " failed", e);
        }
        try {
            GatewayUpgradeRewards.completed(sector, run.tier());
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Granting the Wormhole Gateway upgrade for run " + run.runId() + " failed", e);
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
