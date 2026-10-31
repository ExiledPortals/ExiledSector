package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.RunFactionDress;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.content.ContentForces;
import exiledsector.charts.content.SettledObjective;
import exiledsector.charts.objective.ChartObjective;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class SkirmishBattle {

    static final String HOSTILE_TO_FACTION_PREFIX = "$cfai_makeHostile_";
    static final long FLEET_SALT = 0x4348534b464c54L;
    static final long STAGING_SALT = 0x4348535441474eL;
    private static final Logger LOG = Logger.getLogger(SkirmishBattle.class);

    private final AnomalyContentSite site;
    private final SkirmishPlan plan;
    private final CampaignFleetAPI station;
    private final List<String> attackerIds = new ArrayList<>();
    private String allyFactionId;
    private CampaignFleetAPI commander;
    private Vector2f stagingPoint;

    private SkirmishBattle(AnomalyContentSite site, SkirmishPlan plan) {
        this.site = site;
        this.plan = plan;
        this.station = site.forces().bossStation();
    }

    static void generate(AnomalyContentSite site) {
        SkirmishPlan plan = site.plan(SkirmishPlan.class);
        if (plan != null) {
            new SkirmishBattle(site, plan).fight();
        }
    }

    private void fight() {
        ContentForces defenders = site.forces();
        allyFactionId = plan.side() == SkirmishSide.ATTACKER
                ? ChartAnomalyFleets.dress(RunFactionDress.ALLY_FACTION_ID, RunFactionDress.alliesOf(plan.attackerFactionId()))
                : defenders.allyFactionId();
        for (CampaignFleetAPI defender : defenders.fleets()) {
            takeSide(defender, SkirmishSide.DEFENDER, plan.attackerFactionId());
        }
        if (station != null) {
            defenders.fleets(FleetRole.PATROL).forEach(this::rallyAtStation);
        }
        Random fleetSeeds = site.random(FLEET_SALT);
        for (PlannedFleet planned : plan.attackerFleets()) {
            long fleetSeed = fleetSeeds.nextLong();
            try {
                spawnAttacker(planned, fleetSeed);
            } catch (RuntimeException | LinkageError e) {
                LOG.error("[ExiledSector] A Faction Skirmish attacker fleet could not be spawned; the attack goes ahead without it", e);
            }
        }
        if (plan.side() == SkirmishSide.DEFENDER) {
            settleForDefenders(defenders);
        }
    }

    private void takeSide(CampaignFleetAPI fleet, SkirmishSide side, String enemyFactionId) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(HOSTILE_TO_FACTION_PREFIX + enemyFactionId, true);
        memory.set(ChartIds.SKIRMISH_SIDE_MEMORY_KEY, side.key());
        if (!plan.alliedWith(side) && allyFactionId != null) {
            memory.set(HOSTILE_TO_FACTION_PREFIX + allyFactionId, true);
        }
    }

    private void rallyAtStation(CampaignFleetAPI patrol) {
        patrol.clearAssignments();
        patrol.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, station, ChartContentGenerator.ASSIGNMENT_DAYS);
        patrolSystem(patrol);
    }

    private void spawnAttacker(PlannedFleet planned, long fleetSeed) {
        Random oneFleetRandom = new Random(fleetSeed);
        CampaignFleetAPI fleet = ChartAnomalyFleets.planned(plan.attackerFactionId(), planned, oneFleetRandom, site.runId(), site.tier(), fleetSeed);
        if (fleet == null) {
            return;
        }
        boolean leads = planned.role() == FleetRole.BOSS;
        takeSide(fleet, SkirmishSide.ATTACKER, site.factionId());
        if (leads) {
            fleet.getMemoryWithoutUpdate().set(ChartIds.SKIRMISH_COMMANDER_MEMORY_KEY, true);
        }
        if (plan.alliedWith(SkirmishSide.ATTACKER)) {
            ChartAnomalyFleets.joinPlayer(fleet, allyFactionId);
        } else {
            ChartAnomalyFleets.arm(fleet, leads, site.context(), site.tier());
        }
        Vector2f staging = stagingPoint();
        site.addFleet(fleet, ChartSites.pointAround(staging, oneFleetRandom.nextDouble() * Math.PI * 2d, ChartContentGenerator.GUARD_OFFSET));
        if (leads) {
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, site.system().createToken(staging.x, staging.y), ChartContentGenerator.ASSIGNMENT_DAYS);
        } else {
            if (station != null) {
                fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, station, ChartContentGenerator.ASSIGNMENT_DAYS);
            }
            patrolSystem(fleet);
        }
        attackerIds.add(fleet.getId());
        if (leads) {
            commander = fleet;
        }
    }

    private void patrolSystem(CampaignFleetAPI fleet) {
        SectorEntityToken patrolCentre = site.centre();
        if (patrolCentre != null) {
            fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, patrolCentre, ChartContentGenerator.ASSIGNMENT_DAYS);
        }
    }

    private Vector2f stagingPoint() {
        if (stagingPoint != null) {
            return stagingPoint;
        }
        Vector2f defended = station != null && station.getLocation() != null ? new Vector2f(station.getLocation())
                : ChartAnomalyFleets.farSideOf(site.entry());
        ChartSpot farthest = null;
        float farthestDistance = -1f;
        for (ChartSpot spot : ChartAnomalyFleets.openSpots(site.system(), site.entry(), site.random(STAGING_SALT))) {
            float distance = Vector2f.sub(spot.position(), defended, new Vector2f()).length();
            if (distance > farthestDistance) {
                farthest = spot;
                farthestDistance = distance;
            }
        }
        stagingPoint = farthest != null ? new Vector2f(farthest.position()) : new Vector2f(-defended.y, defended.x);
        return stagingPoint;
    }

    private void settleForDefenders(ContentForces defenders) {
        if (commander != null) {
            ChartAnomalyFleets.revealBoss(commander);
        }
        CampaignFleetAPI defended = station != null ? station : defenders.boss();
        ChartObjective objective = commander != null ? ChartObjective.BOSS : attackerIds.isEmpty() ? null : ChartObjective.CLEAR;
        List<String> targets = commander != null ? List.of(commander.getId()) : attackerIds;
        site.settle(new SettledObjective(objective, targets, commander, ChartBossType.FLEET, defended == null ? null : defended.getId()));
    }
}
