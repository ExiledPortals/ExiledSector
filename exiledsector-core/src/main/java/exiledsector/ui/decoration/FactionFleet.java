package exiledsector.ui.decoration;

import exiledlib.skills.skilleffect.render.Fade;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

final class FactionFleet {

    static final float SPAWN_FADE_SECONDS = 2f;

    private final String factionId;
    private final Color factionColor;
    private final String homeRegion;
    private final RoamVolume home;
    private final List<RoamVolume> alliedVolumes;
    private final FleetFlight flight;
    private final List<RoamingShip> ships = new ArrayList<>();
    private float cullRadius;
    private float spawnLevel;
    private FleetBattle battle;

    FactionFleet(String factionId, Color factionColor, String homeRegion, RoamVolume home, List<RoamVolume> alliedVolumes,
                 float travelSpeed, Random random, BiFunction<FactionFleet, RoamVolume, RoamVolume> arrivalCourse) {
        this.factionId = factionId;
        this.factionColor = factionColor;
        this.homeRegion = homeRegion;
        this.home = home;
        this.alliedVolumes = List.copyOf(alliedVolumes);
        this.flight = new FleetFlight(home, travelSpeed, random, current -> arrivalCourse.apply(this, current));
    }

    void addShips(List<RoamingShip> shipsLargestFirst, float fleetRadius) {
        ships.addAll(shipsLargestFirst);
        cullRadius = fleetRadius + (float) shipsLargestFirst.stream().mapToDouble(RoamingShip::longestSide).max().orElse(0.0);
    }

    void advance(float amount) {
        spawnLevel = Fade.step(spawnLevel, true, amount / SPAWN_FADE_SECONDS);
        flight.advance(amount);
        for (RoamingShip ship : ships) {
            ship.drift().advance(amount, flight.facingDeg());
        }
    }

    void showFullySpawned() {
        spawnLevel = 1f;
    }

    float shipX(RoamingShip ship) {
        return flight.positionX() + ship.drift().offsetX();
    }

    float shipY(RoamingShip ship) {
        return flight.positionY() + ship.drift().offsetY();
    }

    void destroy(RoamingShip ship) {
        ships.remove(ship);
    }

    boolean isDestroyed() {
        return ships.isEmpty();
    }

    boolean isFree() {
        return battle == null && !ships.isEmpty() && spawnLevel >= 1f;
    }

    String factionId() {
        return factionId;
    }

    Color factionColor() {
        return factionColor;
    }

    String homeRegion() {
        return homeRegion;
    }

    RoamVolume home() {
        return home;
    }

    List<RoamVolume> alliedVolumes() {
        return alliedVolumes;
    }

    FleetFlight flight() {
        return flight;
    }

    List<RoamingShip> ships() {
        return ships;
    }

    float cullRadius() {
        return cullRadius;
    }

    float spawnLevel() {
        return spawnLevel;
    }

    FleetBattle battle() {
        return battle;
    }

    void setBattle(FleetBattle battle) {
        this.battle = battle;
    }
}
