package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AtlasShipGroup {

    public enum Mode { UNDER_WAY, DORMANT, STATION, SWIMMING, CIRCLING }

    static final float SHIP_SCALE = 4.4f;
    private static final float ENGINE_LEVEL = 0.6f;
    private static final float WRECK_REACH = 0.8f;
    private static final float STATION_TURN_DEGREES_PER_SECOND = 0.3f;
    private static final Logger LOG = Logger.getLogger(AtlasShipGroup.class);

    private final SpriteCache spriteCache = new SpriteCache(AtlasShipGroup.class);
    private final DrawnShips drawnShips = new DrawnShips(spriteCache);
    private final Mode mode;
    private final List<String> hullIds;
    private final float radius;
    private final long seed;
    private List<RoamingShip> ships;
    private List<ErraticOrbit> orbits;
    private List<Wreck> wrecks;
    private AtlasDebrisField debris;
    private AtlasShroudedPod pod;
    private float seconds;

    private record Wreck(String spritePath, float width, float height, float scale, HullEngines engines, float offsetX, float offsetY,
                         float facingDeg, float tumble) {
    }

    public AtlasShipGroup(Mode mode, List<String> hullIds, float radius, long seed) {
        this.mode = mode;
        this.hullIds = List.copyOf(hullIds);
        this.radius = radius;
        this.seed = seed;
    }

    public void advance(float amount, float headingDeg) {
        seconds += amount;
        if (ships == null) {
            return;
        }
        for (int index = 0; index < ships.size(); index++) {
            float facingDeg = orbits == null ? headingDeg : orbits.get(index).headingDegreesAt(seconds);
            ships.get(index).drift().advance(amount, facingDeg);
        }
    }

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
        drawnShips.begin(ships.size());
        for (int index = 0; index < ships.size(); index++) {
            RoamingShip ship = ships.get(index);
            float scale = ship.drift().motion().scaleMult() * SHIP_SCALE * zoom;
            ErraticOrbit orbit = orbits == null ? null : orbits.get(index);
            float x = orbit == null ? screenX + ship.drift().offsetX() * zoom : screenX + orbit.xAt(seconds) * zoom;
            float y = orbit == null ? screenY - ship.drift().offsetY() * zoom : screenY + orbit.yAt(seconds) * zoom;
            if (drawnShips.add(ship, x, y, scale, ENGINE_LEVEL, alpha)) {
                ship.drawHull(spriteCache, x, y, scale, alpha);
            }
        }
        if (!drawnShips.isEmpty()) {
            drawnShips.renderFlames(zoom);
            drawnShips.renderGlows();
        }
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
            case DORMANT -> buildDormant(hulls, random);
            case STATION -> buildStation(hulls);
            case SWIMMING -> pod = new AtlasShroudedPod(spriteCache, hulls, radius, random);
            case CIRCLING -> buildCircling(hulls, random);
        }
    }

    private void buildCircling(List<ShipHullSpecAPI> hulls, Random random) {
        orbits = new ArrayList<>();
        for (ShipHullSpecAPI hull : hulls) {
            String spritePath = hull.getSpriteName();
            SpriteAPI texture = spritePath == null ? null : spriteCache.texture(spritePath);
            if (texture == null) {
                continue;
            }
            ErraticOrbit orbit = ErraticOrbit.around(radius, random, orbits.size() % 2 == 1);
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

    private void buildDormant(List<ShipHullSpecAPI> hulls, Random random) {
        for (int index = 0; index < hulls.size(); index++) {
            ShipHullSpecAPI hull = hulls.get(index);
            float reach = index == 0 ? 0f : (float) Math.sqrt(random.nextFloat()) * radius * WRECK_REACH;
            double angle = random.nextDouble() * Math.PI * 2.0;
            addWreck(hull, FleetShipDrift.HullMotion.of(hull.getHullSize()).scaleMult() * SHIP_SCALE, reach * (float) Math.cos(angle),
                    reach * (float) Math.sin(angle), random.nextFloat() * 360f, 0f);
        }
        debris = new AtlasDebrisField(spriteCache, radius, seed);
    }

    private void buildStation(List<ShipHullSpecAPI> hulls) {
        if (hulls.isEmpty()) {
            return;
        }
        ShipHullSpecAPI hull = hulls.get(0);
        SpriteAPI texture = hull.getSpriteName() == null ? null : spriteCache.texture(hull.getSpriteName());
        if (texture == null) {
            return;
        }
        float diagonal = (float) Math.hypot(texture.getWidth(), texture.getHeight());
        if (diagonal > 0f) {
            addWreck(hull, radius * 2f / diagonal, 0f, 0f, 90f, STATION_TURN_DEGREES_PER_SECOND);
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

    private static ShipHullSpecAPI hullSpec(String hullId) {
        try {
            return Global.getSettings().getHullSpec(hullId);
        } catch (RuntimeException e) {
            LOG.warn("[ExiledSector] atlas fleet hull \"" + hullId + "\" is missing");
            return null;
        }
    }
}
