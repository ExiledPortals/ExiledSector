package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.anomaly.RunFactionDress;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.content.SettledObjective;
import exiledsector.charts.layout.ChartGeometry;
import exiledsector.charts.objective.ChartObjectives;
import exiledsector.charts.officer.OfficerFleets;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class SkirmishBattle {

    static final String HOSTILE_TO_FACTION_PREFIX = "$cfai_makeHostile_";
    static final long DEFENDER_FLEET_SALT = 0x4348534b444546L;
    static final long ATTACKER_FLEET_SALT = 0x4348534b464c54L;
    static final long STAGING_SALT = 0x4348535441474eL;
    private static final Logger LOG = Logger.getLogger(SkirmishBattle.class);

    private final AnomalyContentSite site;
    private final SkirmishPlan plan;
    private final CampaignFleetAPI station;
    private String attackerCopyId;
    private String defenderCopyId;

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
        defenderCopyId = ChartAnomalyFleets.dress(RunFactionDress.DEFENDER_FACTION_ID, RunFactionDress.hostileCopyOf(site.factionId()));
        attackerCopyId = ChartAnomalyFleets.dress(RunFactionDress.ATTACKER_FACTION_ID, RunFactionDress.hostileCopyOf(plan.attackerFactionId()));
        for (CampaignFleetAPI defending : site.forces().fleets()) {
            takeSide(defending, FightingSide.DEFENDER);
        }
        if (station != null) {
            ChartAnomalyFleets.unmarkBossStation(station);
            site.forces().fleets(FleetRole.PATROL).forEach(this::rallyAtStation);
        }
        AnomalyState state = site.state(FactionSkirmish.ANOMALY_ID);
        CampaignFleetAPI defenderFleet = spawnOfficerFleet(FightingSide.DEFENDER, state);
        CampaignFleetAPI attackerFleet = spawnOfficerFleet(FightingSide.ATTACKER, state);
        CampaignFleetAPI objective = defenderFleet != null ? defenderFleet : attackerFleet;
        if (objective != null) {
            site.settle(new SettledObjective(objective, ChartBossType.FLEET));
        }
    }

    private void takeSide(CampaignFleetAPI fleet, FightingSide side) {
        boolean attacking = side == FightingSide.ATTACKER;
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(ChartIds.SKIRMISH_SIDE_MEMORY_KEY, side.key());
        memory.set(ChartIds.REAL_FACTION_MEMORY_KEY, realFactionOf(attacking));
        String ownFactionId = fightingFactionOf(attacking);
        String enemyFactionId = fightingFactionOf(!attacking);
        if (!enemyFactionId.equals(ownFactionId)) {
            memory.set(HOSTILE_TO_FACTION_PREFIX + enemyFactionId, true);
        }
        if (!ownFactionId.equals(realFactionOf(attacking))) {
            fleet.setFaction(ownFactionId, true);
        }
    }

    private String realFactionOf(boolean attacking) {
        return attacking ? plan.attackerFactionId() : site.factionId();
    }

    private String fightingFactionOf(boolean attacking) {
        String copyId = attacking ? attackerCopyId : defenderCopyId;
        return copyId != null ? copyId : realFactionOf(attacking);
    }

    private void rallyAtStation(CampaignFleetAPI patrol) {
        patrol.clearAssignments();
        patrol.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, station, ChartContentGenerator.ASSIGNMENT_DAYS);
        ChartContentGenerator.patrolSystem(patrol, site.system());
    }

    private CampaignFleetAPI spawnOfficerFleet(FightingSide side, AnomalyState state) {
        long fleetSeed = site.random(side == FightingSide.ATTACKER ? ATTACKER_FLEET_SALT : DEFENDER_FLEET_SALT).nextLong();
        try {
            return spawnOfficerFleet(side, state, fleetSeed);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] The Faction Skirmish fleet of the " + side.key() + " could not be spawned", e);
            return null;
        }
    }

    private CampaignFleetAPI spawnOfficerFleet(FightingSide side, AnomalyState state, long fleetSeed) {
        boolean attacking = side == FightingSide.ATTACKER;
        Random oneFleetRandom = new Random(fleetSeed);
        PlannedFleet planned = attacking ? plan.attackerFleet() : plan.defenderFleet();
        int officerRecordId = attacking ? plan.attackerOfficerId() : plan.defenderOfficerId();
        CampaignFleetAPI fleet = ChartAnomalyFleets.planned(realFactionOf(attacking), planned, oneFleetRandom,
                site.runId(), site.tier(), fleetSeed);
        if (fleet == null) {
            return null;
        }
        OfficerFleets.dress(fleet, officerRecordId);
        takeSide(fleet, side);
        ChartAnomalyFleets.arm(fleet, true, site.context(), site.tier());
        Vector2f holdPoint = attacking ? stagingPoint() : defendedPoint();
        site.addFleet(fleet, ChartGeometry.pointAtRandomAngle(holdPoint, ChartContentGenerator.GUARD_OFFSET, oneFleetRandom));
        if (attacking) {
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, site.system().createToken(holdPoint.x, holdPoint.y), ChartContentGenerator.ASSIGNMENT_DAYS);
        } else {
            SectorEntityToken guarded = station != null ? station : site.system().createToken(holdPoint.x, holdPoint.y);
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, guarded, ChartContentGenerator.ASSIGNMENT_DAYS);
            ChartContentGenerator.patrolSystem(fleet, site.system());
        }
        ChartAnomalyFleets.revealBoss(fleet);
        String flagshipId = ChartObjectives.flagshipIdOf(fleet);
        if (SkirmishFlagship.canRecord(fleet.getId(), flagshipId)) {
            SkirmishFlagships.record(state, SkirmishFlagship.standing(side, officerRecordId, fleet.getId(), flagshipId));
        }
        return fleet;
    }

    private Vector2f defendedPoint() {
        return station != null && station.getLocation() != null ? new Vector2f(station.getLocation()) : ChartAnomalyFleets.farSideOf(site.entry());
    }

    private Vector2f stagingPoint() {
        Vector2f defended = defendedPoint();
        ChartSpot farthest = null;
        float farthestDistance = -1f;
        for (ChartSpot spot : ChartAnomalyFleets.openSpots(site.system(), site.entry(), site.random(STAGING_SALT))) {
            float distance = Vector2f.sub(spot.position(), defended, new Vector2f()).length();
            if (distance > farthestDistance) {
                farthest = spot;
                farthestDistance = distance;
            }
        }
        return farthest != null ? new Vector2f(farthest.position()) : new Vector2f(-defended.y, defended.x);
    }
}
