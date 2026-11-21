package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class AtlasFlagshipScene implements AtlasShipScene {

    public enum Scene { COMMAND, ESCORT, GUARD_RING, STANDOFF, TROPHY, SCRAPPERS, SPOILS, TOW, SALVAGE }

    public record Cast(List<String> hulls, List<String> wrecks, List<String> rivals) {

        public Cast {
            hulls = hulls == null ? List.of() : List.copyOf(hulls);
            wrecks = wrecks == null ? List.of() : List.copyOf(wrecks);
            rivals = rivals == null ? List.of() : List.copyOf(rivals);
        }
    }

    static final float NOTABLE_LENGTH_SHARE = 1.1f;
    static final float SMALL_LENGTH_SHARE = 1.2f;
    static final float FLAGSHIP_ENGINE_LEVEL = 0.4f;
    static final float PARKED_ENGINE_LEVEL = 0.05f;
    static final float FORMATION_JITTER_SHARE = 0.04f;
    static final float PULSE_REACH_SHARE = 1.5f;
    static final float COMMAND_SWAY_DEG = 12f;
    static final float SMALL_SWAY_DEG = 14f;
    static final float GUARD_LENGTH_SHARE = 0.9f;
    static final float GUARD_TRACK_SHARE = 0.5f;
    static final float GUARD_SWAY_DEG = 6f;
    static final float HEAVY_REACH_SHARE = 1.7f;
    static final float STANDOFF_LENGTH_SHARE = 0.55f;
    static final float STANDOFF_DISTANCE_SHARE = 0.55f;
    static final float STANDOFF_FORMATION_SHARE = 0.55f;
    static final float STANDOFF_SWAY_DEG = 5f;
    static final float STANDOFF_MEET_PROGRESS = 0.55f;
    static final float TROPHY_LENGTH_SHARE = 0.95f;
    static final float TROPHY_PARKED_LATERAL_SHARE = 0.28f;
    static final float TROPHY_WRECK_LATERAL_SHARE = 0.45f;
    static final float SCRAPPER_LENGTH_SHARE = 1f;
    static final float SCRAPPER_HOME_SHARE = 0.8f;
    static final float SCRAPPER_FIELD_SHARE = 0.75f;
    static final float SCRAPPER_SWAY_DEG = 10f;
    static final float SPOILS_LENGTH_SHARE = 1.05f;
    static final float SPOILS_SWAY_DEG = 8f;
    static final float SPOILS_WRECK_ARC_START_DEG = 110f;
    static final float SPOILS_WRECK_ARC_DEG = 140f;
    static final float SPOILS_WRECK_JITTER_DEG = 12f;
    static final float SPOILS_MIN_WRECK_REACH_SHARE = 0.78f;
    static final float SPOILS_MAX_WRECK_REACH_SHARE = 0.95f;
    static final float SPOILS_FIELD_SHARE = 0.6f;
    static final int SPOILS_DEBRIS_PIECES = 28;
    static final float SPOILS_PIECE_SCALE = 0.85f;
    static final float TOW_LENGTH_SHARE = 1f;
    static final float TOW_LEAD_SHARE = 0.35f;
    static final float TOW_SWAY_DEG = 16f;
    static final float TOW_STERN_SHARE = 0.45f;
    static final float TOW_GAP_SHARE = 0.2f;
    static final float TOW_LAG_SECONDS = 1.6f;
    static final float SALVAGE_LENGTH_SHARE = 1.15f;
    static final float SALVAGE_SWAY_DEG = 10f;
    static final float SALVAGE_FIELD_SHARE = 0.6f;
    static final float SALVAGE_PATCH_FORWARD_SHARE = 0.3f;
    static final float SALVAGE_PATCH_LATERAL_SHARE = 0.7f;
    static final float SALVAGE_PATCH_SHARE = 0.4f;
    static final int SALVAGE_DEBRIS_PIECES = 12;
    static final float SALVAGE_PIECE_SCALE = 0.45f;
    private static final float TROPHY_MIN_TUMBLE = 3f;
    private static final float TROPHY_MAX_TUMBLE = 5f;
    private static final float SPOILS_MIN_TUMBLE = 3f;
    private static final float SPOILS_MAX_TUMBLE = 9f;
    private static final float TOW_MIN_TUMBLE = 4f;
    private static final float TOW_MAX_TUMBLE = 7f;
    private static final float TOW_LINE_WIDTH = 1.2f;
    private static final float TOW_LINE_ALPHA = 0.4f;
    private static final String CLASH_GLOW_PATH = "graphics/fx/hit_glow.png";
    private static final String CLASH_CORE_PATH = "graphics/fx/starburst_glow1.png";
    private static final float CLASH_SIZE_SHARE = 0.6f;
    private static final float CLASH_CORE_SHARE = 0.45f;
    private static final float CLASH_ALPHA = 0.55f;
    private static final float CLASH_SPIN_DEGREES_PER_SECOND = 30f;
    private static final Color DEFAULT_RIVAL_COLOUR = new Color(255, 90, 80);
    private static final DrawnWreck.Embers CUTTING_SPARKS = new DrawnWreck.Embers(4, new Color(255, 236, 190), 2.2f, 8f, 0.1f);
    private static final DrawnWreck.Embers FRESH_EMBERS = new DrawnWreck.Embers(3, new Color(255, 140, 60), 0.35f, 2f, 0.18f);

    private final SpriteCache spriteCache = new SpriteCache(AtlasFlagshipScene.class);
    private final DrawnShips drawnShips = new DrawnShips(spriteCache);
    private final Scene scene;
    private final Cast cast;
    private final float radius;
    private final long seed;
    private final Color colour;
    private final Color rivalColour;
    private final List<FlagshipGroup> groups = new ArrayList<>();
    private final List<PlacedWreck> wrecks = new ArrayList<>();
    private AtlasDebrisField debris;
    private float debrisX;
    private float debrisY;
    private DrawnWreck towed;
    private TowLine tow;
    private float clashDistance = -1f;
    private int shipCount;
    private boolean built;
    private Random random;
    private float baseHeadingDeg;
    private float seconds;

    private record PlacedWreck(DrawnWreck wreck, float x, float y) {
    }

    private record Look(float lengthShare, float engineLevel, float jitter, Color colour) {
    }

    public AtlasFlagshipScene(Scene scene, Cast cast, float radius, long seed, Color colour, Color rivalColour) {
        this.scene = scene == null ? Scene.COMMAND : scene;
        this.cast = cast == null ? new Cast(null, null, null) : cast;
        this.radius = radius;
        this.seed = seed;
        this.colour = colour == null ? Color.WHITE : colour;
        this.rivalColour = rivalColour == null ? DEFAULT_RIVAL_COLOUR : rivalColour;
    }

    @Override
    public void advance(float amount, float headingDeg) {
        seconds += amount;
        for (FlagshipGroup group : groups) {
            group.advance(amount, headingDeg, seconds);
        }
    }

    @Override
    public void render(float screenX, float screenY, float zoom, float headingDeg, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        build(headingDeg);
        SceneFrame frame = new SceneFrame(screenX, screenY, zoom, headingDeg, seconds, alpha);
        if (debris != null) {
            debris.render(frame.screenXOf(debrisX), frame.screenYOf(debrisY), zoom, seconds, alpha);
        }
        for (FlagshipGroup group : groups) {
            group.renderGlow(frame);
        }
        renderTow(frame);
        for (PlacedWreck placed : wrecks) {
            placed.wreck().renderHull(placed.x(), placed.y(), frame);
        }
        drawnShips.begin(shipCount);
        for (FlagshipGroup group : groups) {
            group.addShips(drawnShips, frame);
        }
        if (!drawnShips.isEmpty()) {
            drawnShips.renderFlames(zoom);
            drawnShips.renderGlows();
        }
        for (PlacedWreck placed : wrecks) {
            placed.wreck().renderEmbers(placed.x(), placed.y(), frame);
        }
        for (FlagshipGroup group : groups) {
            group.renderPulse(frame);
        }
        renderClash(frame);
    }

    private void renderTow(SceneFrame frame) {
        if (towed == null || groups.isEmpty()) {
            return;
        }
        FlagshipGroup tug = groups.get(0);
        GroupPose pose = tug.pose();
        float wreckX = tow.xAt(pose, frame.baseHeadingDeg(), frame.seconds());
        float wreckY = tow.yAt(pose, frame.baseHeadingDeg(), frame.seconds());
        double heading = Math.toRadians(pose.headingAt(frame.baseHeadingDeg(), frame.seconds()));
        float sternReach = tug.flagshipLength() * TOW_STERN_SHARE;
        float sternX = pose.x() - sternReach * (float) Math.cos(heading);
        float sternY = pose.y() - sternReach * (float) Math.sin(heading);
        try (GlScope scope = GlScope.flat()) {
            HoloShapes.line(frame.screenXOf(sternX), frame.screenYOf(sternY), frame.screenXOf(wreckX), frame.screenYOf(wreckY), TOW_LINE_WIDTH, colour,
                    frame.alpha() * TOW_LINE_ALPHA);
        }
        towed.renderHull(wreckX, wreckY, frame);
    }

    private void renderClash(SceneFrame frame) {
        if (clashDistance < 0f || groups.isEmpty() || groups.get(0).pulse() == null) {
            return;
        }
        float clash = groups.get(0).pulse().clashAt(frame.seconds(), clashDistance);
        if (clash <= 0f) {
            return;
        }
        float size = radius * CLASH_SIZE_SHARE * (0.6f + 0.4f * clash) * frame.zoom();
        float glowAlpha = frame.alpha() * clash * CLASH_ALPHA;
        SpriteDraw.drawAdditiveAtCenter(spriteCache, CLASH_GLOW_PATH, frame.screenX(), frame.screenY(), size, size, colour, glowAlpha);
        SpriteDraw.drawAdditiveAtCenter(spriteCache, CLASH_GLOW_PATH, frame.screenX(), frame.screenY(), size, size, rivalColour, glowAlpha);
        float core = size * CLASH_CORE_SHARE;
        SpriteDraw.drawAdditiveAtCenter(spriteCache, CLASH_CORE_PATH, frame.screenX(), frame.screenY(), core, core, Color.WHITE, frame.alpha() * clash,
                frame.seconds() * CLASH_SPIN_DEGREES_PER_SECOND);
    }

    private void build(float headingDeg) {
        if (built) {
            return;
        }
        built = true;
        random = new Random(seed);
        baseHeadingDeg = headingDeg;
        List<ShipHullSpecAPI> hulls = hullSpecs(cast.hulls());
        if (hulls.isEmpty()) {
            return;
        }
        switch (scene) {
            case COMMAND -> addGroup(hulls, cruising(NOTABLE_LENGTH_SHARE, colour), GroupPose.centred(COMMAND_SWAY_DEG, random),
                    FlagshipFormation.slots(hulls.size() - 1, radius), steadyPulse());
            case ESCORT -> addGroup(hulls, cruising(SMALL_LENGTH_SHARE, colour), GroupPose.centred(SMALL_SWAY_DEG, random),
                    FlagshipFormation.slots(hulls.size() - 1, radius), null);
            case GUARD_RING -> buildGuardRing(hulls);
            case STANDOFF -> buildStandoff(hulls);
            case TROPHY -> buildTrophy(hulls);
            case SCRAPPERS -> buildScrappers(hulls);
            case SPOILS -> buildSpoils(hulls);
            case TOW -> buildTow(hulls);
            case SALVAGE -> buildSalvage(hulls);
        }
    }

    private void buildGuardRing(List<ShipHullSpecAPI> hulls) {
        List<EscortPath> orbits = new ArrayList<>();
        for (int index = 1; index < hulls.size(); index++) {
            orbits.add(new GuardOrbitPath(ErraticOrbit.around(radius * GUARD_TRACK_SHARE, random, index % 2 == 0)));
        }
        addGroup(hulls, new Look(GUARD_LENGTH_SHARE, FLAGSHIP_ENGINE_LEVEL, 0f, colour), GroupPose.centred(GUARD_SWAY_DEG, random), orbits,
                CommandPulse.heavy(radius * HEAVY_REACH_SHARE, random));
    }

    private void buildStandoff(List<ShipHullSpecAPI> hulls) {
        float distance = radius * STANDOFF_DISTANCE_SHARE;
        float formationRadius = radius * STANDOFF_FORMATION_SHARE;
        CommandPulse shared = CommandPulse.steady(distance / STANDOFF_MEET_PROGRESS, random);
        addGroup(hulls, cruising(STANDOFF_LENGTH_SHARE, colour),
                GroupPose.at(new FlagshipFormation.Slot(-distance, 0f), baseHeadingDeg, 0f, STANDOFF_SWAY_DEG, random),
                FlagshipFormation.slots(hulls.size() - 1, formationRadius), shared);
        List<ShipHullSpecAPI> rivals = hullSpecs(cast.rivals());
        if (rivals.isEmpty()) {
            return;
        }
        addGroup(rivals, cruising(STANDOFF_LENGTH_SHARE, rivalColour),
                GroupPose.at(new FlagshipFormation.Slot(distance, 0f), baseHeadingDeg, 180f, STANDOFF_SWAY_DEG, random),
                FlagshipFormation.slots(rivals.size() - 1, formationRadius), shared);
        clashDistance = distance;
    }

    private void buildTrophy(List<ShipHullSpecAPI> hulls) {
        FlagshipGroup stripper = addGroup(hulls, new Look(TROPHY_LENGTH_SHARE, PARKED_ENGINE_LEVEL, radius * FORMATION_JITTER_SHARE, colour),
                GroupPose.at(new FlagshipFormation.Slot(0f, radius * TROPHY_PARKED_LATERAL_SHARE), baseHeadingDeg, 0f, 0f, random),
                FlagshipFormation.slots(hulls.size() - 1, radius), steadyPulse());
        List<ShipHullSpecAPI> wreckHulls = hullSpecs(cast.wrecks());
        if (stripper != null && !wreckHulls.isEmpty()) {
            placeWreck(wreckHulls.get(0), stripper.shipScale(), new FlagshipFormation.Slot(0f, -radius * TROPHY_WRECK_LATERAL_SHARE),
                    tumble(TROPHY_MIN_TUMBLE, TROPHY_MAX_TUMBLE), CUTTING_SPARKS);
        }
    }

    private void buildScrappers(List<ShipHullSpecAPI> hulls) {
        float fieldRadius = radius * SCRAPPER_FIELD_SHARE;
        List<EscortPath> runs = new ArrayList<>();
        for (FlagshipFormation.Slot home : FlagshipFormation.slots(hulls.size() - 1, radius * SCRAPPER_HOME_SHARE)) {
            runs.add(SalvageRun.from(home, fieldRadius, random));
        }
        addGroup(hulls, new Look(SCRAPPER_LENGTH_SHARE, FLAGSHIP_ENGINE_LEVEL, 0f, colour), GroupPose.centred(SCRAPPER_SWAY_DEG, random), runs,
                steadyPulse());
        debris = new AtlasDebrisField(spriteCache, fieldRadius, seed);
    }

    private void buildSpoils(List<ShipHullSpecAPI> hulls) {
        FlagshipGroup victor = addGroup(hulls, cruising(SPOILS_LENGTH_SHARE, colour), GroupPose.centred(SPOILS_SWAY_DEG, random),
                FlagshipFormation.slots(hulls.size() - 1, radius), steadyPulse());
        List<ShipHullSpecAPI> wreckHulls = hullSpecs(cast.wrecks());
        if (victor != null) {
            int count = wreckHulls.size();
            for (int index = 0; index < count; index++) {
                float arcShare = count == 1 ? 0.5f : index / (float) (count - 1);
                float jitter = (random.nextFloat() * 2f - 1f) * SPOILS_WRECK_JITTER_DEG;
                double angle = Math.toRadians(SPOILS_WRECK_ARC_START_DEG + SPOILS_WRECK_ARC_DEG * arcShare + jitter);
                float reach = radius * between(SPOILS_MIN_WRECK_REACH_SHARE, SPOILS_MAX_WRECK_REACH_SHARE);
                placeWreck(wreckHulls.get(index), victor.shipScale(),
                        new FlagshipFormation.Slot(reach * (float) Math.cos(angle), reach * (float) Math.sin(angle)),
                        tumble(SPOILS_MIN_TUMBLE, SPOILS_MAX_TUMBLE), FRESH_EMBERS);
            }
        }
        debris = new AtlasDebrisField(spriteCache, radius * SPOILS_FIELD_SHARE, seed, SPOILS_DEBRIS_PIECES, SPOILS_PIECE_SCALE);
    }

    private void buildTow(List<ShipHullSpecAPI> hulls) {
        FlagshipGroup tug = addGroup(hulls, cruising(TOW_LENGTH_SHARE, colour),
                GroupPose.at(new FlagshipFormation.Slot(radius * TOW_LEAD_SHARE, 0f), baseHeadingDeg, 0f, TOW_SWAY_DEG, random),
                FlagshipFormation.slots(hulls.size() - 1, radius), null);
        List<ShipHullSpecAPI> wreckHulls = hullSpecs(cast.wrecks());
        if (tug == null || wreckHulls.isEmpty()) {
            return;
        }
        towed = DrawnWreck.of(wreckHulls.get(0), spriteCache, tug.shipScale(), tumble(TOW_MIN_TUMBLE, TOW_MAX_TUMBLE), DrawnWreck.Embers.NONE, random);
        if (towed != null) {
            tow = new TowLine(tug.flagshipLength() * TOW_STERN_SHARE + towed.halfLength() + radius * TOW_GAP_SHARE, TOW_LAG_SECONDS);
        }
    }

    private void buildSalvage(List<ShipHullSpecAPI> hulls) {
        float fieldRadius = radius * SALVAGE_FIELD_SHARE;
        List<EscortPath> runs = new ArrayList<>();
        for (FlagshipFormation.Slot home : FlagshipFormation.slots(hulls.size() - 1, radius)) {
            runs.add(SalvageRun.from(home, fieldRadius, random));
        }
        addGroup(hulls, new Look(SALVAGE_LENGTH_SHARE, FLAGSHIP_ENGINE_LEVEL, 0f, colour), GroupPose.centred(SALVAGE_SWAY_DEG, random), runs, null);
        FlagshipFormation.Slot patch = new FlagshipFormation.Slot(radius * SALVAGE_PATCH_FORWARD_SHARE, radius * SALVAGE_PATCH_LATERAL_SHARE);
        debrisX = patch.xAt(0f, baseHeadingDeg);
        debrisY = patch.yAt(0f, baseHeadingDeg);
        debris = new AtlasDebrisField(spriteCache, radius * SALVAGE_PATCH_SHARE, seed, SALVAGE_DEBRIS_PIECES, SALVAGE_PIECE_SCALE);
    }

    private Look cruising(float lengthShare, Color groupColour) {
        return new Look(lengthShare, FLAGSHIP_ENGINE_LEVEL, radius * FORMATION_JITTER_SHARE, groupColour);
    }

    private CommandPulse steadyPulse() {
        return CommandPulse.steady(radius * PULSE_REACH_SHARE, random);
    }

    private DrawnWreck.Tumble tumble(float slowest, float fastest) {
        float direction = random.nextBoolean() ? 1f : -1f;
        return new DrawnWreck.Tumble(random.nextFloat() * 360f, direction * between(slowest, fastest));
    }

    private float between(float low, float high) {
        return low + (high - low) * random.nextFloat();
    }

    private FlagshipGroup addGroup(List<ShipHullSpecAPI> hulls, Look look, GroupPose pose, List<? extends EscortPath> paths, CommandPulse pulse) {
        float startHeadingDeg = pose.headingAt(baseHeadingDeg, 0f);
        RoamingShip lead = ship(hulls.get(0), 0f, startHeadingDeg);
        if (lead == null) {
            return null;
        }
        float scale = radius * look.lengthShare() / Math.max(1f, Math.max(lead.width(), lead.height()));
        List<FlagshipGroup.Escort> escorts = new ArrayList<>();
        for (int index = 1; index < hulls.size() && index <= paths.size(); index++) {
            EscortPath path = paths.get(index - 1);
            RoamingShip escort = ship(hulls.get(index), look.jitter(), path.facingAt(0f, startHeadingDeg));
            if (escort != null) {
                escorts.add(new FlagshipGroup.Escort(escort, path));
            }
        }
        FlagshipGroup group = new FlagshipGroup(spriteCache, new FlagshipGroup.Flagship(lead, scale, look.engineLevel()), pose, escorts, pulse,
                look.colour());
        groups.add(group);
        shipCount += group.shipCount();
        return group;
    }

    private void placeWreck(ShipHullSpecAPI hull, float scale, FlagshipFormation.Slot spot, DrawnWreck.Tumble wreckTumble, DrawnWreck.Embers embers) {
        DrawnWreck wreck = DrawnWreck.of(hull, spriteCache, scale, wreckTumble, embers, random);
        if (wreck != null) {
            wrecks.add(new PlacedWreck(wreck, spot.xAt(0f, baseHeadingDeg), spot.yAt(0f, baseHeadingDeg)));
        }
    }

    private RoamingShip ship(ShipHullSpecAPI hull, float jitter, float facingDeg) {
        String spritePath = hull.getSpriteName();
        SpriteAPI texture = spritePath == null ? null : spriteCache.texture(spritePath);
        if (texture == null) {
            return null;
        }
        FleetShipDrift drift = new FleetShipDrift(FleetShipDrift.HullMotion.of(hull.getHullSize()), jitter, facingDeg, random);
        return new RoamingShip(spritePath, texture.getWidth(), texture.getHeight(), hull.getHullSize(), drift, HullEngines.of(hull));
    }

    private static List<ShipHullSpecAPI> hullSpecs(List<String> hullIds) {
        List<ShipHullSpecAPI> hulls = new ArrayList<>();
        for (String hullId : hullIds) {
            ShipHullSpecAPI hull = AtlasShipGroup.hullSpec(hullId);
            if (hull != null) {
                hulls.add(hull);
            }
        }
        return hulls;
    }
}
