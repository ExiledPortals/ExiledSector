package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.TreeViewport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class FleetTraffic {

    static final int MAX_ACTIVE_BATTLES = 3;

    private static final class Respawn {
        private final String homeRegion;
        private float secondsLeft;

        private Respawn(String homeRegion, float secondsLeft) {
            this.homeRegion = homeRegion;
            this.secondsLeft = secondsLeft;
        }
    }

    private final Random random;
    private final float minRespawnSeconds;
    private final float maxRespawnSeconds;
    private final Consumer<String> respawnIn;
    private final SpriteCache spriteCache = new SpriteCache(FleetTraffic.class);
    private final DrawnShips drawnShips = new DrawnShips(spriteCache);
    private final BattleEffects effects = new BattleEffects();
    private final List<FactionFleet> fleets = new ArrayList<>();
    private final List<FleetBattle> battles = new ArrayList<>();
    private final List<Respawn> respawns = new ArrayList<>();

    FleetTraffic(Random random, float minRespawnSeconds, float maxRespawnSeconds, Consumer<String> respawnIn) {
        this.random = random;
        this.minRespawnSeconds = minRespawnSeconds;
        this.maxRespawnSeconds = maxRespawnSeconds;
        this.respawnIn = respawnIn;
    }

    Random random() {
        return random;
    }

    int fleetCount() {
        return fleets.size();
    }

    int fleetAndRespawnCount() {
        return fleets.size() + respawns.size();
    }

    void removeFleetsIf(Predicate<FactionFleet> gone) {
        fleets.removeIf(gone);
    }

    FactionFleet addFleet(FactionFleet fleet, List<ShipHullSpecAPI> factionHulls, float fleetRadius) {
        List<ShipHullSpecAPI> hulls = FleetHulls.pickHulls(factionHulls, random);
        List<RoamingShip> ships = FleetFormation.of(hulls, fleetRadius).shipsLargestFirst(hulls, spriteCache, fleet.flight().facingDeg(), random);
        if (ships.isEmpty()) {
            return null;
        }
        fleet.addShips(ships, fleetRadius);
        fleets.add(fleet);
        return fleet;
    }

    boolean startBattle(FactionFleet fleet, Predicate<FactionFleet> eligibleEnemy) {
        if (battles.size() >= MAX_ACTIVE_BATTLES || !fleet.isFree()) {
            return false;
        }
        List<FactionFleet> enemies = new ArrayList<>();
        for (FactionFleet other : fleets) {
            if (other != fleet && other.isFree() && eligibleEnemy.test(other)) {
                enemies.add(other);
            }
        }
        if (enemies.isEmpty()) {
            return false;
        }
        battles.add(FleetBattle.start(fleet, enemies.get(random.nextInt(enemies.size())), random));
        return true;
    }

    void advance(float amount) {
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
            FactionFleet fleet = iterator.next();
            if (fleet.isDestroyed()) {
                iterator.remove();
                respawns.add(new Respawn(fleet.homeRegion(), minRespawnSeconds + random.nextFloat() * (maxRespawnSeconds - minRespawnSeconds)));
            }
        }
        effects.advance(amount);
        for (int index = respawns.size() - 1; index >= 0; index--) {
            Respawn respawn = respawns.get(index);
            respawn.secondsLeft -= amount;
            if (respawn.secondsLeft <= 0f) {
                respawns.remove(index);
                respawnIn.accept(respawn.homeRegion);
            }
        }
    }

    void render(TreeViewport viewport, float alphaMult) {
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
}
