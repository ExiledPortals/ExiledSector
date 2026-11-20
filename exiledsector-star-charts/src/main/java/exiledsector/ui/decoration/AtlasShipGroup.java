package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AtlasShipGroup implements AtlasShipScene {

    public enum Mode { UNDER_WAY, DORMANT, STATION, SWIMMING, CIRCLING, CROSSING, GUARDING, PATROLLING }

    static final float SHIP_SCALE = 4.4f;
    private static final float ENGINE_LEVEL = 0.6f;
    static final float DORMANT_SHIP_SHARE = 0.3f;
    static final float DORMANT_PACE = 0.35f;
    private static final float DORMANT_FORMATION_SHARE = 0.5f;
    private static final float STATION_TURN_DEGREES_PER_SECOND = 0.3f;
    static final float PICKET_SHIP_SHARE = 0.37f;
    public static final float STATION_SHARE = 0.25f;
    static final float GUARD_ORBIT_SHARE = 0.45f;
    static final float PATROL_ORBIT_SHARE = 0.4f;
    static final String PING_TEXTURE = "graphics/fx/shields256ringd.png";
    static final float PING_SECONDS = 1.6f;
    static final float MIN_PING_INTERVAL = 5f;
    static final float EXTRA_PING_INTERVAL = 4f;
    static final float PING_REACH_SHARE = 1.5f;
    private static final float PING_ALPHA = 0.7f;
    private static final Logger LOG = Logger.getLogger(AtlasShipGroup.class);

    private final SpriteCache spriteCache = new SpriteCache(AtlasShipGroup.class);
    private final DrawnShips drawnShips = new DrawnShips(spriteCache);
    private final Mode mode;
    private final List<String> hullIds;
    private final float radius;
    private final long seed;
    private final Color pingColour;
    private List<RoamingShip> ships;
    private List<ErraticOrbit> orbits;
    private ConvoyCrossing crossing;
    private List<Wreck> wrecks;
    private AtlasDebrisField debris;
    private AtlasShroudedPod pod;
    private List<SensorPing> pings = List.of();
    private float seconds;

    private record Wreck(String spritePath, float width, float height, float scale, HullEngines engines, float offsetX, float offsetY,
                         float facingDeg, float tumble) {
    }

    record SensorPing(float intervalSeconds, float phaseSeconds) {

        float progressAt(float seconds) {
            float sinceLast = ((seconds + phaseSeconds) % intervalSeconds + intervalSeconds) % intervalSeconds;
            return sinceLast < PING_SECONDS ? sinceLast / PING_SECONDS : -1f;
        }
    }

    public AtlasShipGroup(Mode mode, List<String> hullIds, float radius, long seed) {
        this(mode, hullIds, radius, seed, Color.WHITE);
    }

    public AtlasShipGroup(Mode mode, List<String> hullIds, float radius, long seed, Color pingColour) {
        this.mode = mode;
        this.hullIds = List.copyOf(hullIds);
        this.radius = radius;
        this.seed = seed;
        this.pingColour = pingColour;
    }

    @Override
    public void advance(float amount, float headingDeg) {
        seconds += amount;
        if (ships == null) {
            return;
        }
        float shipAmount = mode == Mode.DORMANT ? amount * DORMANT_PACE : amount;
        for (int index = 0; index < ships.size(); index++) {
            float facingDeg = orbits == null ? headingDeg : orbits.get(index).headingDegreesAt(seconds);
            ships.get(index).drift().advance(shipAmount, facingDeg);
        }
    }

    @Override
    public void render(float screenX, float screenY, float zoom, float headingDeg, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        build(headingDeg);
        if (debris != null) {
            debris.render(screenX, screenY, zoom, seconds, alpha);
        }
        if (pod != null) {
            pod.render(screenX, screenY, zoom, seconds, alpha);
        }
        for (Wreck wreck : wrecks) {
            float x = screenX + wreck.offsetX() * zoom;
            float y = screenY - wreck.offsetY() * zoom;
            float scale = wreck.scale() * zoom;
            wreck.engines().drawHull(spriteCache, wreck.spritePath(), x, y, wreck.width() * scale, wreck.height() * scale, scale,
                    wreck.facingDeg() + wreck.tumble() * seconds, alpha);
        }
        if (ships.isEmpty()) {
            return;
        }
        boolean dormant = mode == Mode.DORMANT;
        float shipZoom = zoom * shipShare();
        drawnShips.begin(ships.size());
        for (int index = 0; index < ships.size(); index++) {
            RoamingShip ship = ships.get(index);
            float scale = ship.drift().motion().scaleMult() * SHIP_SCALE * shipZoom;
            float x = screenX + offsetXOf(index, ship, headingDeg) * zoom;
            float y = screenY + offsetYOf(index, ship, headingDeg) * zoom;
            float shipAlpha = crossing == null ? alpha : alpha * crossing.visibilityAt(index, seconds);
            if (drawnShips.add(ship, x, y, scale, dormant ? 0f : ENGINE_LEVEL, shipAlpha)) {
                ship.drawHull(spriteCache, x, y, scale, shipAlpha);
            }
            if (index < pings.size()) {
                renderPing(pings.get(index), x, y, zoom, shipAlpha);
            }
        }
        if (!dormant && !drawnShips.isEmpty()) {
            drawnShips.renderFlames(shipZoom);
            drawnShips.renderGlows();
        }
    }

    private float shipShare() {
        return switch (mode) {
            case DORMANT -> DORMANT_SHIP_SHARE;
            case STATION, GUARDING, PATROLLING -> PICKET_SHIP_SHARE;
            default -> 1f;
        };
    }

    private void renderPing(SensorPing ping, float x, float y, float zoom, float alpha) {
        float progress = ping.progressAt(seconds);
        if (progress < 0f) {
            return;
        }
        float size = 2f * radius * PING_REACH_SHARE * progress * zoom;
        SpriteDraw.drawAdditiveAtCenter(spriteCache, PING_TEXTURE, x, y, size, size, pingColour, alpha * PING_ALPHA * (1f - progress));
    }

    private float offsetXOf(int index, RoamingShip ship, float headingDeg) {
        if (orbits != null) {
            return orbits.get(index).xAt(seconds);
        }
        return crossing == null ? ship.drift().offsetX() : crossing.xAt(index, seconds, headingDeg);
    }

    private float offsetYOf(int index, RoamingShip ship, float headingDeg) {
        if (orbits != null) {
            return orbits.get(index).yAt(seconds);
        }
        return crossing == null ? -ship.drift().offsetY() : crossing.yAt(index, seconds, headingDeg);
    }

    private void build(float headingDeg) {
        if (ships != null) {
            return;
        }
        Random random = new Random(seed);
        List<ShipHullSpecAPI> hulls = new ArrayList<>();
        for (String hullId : hullIds) {
            ShipHullSpecAPI hull = hullSpec(hullId);
            if (hull != null) {
                hulls.add(hull);
            }
        }
        ships = new ArrayList<>();
        wrecks = new ArrayList<>();
        switch (mode) {
            case UNDER_WAY -> buildUnderWay(hulls, headingDeg, random);
            case DORMANT -> buildDormant(hulls, headingDeg, random);
            case STATION -> buildStation(hulls, random);
            case SWIMMING -> {
                pod = new AtlasShroudedPod(spriteCache, hulls, radius, random);
                debris = new AtlasDebrisField(spriteCache, radius, seed);
            }
            case CIRCLING -> buildCircling(hulls, radius, random);
            case GUARDING -> buildCircling(hulls, radius * GUARD_ORBIT_SHARE, random);
            case PATROLLING -> buildPatrol(hulls, random);
            case CROSSING -> buildCrossing(hulls, headingDeg, random);
        }
    }

    private void buildCrossing(List<ShipHullSpecAPI> hulls, float headingDeg, Random random) {
        for (ShipHullSpecAPI hull : hulls) {
            String spritePath = hull.getSpriteName();
            SpriteAPI texture = spritePath == null ? null : spriteCache.texture(spritePath);
            if (texture != null) {
                FleetShipDrift drift = new FleetShipDrift(FleetShipDrift.HullMotion.of(hull.getHullSize()), 0f, headingDeg, random);
                ships.add(new RoamingShip(spritePath, texture.getWidth(), texture.getHeight(), hull.getHullSize(), drift, HullEngines.of(hull)));
            }
        }
        crossing = new ConvoyCrossing(radius, ships.size());
    }

    private void buildPatrol(List<ShipHullSpecAPI> hulls, Random random) {
        buildCircling(hulls, radius * PATROL_ORBIT_SHARE, random);
        List<SensorPing> schedule = new ArrayList<>();
        for (int index = 0; index < ships.size(); index++) {
            float interval = MIN_PING_INTERVAL + random.nextFloat() * EXTRA_PING_INTERVAL;
            schedule.add(new SensorPing(interval, random.nextFloat() * interval));
        }
        pings = schedule;
    }

    private void buildCircling(List<ShipHullSpecAPI> hulls, float orbitRadius, Random random) {
        orbits = new ArrayList<>();
        for (ShipHullSpecAPI hull : hulls) {
            String spritePath = hull.getSpriteName();
            SpriteAPI texture = spritePath == null ? null : spriteCache.texture(spritePath);
            if (texture == null) {
                continue;
            }
            ErraticOrbit orbit = ErraticOrbit.around(orbitRadius, random, orbits.size() % 2 == 1);
            FleetShipDrift drift = new FleetShipDrift(FleetShipDrift.HullMotion.of(hull.getHullSize()), 0f, orbit.headingDegreesAt(0f), random);
            ships.add(new RoamingShip(spritePath, texture.getWidth(), texture.getHeight(), hull.getHullSize(), drift, HullEngines.of(hull)));
            orbits.add(orbit);
        }
    }

    private void buildUnderWay(List<ShipHullSpecAPI> hulls, float headingDeg, Random random) {
        FleetFormation formation = FleetFormation.of(hulls, radius);
        for (ShipHullSpecAPI hull : hulls) {
            RoamingShip ship = formation.ship(hull, spriteCache, headingDeg, random);
            if (ship != null) {
                ships.add(ship);
            }
        }
        ships.sort(FleetFormation.LARGEST_FIRST);
    }

    private void buildDormant(List<ShipHullSpecAPI> hulls, float headingDeg, Random random) {
        ships.addAll(FleetFormation.of(hulls, radius * DORMANT_FORMATION_SHARE).shipsLargestFirst(hulls, spriteCache, headingDeg, random));
        debris = new AtlasDebrisField(spriteCache, radius, seed);
    }

    private void buildStation(List<ShipHullSpecAPI> hulls, Random random) {
        if (hulls.isEmpty()) {
            return;
        }
        buildCircling(hulls.subList(1, hulls.size()), radius * GUARD_ORBIT_SHARE, random);
        ShipHullSpecAPI hull = hulls.get(0);
        SpriteAPI texture = hull.getSpriteName() == null ? null : spriteCache.texture(hull.getSpriteName());
        if (texture == null) {
            return;
        }
        float diagonal = (float) Math.hypot(texture.getWidth(), texture.getHeight());
        if (diagonal > 0f) {
            addWreck(hull, radius * STATION_SHARE * 2f / diagonal, 0f, 0f, 90f, STATION_TURN_DEGREES_PER_SECOND);
        }
    }

    private void addWreck(ShipHullSpecAPI hull, float scale, float offsetX, float offsetY, float facingDeg, float tumble) {
        String spritePath = hull.getSpriteName();
        SpriteAPI texture = spritePath == null ? null : spriteCache.texture(spritePath);
        if (texture != null) {
            wrecks.add(new Wreck(spritePath, texture.getWidth(), texture.getHeight(), scale, HullEngines.of(hull), offsetX, offsetY, facingDeg,
                    tumble));
        }
    }

    static ShipHullSpecAPI hullSpec(String hullId) {
        try {
            return Global.getSettings().getHullSpec(hullId);
        } catch (RuntimeException e) {
            LOG.warn("[ExiledSector] atlas fleet hull \"" + hullId + "\" is missing");
            return null;
        }
    }
}
