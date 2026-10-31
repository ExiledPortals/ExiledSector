package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunBound;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.CampaignMessages;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ConvoyScript implements EveryFrameScript, FleetEventListener, ChartRunBound {

    private static final Logger LOG = Logger.getLogger(ConvoyScript.class);

    private final String runId;
    private final int tier;
    private final StarSystemAPI system;
    private final List<ConvoyState> convoys = new ArrayList<>();
    private final Map<String, Float> holds = new HashMap<>();
    private long arrivalTimestamp = Long.MIN_VALUE;
    private long lastPulseTimestamp = Long.MIN_VALUE;
    private boolean intelAdded;
    private float daysSinceCheck = Float.MAX_VALUE;
    private boolean done;
    private transient boolean failureReported;

    public ConvoyScript(String runId, int tier, StarSystemAPI system) {
        this.runId = runId;
        this.tier = tier;
        this.system = system;
    }

    public static ConvoyScript in(LocationAPI location) {
        if (location == null) {
            return null;
        }
        for (EveryFrameScript script : location.getScripts()) {
            if (script instanceof ConvoyScript convoyScript && !convoyScript.isDone()) {
                return convoyScript;
            }
        }
        return null;
    }

    public static ConvoyState convoyOf(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getMemoryWithoutUpdate() == null || !fleet.getMemoryWithoutUpdate().contains(SupplyConvoy.CONVOY_MEMORY_KEY)) {
            return null;
        }
        ConvoyScript script = in(fleet.getContainingLocation());
        return script == null ? null : script.convoy(fleet.getMemoryWithoutUpdate().getInt(SupplyConvoy.CONVOY_MEMORY_KEY));
    }

    void add(ConvoyState convoy) {
        convoys.add(convoy);
    }

    public List<ConvoyState> convoys() {
        return List.copyOf(convoys);
    }

    public ConvoyState convoy(int index) {
        for (ConvoyState convoy : convoys) {
            if (convoy.index() == index) {
                return convoy;
            }
        }
        return null;
    }

    public int tier() {
        return tier;
    }

    public StarSystemAPI system() {
        return system;
    }

    public String runId() {
        return runId;
    }

    public boolean isActive() {
        for (ConvoyState convoy : convoys) {
            if (convoy.phase() != ConvoyState.Phase.LOST && !convoy.isCacheTaken()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return runId != null && runId.equals(wantedRunId);
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            return;
        }
        if (ChartRuns.unclosed(runId) == null) {
            done = true;
            return;
        }
        try {
            if (!intelAdded) {
                intelAdded = true;
                convoys.forEach(convoy -> ConvoyIntel.addFor(sector, this, convoy));
            }
            holdFleets(amount);
            daysSinceCheck += sector.getClock().convertToDays(amount);
            if (daysSinceCheck < ConvoyRules.CHECK_DAYS) {
                return;
            }
            daysSinceCheck = 0f;
            check(sector);
        } catch (RuntimeException | LinkageError e) {
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] Running the supply convoy failed; it is tried again next check", e);
            }
        }
    }

    void check(SectorAPI sector) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        CampaignClockAPI clock = sector.getClock();
        boolean playerInside = playerFleet != null && playerFleet.getContainingLocation() == system;
        if (arrivalTimestamp == Long.MIN_VALUE && playerInside) {
            arrivalTimestamp = clock.getTimestamp();
        }
        if (playerInside) {
            checkPulse(playerFleet, clock);
        }
        List<CampaignTerrainPlugin> slowing = ConvoyTerrain.slowingTerrain(system);
        for (ConvoyState convoy : convoys) {
            if (convoy.phase() == ConvoyState.Phase.WAITING && arrivalTimestamp != Long.MIN_VALUE
                    && clock.getElapsedDaysSince(arrivalTimestamp) >= ConvoyRules.DEPARTURE_DAYS) {
                depart(convoy);
            }
            catchUpOnLosses(convoy);
            moveScreenLead(convoy);
            keepFormation(convoy, slowing);
        }
    }

    void keepFormation(ConvoyState convoy, List<CampaignTerrainPlugin> slowing) {
        float slowestEscort = Float.MAX_VALUE;
        for (ConvoyRole role : List.of(ConvoyRole.SCREEN, ConvoyRole.ESCORT)) {
            CampaignFleetAPI escort = fleet(convoy, role);
            if (escort != null) {
                slowestEscort = Math.min(slowestEscort, ConvoyTerrain.unmodifiedBurn(escort));
            }
        }
        for (ConvoyRole role : ConvoyRole.values()) {
            CampaignFleetAPI fleet = fleet(convoy, role);
            if (fleet == null) {
                continue;
            }
            float burnMult = ConvoyTerrain.inside(fleet, slowing) ? ConvoyRules.terrainBurn(role) : 1f;
            if (role == ConvoyRole.FREIGHTERS && burnMult == 1f && !convoy.isHostile() && slowestEscort < Float.MAX_VALUE) {
                burnMult = ConvoyTerrain.formationMult(ConvoyTerrain.unmodifiedBurn(fleet), slowestEscort);
            }
            ConvoyTerrain.apply(fleet, burnMult);
        }
    }

    void checkPulse(CampaignFleetAPI playerFleet, CampaignClockAPI clock) {
        if (playerFleet.getMemoryWithoutUpdate() == null || !playerFleet.getMemoryWithoutUpdate().getBoolean(MemFlags.JUST_DID_INTERDICTION_PULSE)
                || lastPulseTimestamp != Long.MIN_VALUE && clock.getElapsedDaysSince(lastPulseTimestamp) < ConvoyRules.PULSE_REPEAT_DAYS) {
            return;
        }
        lastPulseTimestamp = clock.getTimestamp();
        float range = InterdictionPulseAbility.getRange(playerFleet);
        for (ConvoyState convoy : convoys) {
            boolean hit = false;
            for (ConvoyRole role : ConvoyRole.values()) {
                CampaignFleetAPI fleet = fleet(convoy, role);
                if (fleet != null && !fleet.isInHyperspaceTransition() && Misc.getDistance(playerFleet.getLocation(), fleet.getLocation()) <= range) {
                    hit = true;
                    holds.put(fleet.getId(), Math.max(0f, InterdictionPulseAbility.getInterdictSeconds(playerFleet, fleet)));
                }
            }
            if (hit) {
                turnHostile(convoy);
            }
        }
    }

    void holdFleets(float amount) {
        if (holds.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Float>> held = holds.entrySet().iterator();
        while (held.hasNext()) {
            Map.Entry<String, Float> hold = held.next();
            SectorEntityToken entity = system.getEntityById(hold.getKey());
            float left = hold.getValue() - amount;
            if (!(entity instanceof CampaignFleetAPI fleet) || left <= 0f) {
                held.remove();
                continue;
            }
            fleet.goSlowOneFrame(true);
            hold.setValue(left);
        }
    }

    public boolean turnHostile(ConvoyState convoy) {
        if (!convoy.markHostile()) {
            return false;
        }
        CampaignFleetAPI freighters = fleet(convoy, ConvoyRole.FREIGHTERS);
        for (ConvoyRole role : ConvoyRole.values()) {
            CampaignFleetAPI fleet = fleet(convoy, role);
            if (fleet == null) {
                continue;
            }
            ConvoyHostility.makeHostile(fleet, role);
            if (role != ConvoyRole.FREIGHTERS) {
                guard(fleet, freighters);
            }
        }
        if (freighters == null || convoy.phase() == ConvoyState.Phase.LOST || convoy.phase() == ConvoyState.Phase.DELIVERED) {
            leave(convoy);
        } else {
            flee(convoy, freighters);
        }
        if (CampaignMessages.available() && Global.getSector().getPlayerFleet() != null
                && Global.getSector().getPlayerFleet().getContainingLocation() == system) {
            CampaignMessages.plain(Translation.gameText("chart.convoy.alarm"), Misc.getNegativeHighlightColor());
        }
        return true;
    }

    void flee(ConvoyState convoy, CampaignFleetAPI freighters) {
        if (convoy.phase() != ConvoyState.Phase.WAITING && convoy.phase() != ConvoyState.Phase.EN_ROUTE) {
            return;
        }
        convoy.setPhase(ConvoyState.Phase.EN_ROUTE);
        freighters.clearAssignments();
        headForDestination(convoy, freighters);
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        ConvoyState convoy = freightersOf(fleet);
        if (convoy != null) {
            recordLosses(convoy, ConvoyCargo.freighterPoints(fleet, convoy.countsCivilianOnly()), fleet.getLocation());
        }
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        ConvoyState convoy = freightersOf(fleet);
        if (convoy == null) {
            return;
        }
        if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE) {
            recordLosses(convoy, 0f, fleet.getLocation());
        } else if (convoy.phase() != ConvoyState.Phase.DELIVERED) {
            convoy.setPhase(ConvoyState.Phase.LOST);
        }
        leave(convoy);
    }

    void leave(ConvoyState convoy) {
        for (ConvoyRole role : ConvoyRole.values()) {
            CampaignFleetAPI fleet = fleet(convoy, role);
            if (fleet != null) {
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, convoy.exitToken(), ChartContentGenerator.ASSIGNMENT_DAYS);
            }
        }
    }

    private ConvoyState freightersOf(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getId() == null) {
            return null;
        }
        for (ConvoyState convoy : convoys) {
            if (fleet.getId().equals(convoy.fleetId(ConvoyRole.FREIGHTERS))) {
                return convoy;
            }
        }
        return null;
    }

    void catchUpOnLosses(ConvoyState convoy) {
        CampaignFleetAPI freighters = fleet(convoy, ConvoyRole.FREIGHTERS);
        float remaining = ConvoyCargo.freighterPoints(freighters, convoy.countsCivilianOnly());
        if (freighters != null && convoy.isHostile() && remaining < convoy.carriedFreighterPoints()) {
            recordLosses(convoy, remaining, freighters.getLocation());
        }
    }

    void recordLosses(ConvoyState convoy, float remainingFreighterPoints, Vector2f at) {
        boolean wasLost = convoy.phase() == ConvoyState.Phase.LOST;
        float lostShare = convoy.recordFreighterPoints(remainingFreighterPoints);
        if (!wasLost && convoy.phase() == ConvoyState.Phase.LOST) {
            leave(convoy);
        }
        SectorAPI sector = Global.getSector();
        ChartRun run = sector == null ? null : ChartRuns.live(runId);
        if (!(lostShare > 0f) || at == null || run == null) {
            return;
        }
        Random dropRandom = new Random(convoy.nextDropSeed());
        ConvoyCargo.dropPods(system, runId, ConvoyCargo.roll(sector, run, convoy, lostShare * ConvoyRules.POD_SHARE, dropRandom), at);
    }

    void guard(CampaignFleetAPI escort, CampaignFleetAPI freighters) {
        escort.clearAssignments();
        if (freighters != null) {
            escort.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, freighters, ChartContentGenerator.ASSIGNMENT_DAYS);
        }
        SectorEntityToken centre = system.getCenter() != null ? system.getCenter() : system.getStar();
        if (centre != null) {
            escort.addAssignment(FleetAssignment.PATROL_SYSTEM, centre, ChartContentGenerator.ASSIGNMENT_DAYS);
        }
    }

    public float daysUntilDeparture(CampaignClockAPI clock) {
        if (arrivalTimestamp == Long.MIN_VALUE) {
            return ConvoyRules.DEPARTURE_DAYS;
        }
        return Math.max(0f, ConvoyRules.DEPARTURE_DAYS - clock.getElapsedDaysSince(arrivalTimestamp));
    }

    public boolean playerHasArrived() {
        return arrivalTimestamp != Long.MIN_VALUE;
    }

    CampaignFleetAPI fleet(ConvoyState convoy, ConvoyRole role) {
        String fleetId = convoy.fleetId(role);
        SectorEntityToken entity = fleetId == null ? null : system.getEntityById(fleetId);
        return entity instanceof CampaignFleetAPI fleet && fleet.isAlive() ? fleet : null;
    }

    SectorEntityToken entity(String entityId) {
        return entityId == null ? null : system.getEntityById(entityId);
    }

    void depart(ConvoyState convoy) {
        CampaignFleetAPI freighters = fleet(convoy, ConvoyRole.FREIGHTERS);
        if (freighters == null) {
            convoy.setPhase(ConvoyState.Phase.LOST);
            leave(convoy);
            return;
        }
        convoy.setPhase(ConvoyState.Phase.EN_ROUTE);
        freighters.clearAssignments();
        for (String waypointId : convoy.waypointIds()) {
            SectorEntityToken waypoint = entity(waypointId);
            if (waypoint != null) {
                freighters.addAssignment(FleetAssignment.GO_TO_LOCATION, waypoint, ChartContentGenerator.ASSIGNMENT_DAYS);
                freighters.addAssignment(FleetAssignment.ORBIT_PASSIVE, waypoint, ConvoyRules.STOP_DAYS);
            }
        }
        headForDestination(convoy, freighters);
    }

    void headForDestination(ConvoyState convoy, CampaignFleetAPI freighters) {
        SectorEntityToken destination = entity(convoy.destinationId());
        if (destination == null) {
            freighters.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, convoy.exitToken(), ChartContentGenerator.ASSIGNMENT_DAYS);
            return;
        }
        freighters.addAssignment(FleetAssignment.GO_TO_LOCATION, destination, ChartContentGenerator.ASSIGNMENT_DAYS,
                new ConvoyStep(this, convoy.index(), ConvoyStep.Kind.ARRIVED));
        freighters.addAssignment(FleetAssignment.ORBIT_PASSIVE, destination, ConvoyRules.UNLOAD_DAYS,
                new ConvoyStep(this, convoy.index(), ConvoyStep.Kind.UNLOADED));
    }

    void arrived(ConvoyState convoy) {
        if (convoy.phase() == ConvoyState.Phase.EN_ROUTE) {
            convoy.setPhase(ConvoyState.Phase.UNLOADING);
        }
    }

    void unloaded(ConvoyState convoy) {
        if (!convoy.deliver()) {
            return;
        }
        CampaignFleetAPI freighters = fleet(convoy, ConvoyRole.FREIGHTERS);
        if (freighters != null) {
            freighters.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, convoy.exitToken(), ChartContentGenerator.ASSIGNMENT_DAYS);
        }
        SectorEntityToken destination = entity(convoy.destinationId());
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        if (destination != null && convoy.hasCache() && playerFleet != null && playerFleet.getContainingLocation() == system && CampaignMessages.available()) {
            String planet = destination.getName();
            CampaignMessages.plain(I18n.forGameText(() -> Translation.msg("chart.convoy.delivered").arg("planet", planet).text()),
                    Misc.getHighlightColor());
        }
    }

    public List<ConvoyState> cachesAt(SectorEntityToken planet) {
        List<ConvoyState> caches = new ArrayList<>();
        for (ConvoyState convoy : convoys) {
            if (planet != null && planet.getId() != null && planet.getId().equals(convoy.destinationId()) && convoy.hasCache()) {
                caches.add(convoy);
            }
        }
        return caches;
    }

    void moveScreenLead(ConvoyState convoy) {
        SectorEntityToken lead = convoy.screenLead();
        CampaignFleetAPI freighters = fleet(convoy, ConvoyRole.FREIGHTERS);
        if (lead == null || freighters == null) {
            return;
        }
        Vector2f from = freighters.getLocation();
        Vector2f heading = headingOf(freighters, convoy);
        lead.setLocation(from.x + heading.x * ConvoyRules.SCREEN_LEAD, from.y + heading.y * ConvoyRules.SCREEN_LEAD);
    }

    Vector2f headingOf(CampaignFleetAPI freighters, ConvoyState convoy) {
        FleetAssignmentDataAPI current = freighters.getCurrentAssignment();
        SectorEntityToken target = current == null ? null : current.getTarget();
        if (target == null || target.getLocation() == null || target == convoy.entryToken()) {
            target = nextStop(convoy);
        }
        if (target == null || target.getLocation() == null) {
            return new Vector2f();
        }
        Vector2f heading = Vector2f.sub(target.getLocation(), freighters.getLocation(), new Vector2f());
        if (heading.lengthSquared() < 1f) {
            return new Vector2f();
        }
        return (Vector2f) heading.normalise();
    }

    SectorEntityToken nextStop(ConvoyState convoy) {
        for (String waypointId : convoy.waypointIds()) {
            SectorEntityToken waypoint = entity(waypointId);
            if (waypoint != null) {
                return waypoint;
            }
        }
        return entity(convoy.destinationId());
    }
}
