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
import exiledsector.charts.officer.OfficerDefeat;
import exiledsector.effects.ItemLootListener;
import exiledsector.items.ItemDrops;
import exiledsector.loot.LootTaken;
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
        completedOutsideBattle(sector, run);
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
        completedOutsideBattle(sector, run);
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

    public static ItemDrops.SiteLoot bossFellInBattle(SectorAPI sector, CampaignFleetAPI sourceFleet, List<FleetMemberAPI> lostMembers) {
        ChartRun run = trackedRun();
        if (run == null || sourceFleet == null || run.bossFlagshipId() == null || !isBossFleet(run, sourceFleet)
                || lostMembers.stream().noneMatch(member -> run.bossFlagshipId().equals(member.getId()))
                || run.protectedFleetId() != null && protectedFallen(sector, run)) {
            return ItemDrops.SiteLoot.NONE;
        }
        return completed(sector, run);
    }

    private static void completedOutsideBattle(SectorAPI sector, ChartRun run) {
        ItemDrops.SiteLoot rewardLoot = completed(sector, run);
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (!rewardLoot.isEmpty() && playerFleet != null && playerFleet.getCargo() != null) {
            ItemLootListener.addSiteLoot(playerFleet.getCargo(), rewardLoot);
            LootTaken.report();
        }
    }

    private static ItemDrops.SiteLoot completed(SectorAPI sector, ChartRun run) {
        if (!run.markObjectiveComplete()) {
            return ItemDrops.SiteLoot.NONE;
        }
        AtlasProgress.completed(sector, run);
        ItemDrops.SiteLoot rewardLoot = ItemDrops.SiteLoot.NONE;
        try {
            ChartRewards.Reward reward = ChartRewards.grant(sector, run);
            if (reward != null) {
                rewardLoot = reward.loot();
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Rolling the Star Chart reward for run " + run.runId() + " failed", e);
        }
        try {
            GatewayUpgradeRewards.completed(sector, run.tier());
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Granting the Wormhole Gateway upgrade for run " + run.runId() + " failed", e);
        }
        rewardLoot = rewardLoot.plus(OfficerDefeat.bossDefeated(sector, run));
        updateIntel(sector, run);
        return rewardLoot;
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
