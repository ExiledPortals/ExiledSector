package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.TreeViewport;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class LaneFleetTraffic {

    public record Port(long id, float x, float y, float innerRadius, float outerRadius, String factionId, List<Long> neighbours) {

        public Port {
            neighbours = List.copyOf(neighbours);
        }
    }

    public interface Factions {

        List<ShipHullSpecAPI> hulls(String factionId);

        Color colour(String factionId);

        boolean friendly(String factionId, String otherFactionId);

        boolean hostile(String factionId, String otherFactionId);
    }

    static final float FLEET_RADIUS = 40f;
    static final float MIN_TRAVEL_SPEED = 160f;
    static final float MAX_TRAVEL_SPEED = 280f;
    static final float PORTS_PER_FLEET = 3f;
    static final int MAX_FLEETS = 12;
    static final float MIN_RESPAWN_SECONDS = 15f;
    static final float MAX_RESPAWN_SECONDS = 35f;
    static final float VISIT_CHANCE = 0.25f;
    static final float RETURN_CHANCE = 0.5f;
    static final float ENCOUNTER_CHANCE = 0.35f;
    static final int MAX_ACTIVE_BATTLES = 3;
    static final RepLevel FRIENDLY_LEVEL = RepLevel.COOPERATIVE;

    private final Factions factions;
    private final Random random;
    private final SpriteCache spriteCache = new SpriteCache(LaneFleetTraffic.class);
    private final DrawnShips drawnShips = new DrawnShips(spriteCache);
    private final BattleEffects effects = new BattleEffects();
    private final Map<Long, Port> ports = new LinkedHashMap<>();
    private final Map<Long, RoamVolume> volumes = new HashMap<>();
    private final Map<RoamVolume, Port> portsByVolume = new IdentityHashMap<>();
    private final List<FactionFleet> fleets = new ArrayList<>();
    private final List<FleetBattle> battles = new ArrayList<>();
    private final List<Float> respawns = new ArrayList<>();
    private boolean seeded;

    public LaneFleetTraffic(Factions factions, Random random) {
        this.factions = factions;
        this.random = random;
    }

    public static Factions sectorFactions() {
        return new SectorFactions();
    }

    public void setPorts(Collection<Port> newPorts) {
        ports.clear();
        portsByVolume.clear();
        for (Port port : newPorts) {
            ports.put(port.id(), port);
            RoamVolume volume = volumes.get(port.id());
            RoamVolume wanted = new RoamVolume(port.x(), port.y(), port.innerRadius(), port.outerRadius());
            if (!wanted.equals(volume)) {
                volume = wanted;
                volumes.put(port.id(), volume);
            }
            portsByVolume.put(volume, port);
        }
        fleets.removeIf(fleet -> fleet.battle() == null && !portsByVolume.containsKey(fleet.home()));
        int missing = targetFleets(ports.values(), factions) - fleets.size() - respawns.size();
        for (int index = 0; index < missing; index++) {
            FactionFleet fleet = spawn();
            if (fleet != null && !seeded) {
                fleet.showFullySpawned();
            }
        }
        seeded = true;
    }

    public void advance(float amount) {
        for (FactionFleet fleet : fleets) {
            fleet.advance(amount);
        }
        for (Iterator<FleetBattle> iterator = battles.iterator(); iterator.hasNext(); ) {
            FleetBattle battle = iterator.next();
            battle.advance(amount, random, effects);
            if (battle.isOver()) {
                iterator.remove();
            }
        }
        for (Iterator<FactionFleet> iterator = fleets.iterator(); iterator.hasNext(); ) {
            if (iterator.next().isDestroyed()) {
                iterator.remove();
                respawns.add(MIN_RESPAWN_SECONDS + random.nextFloat() * (MAX_RESPAWN_SECONDS - MIN_RESPAWN_SECONDS));
            }
        }
        effects.advance(amount);
        for (int index = respawns.size() - 1; index >= 0; index--) {
            float left = respawns.get(index) - amount;
            if (left > 0f) {
                respawns.set(index, left);
            } else {
                respawns.remove(index);
                spawn();
            }
        }
    }

    public void render(TreeViewport viewport, float alphaMult) {
        if ((fleets.isEmpty() && effects.isEmpty()) || alphaMult <= 0f) {
            return;
        }
        float zoom = viewport.zoom();
        int shipCount = 0;
        for (FactionFleet fleet : fleets) {
            shipCount += fleet.ships().size();
        }
        drawnShips.begin(shipCount);
        for (FactionFleet fleet : fleets) {
            FleetFlight flight = fleet.flight();
            float fleetAlpha = alphaMult * fleet.spawnLevel();
            if (fleetAlpha <= 0f
                    || !viewport.isVisible(viewport.screenX(flight.positionX()), viewport.screenY(flight.positionY()), fleet.cullRadius() * zoom)) {
                continue;
            }
            for (RoamingShip ship : fleet.ships()) {
                float screenScale = ship.drift().motion().scaleMult() * zoom;
                float screenX = viewport.screenX(fleet.shipX(ship));
                float screenY = viewport.screenY(fleet.shipY(ship));
                if (drawnShips.add(ship, screenX, screenY, screenScale, flight.engineLevel(), fleetAlpha)) {
                    ship.drawHull(spriteCache, screenX, screenY, screenScale, fleetAlpha);
                }
            }
        }
        drawnShips.renderFlames(zoom);
        if (!drawnShips.isEmpty() || !effects.isEmpty()) {
            drawnShips.renderGlows(quads -> effects.render(quads, viewport, alphaMult));
        }
    }

    int fleetCount() {
        return fleets.size();
    }

    static int targetFleets(Collection<Port> ports, Factions factions) {
        int crewed = 0;
        for (Port port : ports) {
            crewed += port.factionId() != null && !factions.hulls(port.factionId()).isEmpty() ? 1 : 0;
        }
        return Math.min(MAX_FLEETS, (int) Math.ceil(crewed / PORTS_PER_FLEET));
    }

    static Port visitTarget(Port here, String factionId, Map<Long, Port> ports, Factions factions, Random random) {
        List<Port> friendly = new ArrayList<>();
        for (long neighbourId : here.neighbours()) {
            Port neighbour = ports.get(neighbourId);
            if (neighbour != null && neighbour.factionId() != null
                    && (neighbour.factionId().equals(factionId) || factions.friendly(factionId, neighbour.factionId()))) {
                friendly.add(neighbour);
            }
        }
        return friendly.isEmpty() ? null : friendly.get(random.nextInt(friendly.size()));
    }

    private FactionFleet spawn() {
        List<Port> crewed = new ArrayList<>();
        for (Port port : ports.values()) {
            if (port.factionId() != null && !factions.hulls(port.factionId()).isEmpty()) {
                crewed.add(port);
            }
        }
        if (crewed.isEmpty()) {
            return null;
        }
        Port home = crewed.get(random.nextInt(crewed.size()));
        float travelSpeed = MIN_TRAVEL_SPEED + random.nextFloat() * (MAX_TRAVEL_SPEED - MIN_TRAVEL_SPEED);
        Color colour = factions.colour(home.factionId());
        FactionFleet fleet = new FactionFleet(home.factionId(), colour == null ? Color.WHITE : colour, String.valueOf(home.id()),
                volumes.get(home.id()), List.of(), travelSpeed, random, this::arrive);
        List<ShipHullSpecAPI> hulls = FleetHulls.pickHulls(factions.hulls(home.factionId()), random);
        List<RoamingShip> ships = FleetFormation.of(hulls, FLEET_RADIUS).shipsLargestFirst(hulls, spriteCache, fleet.flight().facingDeg(), random);
        if (ships.isEmpty()) {
            return null;
        }
        fleet.addShips(ships, FLEET_RADIUS);
        fleets.add(fleet);
        return fleet;
    }

    private RoamVolume arrive(FactionFleet fleet, RoamVolume current) {
        Port here = portsByVolume.get(current);
        if (here == null) {
            return fleet.home();
        }
        if (random.nextFloat() < ENCOUNTER_CHANCE && startBattle(fleet, here)) {
            return current;
        }
        if (current != fleet.home() && random.nextFloat() < RETURN_CHANCE) {
            return fleet.home();
        }
        if (random.nextFloat() < VISIT_CHANCE) {
            Port target = visitTarget(here, fleet.factionId(), ports, factions, random);
            if (target != null) {
                return volumes.get(target.id());
            }
        }
        return current;
    }

    private boolean startBattle(FactionFleet fleet, Port here) {
        if (battles.size() >= MAX_ACTIVE_BATTLES || !fleet.isFree()) {
            return false;
        }
        List<FactionFleet> enemies = new ArrayList<>();
        for (FactionFleet other : fleets) {
            Port at = portsByVolume.get(other.flight().roamVolume());
            if (other != fleet && other.isFree() && at != null && here.neighbours().contains(at.id())
                    && factions.hostile(fleet.factionId(), other.factionId())) {
                enemies.add(other);
            }
        }
        if (enemies.isEmpty()) {
            return false;
        }
        battles.add(FleetBattle.start(fleet, enemies.get(random.nextInt(enemies.size())), random));
        return true;
    }

    private static final class SectorFactions implements Factions {
        private final Map<String, List<ShipHullSpecAPI>> hulls = new HashMap<>();

        @Override
        public List<ShipHullSpecAPI> hulls(String factionId) {
            return Global.getSector() == null ? List.of() : hulls.computeIfAbsent(factionId, FleetHulls::roamingHulls);
        }

        @Override
        public Color colour(String factionId) {
            FactionAPI faction = faction(factionId);
            return faction == null ? null : faction.getColor();
        }

        @Override
        public boolean friendly(String factionId, String otherFactionId) {
            FactionAPI faction = faction(factionId);
            return faction != null && faction.isAtWorst(otherFactionId, FRIENDLY_LEVEL);
        }

        @Override
        public boolean hostile(String factionId, String otherFactionId) {
            FactionAPI faction = faction(factionId);
            return faction != null && faction.isHostileTo(otherFactionId);
        }

        private static FactionAPI faction(String factionId) {
            return Global.getSector() == null || factionId == null ? null : Global.getSector().getFaction(factionId);
        }
    }
}
