package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.compat.NexerelinCompat;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.hyperspace.HyperspaceAnchor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class FactionFleetRenderer {

    static final int MIN_FLEETS_PER_VOLUME = 2;
    static final int MAX_FLEETS_PER_VOLUME = 3;
    static final float FLEET_RADIUS = 40f;
    static final float MIN_TRAVEL_SPEED = 90f;
    static final float MAX_TRAVEL_SPEED = 180f;
    static final float VOLUME_TRAVEL_CHANCE = 0.15f;
    static final float ENCOUNTER_CHANCE = 0.15f;
    static final int MAX_ACTIVE_BATTLES = 3;
    static final float MIN_RESPAWN_SECONDS = 30f;
    static final float MAX_RESPAWN_SECONDS = 60f;
    static final RepLevel ALLY_LEVEL = RepLevel.COOPERATIVE;

    private static final class Respawn {
        private final String region;
        private float secondsLeft;

        private Respawn(String region, float secondsLeft) {
            this.region = region;
            this.secondsLeft = secondsLeft;
        }
    }

    private final SpriteCache spriteCache = new SpriteCache(FactionFleetRenderer.class);
    private final DrawnShips drawnShips = new DrawnShips(spriteCache);
    private final Random random = new Random();
    private final Map<String, RoamVolume> volumesByRegion = new LinkedHashMap<>();
    private final Map<String, List<ShipHullSpecAPI>> hullsByFaction = new HashMap<>();
    private final List<FactionFleet> fleets = new ArrayList<>();
    private final List<FleetBattle> battles = new ArrayList<>();
    private final List<Respawn> respawns = new ArrayList<>();
    private final BattleEffects effects = new BattleEffects();

    private FactionFleetRenderer() {
    }

    public static FactionFleetRenderer forActiveTree(Predicate<SkillNode> nodeHidden) {
        FactionFleetRenderer renderer = new FactionFleetRenderer();
        if (Global.getSector() == null) {
            return renderer;
        }
        Collection<SkillNode> nodes = SkillTree.getAllNodes().values();
        Set<String> lockedRegions = HyperspaceAnchor.lockedRegions(nodes, SkillNode::getRegion, nodeHidden);
        for (String region : TreeFleetFactions.regions()) {
            RoamVolume volume = lockedRegions.contains(region) ? null : TreeRoamVolumes.region(region, SkillTree.getDecorations().stars(), nodes);
            if (volume != null) {
                renderer.volumesByRegion.put(region, volume);
            }
        }
        for (String region : renderer.volumesByRegion.keySet()) {
            int fleetCount = MIN_FLEETS_PER_VOLUME + renderer.random.nextInt(MAX_FLEETS_PER_VOLUME - MIN_FLEETS_PER_VOLUME + 1);
            for (int i = 0; i < fleetCount; i++) {
                FactionFleet fleet = renderer.spawn(region);
                if (fleet != null) {
                    fleet.showFullySpawned();
                }
            }
        }
        return renderer;
    }

    static RoamVolume nextVolume(RoamVolume current, RoamVolume home, List<RoamVolume> alliedVolumes, Random random) {
        if (alliedVolumes.isEmpty() || random.nextFloat() >= VOLUME_TRAVEL_CHANCE) {
            return current;
        }
        List<RoamVolume> destinations = new ArrayList<>(alliedVolumes);
        destinations.add(home);
        destinations.remove(current);
        return destinations.get(random.nextInt(destinations.size()));
    }

    private static boolean allied(String factionId, String otherFactionId) {
        if (factionId.equals(otherFactionId)) {
            return false;
        }
        FactionAPI faction = Global.getSector().getFaction(factionId);
        return (faction != null && faction.isAtWorst(otherFactionId, ALLY_LEVEL)) || NexerelinCompat.areAllied(factionId, otherFactionId);
    }

    private static boolean hostile(String factionId, String otherFactionId) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        return faction != null && faction.isHostileTo(otherFactionId);
    }

    private FactionFleet spawn(String region) {
        RoamVolume home = volumesByRegion.get(region);
        List<String> factionIds = TreeFleetFactions.factionsIn(region).stream()
                .filter(factionId -> !hullsByFaction.computeIfAbsent(factionId, FleetHulls::roamingHulls).isEmpty()).toList();
        if (home == null || factionIds.isEmpty()) {
            return null;
        }
        String factionId = factionIds.get(random.nextInt(factionIds.size()));
        FactionAPI faction = Global.getSector().getFaction(factionId);
        Color factionColor = faction.getColor() == null ? Color.WHITE : faction.getColor();
        float travelSpeed = MIN_TRAVEL_SPEED + random.nextFloat() * (MAX_TRAVEL_SPEED - MIN_TRAVEL_SPEED);
        FactionFleet fleet = new FactionFleet(factionId, factionColor, region, home, alliedVolumes(factionId, region), travelSpeed, random,
                this::arrive);
        List<ShipHullSpecAPI> hulls = FleetHulls.pickHulls(hullsByFaction.get(factionId), random);
        List<RoamingShip> ships = FleetFormation.of(hulls, FLEET_RADIUS).shipsLargestFirst(hulls, spriteCache, fleet.flight().facingDeg(), random);
        if (ships.isEmpty()) {
            return null;
        }
        fleet.addShips(ships, FLEET_RADIUS);
        fleets.add(fleet);
        return fleet;
    }

    private List<RoamVolume> alliedVolumes(String factionId, String homeRegion) {
        return volumesByRegion.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(homeRegion)
                        && TreeFleetFactions.factionsIn(entry.getKey()).stream().anyMatch(other -> allied(factionId, other)))
                .map(Map.Entry::getValue).toList();
    }

    private RoamVolume arrive(FactionFleet fleet, RoamVolume current) {
        if (random.nextFloat() < ENCOUNTER_CHANCE && startBattle(fleet)) {
            return current;
        }
        return nextVolume(current, fleet.home(), fleet.alliedVolumes(), random);
    }

    private boolean startBattle(FactionFleet fleet) {
        if (battles.size() >= MAX_ACTIVE_BATTLES || !fleet.isFree()) {
            return false;
        }
        RoamVolume fleetVolume = fleet.flight().roamVolume();
        List<FactionFleet> enemies = fleets.stream()
                .filter(other -> other != fleet && other.isFree() && other.flight().roamVolume() != fleetVolume
                        && hostile(fleet.factionId(), other.factionId()))
                .toList();
        if (enemies.isEmpty()) {
            return false;
        }
        battles.add(FleetBattle.start(fleet, enemies.get(random.nextInt(enemies.size())), random));
        return true;
    }

    public void advance(float amount) {
        for (FactionFleet fleet : fleets) {
            fleet.advance(amount);
        }
        Iterator<FleetBattle> battleIterator = battles.iterator();
        while (battleIterator.hasNext()) {
            FleetBattle battle = battleIterator.next();
            battle.advance(amount, random, effects);
            if (battle.isOver()) {
                battleIterator.remove();
            }
        }
        Iterator<FactionFleet> fleetIterator = fleets.iterator();
        while (fleetIterator.hasNext()) {
            FactionFleet fleet = fleetIterator.next();
            if (fleet.isDestroyed()) {
                fleetIterator.remove();
                respawns.add(new Respawn(fleet.homeRegion(), MIN_RESPAWN_SECONDS + random.nextFloat() * (MAX_RESPAWN_SECONDS - MIN_RESPAWN_SECONDS)));
            }
        }
        effects.advance(amount);
        for (int index = respawns.size() - 1; index >= 0; index--) {
            Respawn respawn = respawns.get(index);
            respawn.secondsLeft -= amount;
            if (respawn.secondsLeft <= 0f) {
                respawns.remove(index);
                spawn(respawn.region);
            }
        }
    }

    public void render(TreeViewport viewport, float alphaMult) {
        if ((fleets.isEmpty() && effects.isEmpty()) || alphaMult <= 0f) return;
        float zoom = viewport.zoom();
        drawnShips.begin(shipCount());
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

    private int shipCount() {
        int shipCount = 0;
        for (FactionFleet fleet : fleets) {
            shipCount += fleet.ships().size();
        }
        return shipCount;
    }
}
