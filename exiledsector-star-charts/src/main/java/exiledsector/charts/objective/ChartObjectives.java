package exiledsector.charts.objective;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.FleetMembers;
import exiledsector.Seeds;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.skirmish.SkirmishObjective;
import exiledsector.charts.anomaly.skirmish.SkirmishProgress;
import exiledsector.charts.content.ChartFleetMarks;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.atlas.AtlasProgress;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.loot.ChartFleetLedger;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.charts.officer.OfficerDefeat;
import exiledsector.effects.PlayerEngagement;
import exiledsector.items.ItemData;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.DropOrigin;
import exiledsector.loot.Loot;
import exiledsector.loot.LootDelivery;
import exiledsector.loot.LootTaken;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ChartObjectives {

    static final Set<FleetDespawnReason> DEFEAT_REASONS = Set.of(FleetDespawnReason.DESTROYED_BY_BATTLE, FleetDespawnReason.NO_MEMBERS);
    private static final String BOSS_CHARTS_SEED_PREFIX = "bossCharts|";
    private static final String TWIN_BOSS_CHARTS_SEED_SUFFIX = "|twin";
    private static final Logger LOG = Logger.getLogger(ChartObjectives.class);

    private ChartObjectives() {
    }

    static ChartRun trackedRun() {
        ChartRun run = ChartRuns.live();
        if (run == null || run.isObjectiveComplete() || !run.hasBoss()) {
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
        return memberId == null || fleet.getFleetData() == null || FleetMembers.withId(fleet, memberId) != null;
    }

    public static Loot bossFellInBattle(SectorAPI sector, CampaignFleetAPI sourceFleet, List<FleetMemberAPI> lostMembers) {
        return objectiveBossFell(sector, sourceFleet, lostMembers).plus(twinBossFell(sourceFleet, lostMembers));
    }

    private static Loot objectiveBossFell(SectorAPI sector, CampaignFleetAPI sourceFleet, List<FleetMemberAPI> lostMembers) {
        ChartRun run = trackedRun();
        if (run != null && SkirmishObjective.tracks(run)) {
            SkirmishProgress progress = afterSkirmishFall(sector, run, SkirmishObjective.fellInBattle(sector, run, sourceFleet, lostMembers));
            return progress.loot();
        }
        if (run == null || sourceFleet == null || run.bossFlagshipId() == null || !isBossFleet(run, sourceFleet)
                || lostMembers.stream().noneMatch(member -> run.bossFlagshipId().equals(member.getId()))) {
            return Loot.NONE;
        }
        return completed(sector, run);
    }

    static Loot twinBossFell(CampaignFleetAPI sourceFleet, List<FleetMemberAPI> lostMembers) {
        String twinFlagshipId = ChartFleetMarks.twinBossFlagshipId(sourceFleet);
        if (twinFlagshipId == null || lostMembers.stream().noneMatch(member -> twinFlagshipId.equals(member.getId()))) {
            return Loot.NONE;
        }
        ChartRun run = ChartFleetLedger.liveRunOf(sourceFleet);
        MemoryAPI twinMemory = sourceFleet.getMemoryWithoutUpdate();
        if (run == null || twinMemory.getBoolean(ChartIds.TWIN_BOSS_CHARTS_GIVEN_MEMORY_KEY)) {
            return Loot.NONE;
        }
        twinMemory.set(ChartIds.TWIN_BOSS_CHARTS_GIVEN_MEMORY_KEY, true);
        return guaranteedBossCharts(run, BOSS_CHARTS_SEED_PREFIX + run.runId() + TWIN_BOSS_CHARTS_SEED_SUFFIX);
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
        deliverToPlayerCargo(sector, completed(sector, run));
    }

    private static boolean deliverOutsideBattle(SectorAPI sector, SkirmishProgress progress) {
        deliverToPlayerCargo(sector, progress.loot());
        return progress.flagshipFell();
    }

    private static void deliverToPlayerCargo(SectorAPI sector, Loot loot) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (!loot.isEmpty() && playerFleet != null && playerFleet.getCargo() != null) {
            LootDelivery.deliver(playerFleet.getCargo(), loot, DropOrigin.ANYWHERE);
            LootTaken.report();
        }
    }

    private static SkirmishProgress afterSkirmishFall(SectorAPI sector, ChartRun run, SkirmishProgress progress) {
        if (!progress.bothFallen() || !run.markObjectiveComplete()) {
            return progress;
        }
        Loot anomalyLoot = anomalyBossLoot(sector, run);
        AtlasProgress.completed(sector, run, progress.beatenOfficerId());
        updateIntel(sector, run);
        return new SkirmishProgress(true, progress.loot().plus(anomalyLoot).plus(guaranteedBossCharts(run)), true, progress.beatenOfficerId());
    }

    private static Loot completed(SectorAPI sector, ChartRun run) {
        if (!run.markObjectiveComplete()) {
            return Loot.NONE;
        }
        Loot officerLoot = OfficerDefeat.bossDefeated(sector, run);
        Loot anomalyLoot = anomalyBossLoot(sector, run);
        AtlasProgress.completed(sector, run);
        updateIntel(sector, run);
        return officerLoot.plus(anomalyLoot).plus(guaranteedBossCharts(run));
    }

    private static Loot guaranteedBossCharts(ChartRun run) {
        return guaranteedBossCharts(run, BOSS_CHARTS_SEED_PREFIX + run.runId());
    }

    private static Loot guaranteedBossCharts(ChartRun run, String seed) {
        try {
            Random random = Seeds.random(seed);
            List<ItemData> charts = ChartDrops.guaranteedCharts(ChartDrops.Source.CHART_BOSS, ChartLootContext.of(run), random);
            return charts.isEmpty() ? Loot.NONE : new Loot(charts, List.of(), Map.of());
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] Rolling the guaranteed boss Star Charts for run " + run.runId() + " failed", e);
            return Loot.NONE;
        }
    }

    private static Loot anomalyBossLoot(SectorAPI sector, ChartRun run) {
        Loot anomalyLoot = Loot.NONE;
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
