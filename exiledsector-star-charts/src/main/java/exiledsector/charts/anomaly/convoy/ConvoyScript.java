package exiledsector.charts.anomaly.convoy;

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
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyRunScript;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.loot.ChartFleetLoot;
import exiledsector.charts.run.ChartRun;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ConvoyScript extends AnomalyRunScript implements FleetEventListener {

    private final List<ConvoyState> convoys = new ArrayList<>();
    private final Map<String, Float> holds = new HashMap<>();
    private long arrivalTimestamp = Long.MIN_VALUE;
    private long lastPulseTimestamp = Long.MIN_VALUE;
    private boolean intelAdded;

    public ConvoyScript(String runId, int tier, StarSystemAPI system) {
        super(runId, tier, system, true);
    }

    public static ConvoyScript in(LocationAPI location) {
        return in(location, ConvoyScript.class);
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

    public boolean isActive() {
        for (ConvoyState convoy : convoys) {
            if (convoy.phase() != ConvoyState.Phase.LOST && !convoy.isCacheTaken()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String anomalyId() {
        return SupplyConvoy.ANOMALY_ID;
    }

    @Override
    protected float checkDays() {
        return ConvoyRules.CHECK_DAYS;
    }

    @Override
    protected void everyFrame(SectorAPI sector, float amount) {
        if (!intelAdded) {
            intelAdded = true;
            convoys.forEach(convoy -> ConvoyIntel.addFor(sector, this, convoy));
        }
        holdFleets(amount);
    }

    @Override
    protected void check(SectorAPI sector, ChartRun run, float days) {
        check(sector);
    }

    void check(SectorAPI sector) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        CampaignClockAPI clock = sector.getClock();
        boolean playerInside = playerInSystem();
        if (arrivalTimestamp == Long.MIN_VALUE && playerInside) {
            arrivalTimestamp = clock.getTimestamp();
        }
        if (playerInside) {
            checkPulse(playerFleet, clock);
        }
        List<CampaignTerrainPlugin> slowing = ConvoyTerrain.slowingTerrain(system());
        for (ConvoyState convoy : convoys) {
            if (convoy.phase() == ConvoyState.Phase.WAITING && arrivalTimestamp != Long.MIN_VALUE
                    && clock.getElapsedDaysSince(arrivalTimestamp) >= ConvoyRules.DEPARTURE_DAYS) {
                depart(convoy);
            }
            catchUpOnLosses(convoy);
            slowInTerrain(convoy, slowing);
        }
    }

    void slowInTerrain(ConvoyState convoy, List<CampaignTerrainPlugin> slowing) {
        CampaignFleetAPI fleet = fleet(convoy);
        if (fleet != null) {
            ConvoyTerrain.apply(fleet, ConvoyTerrain.inside(fleet, slowing) ? ConvoyRules.TERRAIN_BURN : 1f);
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
            CampaignFleetAPI fleet = fleet(convoy);
            if (fleet != null && !fleet.isInHyperspaceTransition() && Misc.getDistance(playerFleet.getLocation(), fleet.getLocation()) <= range) {
                holds.put(fleet.getId(), Math.max(0f, InterdictionPulseAbility.getInterdictSeconds(playerFleet, fleet)));
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
            SectorEntityToken entity = entity(hold.getKey());
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
        CampaignFleetAPI fleet = fleet(convoy);
        if (fleet != null) {
            ConvoyHostility.makeHostile(fleet);
        }
        if (fleet == null || convoy.phase() == ConvoyState.Phase.LOST || convoy.phase() == ConvoyState.Phase.DELIVERED) {
            leave(convoy);
        } else {
            flee(convoy, fleet);
        }
        tellPlayer(() -> Translation.gameText("chart.anomaly.supply_convoy.alarm"), Misc::getNegativeHighlightColor);
        return true;
    }

    void flee(ConvoyState convoy, CampaignFleetAPI fleet) {
        if (convoy.phase() != ConvoyState.Phase.WAITING && convoy.phase() != ConvoyState.Phase.EN_ROUTE) {
            return;
        }
        convoy.setPhase(ConvoyState.Phase.EN_ROUTE);
        fleet.clearAssignments();
        headForDestination(convoy, fleet);
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        ConvoyState convoy = convoyFlying(fleet);
        if (convoy != null) {
            recordLosses(convoy, ConvoyCargo.freighterPoints(fleet, convoy.countsCivilianOnly()), fleet.getLocation());
        }
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        ConvoyState convoy = convoyFlying(fleet);
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
        CampaignFleetAPI fleet = fleet(convoy);
        if (fleet != null) {
            fleet.clearAssignments();
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, convoy.exitToken(), ChartContentGenerator.ASSIGNMENT_DAYS);
        }
    }

    private ConvoyState convoyFlying(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getId() == null) {
            return null;
        }
        for (ConvoyState convoy : convoys) {
            if (fleet.getId().equals(convoy.fleetId())) {
                return convoy;
            }
        }
        return null;
    }

    void catchUpOnLosses(ConvoyState convoy) {
        CampaignFleetAPI fleet = fleet(convoy);
        float remaining = ConvoyCargo.freighterPoints(fleet, convoy.countsCivilianOnly());
        if (fleet != null && convoy.isHostile() && remaining < convoy.carriedFreighterPoints()) {
            recordLosses(convoy, remaining, fleet.getLocation());
        }
    }

    void recordLosses(ConvoyState convoy, float remainingFreighterPoints, Vector2f at) {
        boolean wasLost = convoy.phase() == ConvoyState.Phase.LOST;
        float lostShare = convoy.recordFreighterPoints(remainingFreighterPoints);
        if (!wasLost && convoy.phase() == ConvoyState.Phase.LOST) {
            leave(convoy);
        }
        SectorAPI sector = Global.getSector();
        ChartRun run = sector == null ? null : liveRun();
        if (!(lostShare > 0f) || at == null || run == null) {
            return;
        }
        Random dropRandom = new Random(convoy.nextDropSeed());
        ConvoyCargo.dropPods(system(), runId(), ConvoyCargo.roll(sector, run, convoy, lostShare * ConvoyRules.POD_SHARE, dropRandom), at);
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

    CampaignFleetAPI fleet(ConvoyState convoy) {
        return livingFleet(convoy.fleetId());
    }

    void depart(ConvoyState convoy) {
        CampaignFleetAPI fleet = fleet(convoy);
        if (fleet == null) {
            convoy.setPhase(ConvoyState.Phase.LOST);
            return;
        }
        convoy.setPhase(ConvoyState.Phase.EN_ROUTE);
        fleet.clearAssignments();
        for (String waypointId : convoy.waypointIds()) {
            SectorEntityToken waypoint = entity(waypointId);
            if (waypoint != null) {
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, waypoint, ChartContentGenerator.ASSIGNMENT_DAYS);
                fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, waypoint, ConvoyRules.STOP_DAYS);
            }
        }
        headForDestination(convoy, fleet);
    }

    void headForDestination(ConvoyState convoy, CampaignFleetAPI fleet) {
        SectorEntityToken destination = entity(convoy.destinationId());
        if (destination == null) {
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, convoy.exitToken(), ChartContentGenerator.ASSIGNMENT_DAYS);
            return;
        }
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, destination, ChartContentGenerator.ASSIGNMENT_DAYS,
                new ConvoyStep(this, convoy.index(), ConvoyStep.Kind.ARRIVED));
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, destination, ConvoyRules.UNLOAD_DAYS,
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
        CampaignFleetAPI fleet = fleet(convoy);
        ChartRun run = liveRun();
        if (run != null) {
            convoy.storeCache(ConvoyCargo.deliveredCache(Global.getSector(), run, convoy, fleet, new Random(convoy.nextDropSeed())));
        }
        if (fleet != null) {
            ChartFleetLoot.spendLoot(fleet);
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, convoy.exitToken(), ChartContentGenerator.ASSIGNMENT_DAYS);
        }
        SectorEntityToken destination = entity(convoy.destinationId());
        if (destination != null && convoy.hasCache()) {
            String planet = destination.getName();
            tellPlayer(() -> I18n.forGameText(() -> Translation.msg("chart.anomaly.supply_convoy.delivered").arg("planet", planet).text()), Misc::getHighlightColor);
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
}
