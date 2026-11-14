package exiledsector.charts.objective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.skirmish.SkirmishObjective;
import exiledsector.charts.anomaly.skirmish.SkirmishProgress;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.atlas.AtlasProgress;
import exiledsector.charts.officer.OfficerDefeat;
import exiledsector.effects.ItemLootListener;
import exiledsector.effects.PlayerEngagement;
import exiledsector.items.ItemDrops;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.LootTaken;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
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

    public static boolean fleetGone(SectorAPI sector, CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        ChartRun run = trackedRun();
        if (run == null || fleet == null || !DEFEAT_REASONS.contains(reason) || !ChartIds.belongsToRun(fleet, run.runId())) {
            return false;
        }
        if (SkirmishObjective.tracks(run)) {
            boolean inPlayerBattle = param instanceof BattleAPI battle && battle.isPlayerInvolved();
            return deliverOutsideBattle(sector, afterSkirmishFall(sector, run, SkirmishObjective.fleetGone(sector, run, fleet, inPlayerBattle)));
        }
        if (!isBossFleet(run, fleet)) {
            return false;
        }
        completedOutsideBattle(sector, run);
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
        if (SkirmishObjective.tracks(run)) {
            return deliverOutsideBattle(sector, afterSkirmishFall(sector, run, SkirmishObjective.reconcile(sector, run, system)));
        }
        boolean bossStanding = false;
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (fleet.isDespawning() || fleet.isEmpty()) {
                continue;
            }
            if (isBossFleet(run, fleet)) {
                run.recordBossFlagship(flagshipIdOf(fleet));
                bossStanding = holdsMember(fleet, run.bossFlagshipId());
            }
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

    public static CampaignFleetAPI bossFleet(SectorAPI sector, ChartRun run) {
        StarSystemAPI system = sector == null || run == null || !run.hasBoss() ? null : sector.getStarSystem(run.systemId());
        if (system == null) {
            return null;
        }
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (isBossFleet(run, fleet)) {
                return fleet;
            }
        }
        return null;
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

    public static ItemDrops.SiteLoot bossFellInBattle(SectorAPI sector, CampaignFleetAPI sourceFleet, List<FleetMemberAPI> lostMembers) {
        ChartRun run = trackedRun();
        if (run != null && SkirmishObjective.tracks(run)) {
            SkirmishProgress progress = afterSkirmishFall(sector, run, SkirmishObjective.fellInBattle(sector, run, sourceFleet, lostMembers));
            return progress.loot();
        }
        if (run == null || sourceFleet == null || run.bossFlagshipId() == null || !isBossFleet(run, sourceFleet)
                || lostMembers.stream().noneMatch(member -> run.bossFlagshipId().equals(member.getId()))) {
            return ItemDrops.SiteLoot.NONE;
        }
        return completed(sector, run);
    }

    public static void afterPlayerEngagement(PlayerEngagement engagement) {
        if (engagement.playerWon() || engagement.result() == null) {
            return;
        }
        try {
            flagshipsLostInLostEngagement(Global.getSector(), engagement.result().getBattle(), engagement.enemyLosses());
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Checking whether a Faction Skirmish flagship fell in a lost engagement failed", e);
        }
    }

    static boolean flagshipsLostInLostEngagement(SectorAPI sector, BattleAPI battle, List<FleetMemberAPI> enemyLosses) {
        ChartRun run = trackedRun();
        if (run == null || battle == null || !SkirmishObjective.tracks(run)) {
            return false;
        }
        Map<CampaignFleetAPI, List<FleetMemberAPI>> lostBySourceFleet = new BattleEngagement(battle, 1f, 0f, enemyLosses).lostEnemiesBySourceFleet();
        boolean anyFell = false;
        for (Map.Entry<CampaignFleetAPI, List<FleetMemberAPI>> lost : lostBySourceFleet.entrySet()) {
            SkirmishProgress progress = SkirmishObjective.fellInBattle(sector, run, lost.getKey(), lost.getValue());
            anyFell |= deliverOutsideBattle(sector, afterSkirmishFall(sector, run, progress));
        }
        return anyFell;
    }

    private static void completedOutsideBattle(SectorAPI sector, ChartRun run) {
        addToPlayerCargo(sector, completed(sector, run));
    }

    private static boolean deliverOutsideBattle(SectorAPI sector, SkirmishProgress progress) {
        addToPlayerCargo(sector, progress.loot());
        return progress.flagshipFell();
    }

    private static void addToPlayerCargo(SectorAPI sector, ItemDrops.SiteLoot loot) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (!loot.isEmpty() && playerFleet != null && playerFleet.getCargo() != null) {
            ItemLootListener.addSiteLoot(playerFleet.getCargo(), loot);
            LootTaken.report();
        }
    }

    private static SkirmishProgress afterSkirmishFall(SectorAPI sector, ChartRun run, SkirmishProgress progress) {
        if (!progress.bothFallen() || !run.markObjectiveComplete()) {
            return progress;
        }
        ItemDrops.SiteLoot anomalyLoot = anomalyBossLoot(sector, run);
        AtlasProgress.completed(sector, run, progress.beatenOfficerId());
        return new SkirmishProgress(true, progress.loot().plus(rewarded(sector, run)).plus(anomalyLoot), true, progress.beatenOfficerId());
    }

    private static ItemDrops.SiteLoot completed(SectorAPI sector, ChartRun run) {
        if (!run.markObjectiveComplete()) {
            return ItemDrops.SiteLoot.NONE;
        }
        ItemDrops.SiteLoot officerLoot = OfficerDefeat.bossDefeated(sector, run);
        ItemDrops.SiteLoot anomalyLoot = anomalyBossLoot(sector, run);
        AtlasProgress.completed(sector, run);
        return rewarded(sector, run).plus(officerLoot).plus(anomalyLoot);
    }

    private static ItemDrops.SiteLoot rewarded(SectorAPI sector, ChartRun run) {
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
        updateIntel(sector, run);
        return rewardLoot;
    }

    private static ItemDrops.SiteLoot anomalyBossLoot(SectorAPI sector, ChartRun run) {
        ItemDrops.SiteLoot anomalyLoot = ItemDrops.SiteLoot.NONE;
        try {
            for (LootShaper anomaly : ChartAnomalies.inRun(run, LootShaper.class)) {
                anomalyLoot = anomalyLoot.plus(anomaly.bossDefeatedLoot(sector, run));
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Rolling the anomaly boss drops for run " + run.runId() + " failed", e);
        }
        return anomalyLoot;
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
