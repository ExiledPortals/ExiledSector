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

public final class AtlasShipGroup {

    public enum Mode { UNDER_WAY, DORMANT, STATION }

    static final float SHIP_SCALE = 2.2f;
    private static final float ENGINE_LEVEL = 0.6f;
    private static final String[] DEBRIS_PATHS = {
            "graphics/debris/debris_lrg0.png", "graphics/debris/debris_lrg1.png", "graphics/debris/debris_med0.png", "graphics/debris/debris_med1.png",
            "graphics/debris/debris_med2.png", "graphics/debris/debris_sml0.png", "graphics/debris/debris_sml1.png", "graphics/debris/debris_sml2.png",
            "graphics/debris/debris_sml3.png"};
    private static final String DUST_PATH = "graphics/fx/glow64.png";
    private static final int DEBRIS_PIECES = 18;
    private static final float DEBRIS_REACH = 1.5f;
    private static final float DEBRIS_MIN_SIZE = 4f;
    private static final float DEBRIS_SIZE_SPREAD = 10f;
    private static final float DEBRIS_ALPHA = 0.8f;
    private static final float DUST_SCALE = 3.4f;
    private static final float DUST_ALPHA = 0.18f;
    private static final float WRECK_REACH = 0.8f;
    private static final float TUMBLE_DEGREES_PER_SECOND = 5f;
    private static final float STATION_TURN_DEGREES_PER_SECOND = 1.5f;
    private static final Color DEBRIS_TINT = new Color(150, 142, 132);
    private static final Color DUST_TINT = new Color(120, 105, 95);
    private static final Logger LOG = Logger.getLogger(AtlasShipGroup.class);

    private final SpriteCache spriteCache = new SpriteCache(AtlasShipGroup.class);
    private final DrawnShips drawnShips = new DrawnShips(spriteCache);
    private final Mode mode;
    private final List<String> hullIds;
    private final float radius;
    private final long seed;
    private List<RoamingShip> ships;
    private List<Wreck> wrecks;
    private List<Debris> debris;
    private float seconds;

    private record Wreck(String spritePath, float width, float height, float scale, HullEngines engines, float offsetX, float offsetY,
                         float facingDeg, float tumble) {
    }

    private record Debris(String path, float offsetX, float offsetY, float size, float facingDeg, float tumble) {
    }

    public AtlasShipGroup(Mode mode, List<String> hullIds, float radius, long seed) {
        this.mode = mode;
        this.hullIds = List.copyOf(hullIds);
        this.radius = radius;
        this.seed = seed;
    }

    public void advance(float amount, float headingDeg) {
        seconds += amount;
        if (ships != null) {
            for (RoamingShip ship : ships) {
                ship.drift().advance(amount, headingDeg);
            }
        }
    }

    public void render(float screenX, float screenY, float zoom, float headingDeg, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        build(headingDeg);
        if (debris != null) {
            renderDebris(screenX, screenY, zoom, alpha);
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
        for (RoamingShip ship : ships) {
            float scale = ship.drift().motion().scaleMult() * SHIP_SCALE * zoom;
            float x = screenX + ship.drift().offsetX() * zoom;
            float y = screenY - ship.drift().offsetY() * zoom;
            if (drawnShips.add(ship, x, y, scale, ENGINE_LEVEL, alpha)) {
                ship.drawHull(spriteCache, x, y, scale, alpha);
            }
        }
        if (!drawnShips.isEmpty()) {
            drawnShips.renderFlames(zoom);
            drawnShips.renderGlows();
        }
    }

    private void renderDebris(float screenX, float screenY, float zoom, float alpha) {
        float dustSize = radius * DUST_SCALE * zoom;
        SpriteDraw.drawAdditiveAtCenter(spriteCache, DUST_PATH, screenX, screenY, dustSize, dustSize, DUST_TINT, alpha * DUST_ALPHA);
        for (Debris piece : debris) {
            SpriteAPI sprite = spriteCache.sprite(piece.path());
            if (sprite == null || sprite.getWidth() <= 0f || sprite.getHeight() <= 0f) {
                continue;
            }
            float longest = Math.max(sprite.getWidth(), sprite.getHeight());
            float scale = piece.size() * zoom / longest;
            SpriteDraw.drawAtCenter(sprite, screenX + piece.offsetX() * zoom, screenY - piece.offsetY() * zoom, sprite.getWidth() * scale,
                    sprite.getHeight() * scale, DEBRIS_TINT, alpha * DEBRIS_ALPHA, piece.facingDeg() + piece.tumble() * seconds);
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
            float tumble = (random.nextFloat() * 2f - 1f) * TUMBLE_DEGREES_PER_SECOND;
            addWreck(hull, FleetShipDrift.HullMotion.of(hull.getHullSize()).scaleMult() * SHIP_SCALE, reach * (float) Math.cos(angle),
                    reach * (float) Math.sin(angle), random.nextFloat() * 360f, tumble);
        }
        debris = new ArrayList<>();
        for (int piece = 0; piece < DEBRIS_PIECES; piece++) {
            float reach = (float) Math.sqrt(random.nextFloat()) * radius * DEBRIS_REACH;
            double angle = random.nextDouble() * Math.PI * 2.0;
            debris.add(new Debris(DEBRIS_PATHS[random.nextInt(DEBRIS_PATHS.length)], reach * (float) Math.cos(angle), reach * (float) Math.sin(angle),
                    DEBRIS_MIN_SIZE + random.nextFloat() * DEBRIS_SIZE_SPREAD, random.nextFloat() * 360f,
                    (random.nextFloat() * 2f - 1f) * TUMBLE_DEGREES_PER_SECOND * 2f));
        }
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
        float longest = Math.max(texture.getWidth(), texture.getHeight());
        if (longest > 0f) {
            addWreck(hull, radius * 2f / longest, 0f, 0f, 90f, STATION_TURN_DEGREES_PER_SECOND);
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
