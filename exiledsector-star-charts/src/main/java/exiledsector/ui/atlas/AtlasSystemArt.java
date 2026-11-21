package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.anomaly.StarAnomalies;
import exiledsector.charts.anomaly.breach.BreachCorePlugin;
import exiledsector.charts.layout.sketch.ChartSystemSketch;
import exiledsector.ui.belt.AuroraBeltRenderer;
import exiledsector.ui.belt.BandArc;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class AtlasSystemArt {

    enum Kind {
        NEBULA_PUFF, ACCRETION, WRECK, PULSAR, RESEARCH_STATION, AURORA, FLARE, BREACH, BATTLE, RELAY, CONVOY, DEBRIS,
        EXCAVATION, STORM, FORGE, LISTENING_POST
    }

    record Piece(Kind kind, int planet, float orbit, float angle, float size, float speed, int variant) {
    }

    record Layout(String nebulaSheet, List<Piece> pieces) {

        static final Layout EMPTY = new Layout(null, List.of());

        Layout {
            pieces = List.copyOf(pieces);
        }
    }

    static final class Frame {
        final OrbitalPlaneCamera camera;
        float radius;
        float reach;
        float starX;
        float starY;
        float starPixels;
        float starPlaneX;
        float starPlaneY;
        float starPlanePixels;
        Color starColour = Color.WHITE;
        float elapsedSeconds;
        boolean stillOrbits;
        float alpha;
        float[] planetX = new float[0];
        float[] planetY = new float[0];
        float[] planetPixels = new float[0];
        float[] planetPlaneX = new float[0];
        float[] planetPlaneY = new float[0];
        float[] planetPlanePixels = new float[0];

        Frame(OrbitalPlaneCamera camera) {
            this.camera = camera;
        }

        void ensurePlanets(int count) {
            if (planetX.length < count) {
                planetX = new float[count];
                planetY = new float[count];
                planetPixels = new float[count];
                planetPlaneX = new float[count];
                planetPlaneY = new float[count];
                planetPlanePixels = new float[count];
            }
        }
    }

    static final float MIN_ART_RADIUS = 40f;
    static final String NEBULA_SHEET = "graphics/terrain/nebula.png";
    static final String NEBULA_BLUE_SHEET = "graphics/terrain/nebula512_blue.png";
    static final String NEBULA_AMBER_SHEET = "graphics/terrain/nebula_amber.png";
    static final String DEBRIS_SHEET = "graphics/debris/debris_sheet0.png";
    static final String DEBRIS_GLOW_SHEET = "graphics/debris/debris_sheet0_glow.png";
    static final String CLOUD_SHEET = "graphics/fx/cleaner_clouds00.png";
    static final String GLOW = "graphics/fx/starburst_glow1.png";
    static final String AURORA_TEXTURE = "graphics/planets/aurorae.png";
    static final String RESEARCH_STATION_SPRITE = "graphics/stations/station_side00.png";
    static final String FORGE_SPRITE = "graphics/stations/station_mining00.png";
    static final String POST_SPRITE = "graphics/stations/station_side07.png";
    static final String CONVOY_SPRITE = "graphics/ships/buffalo/buffalo_base.png";
    static final String ATTACKER_SPRITE = "graphics/ships/lasher/lasher_base.png";
    static final String DEFENDER_SPRITE = "graphics/ships/wolf/wolf_base.png";
    static final List<String> RELAY_SPRITES = List.of("graphics/ships/nav_buoy.png", "graphics/ships/com_relay.png", "graphics/ships/sensor_array.png");
    static final List<Float> RELAY_ASPECTS = List.of(1f, 177f / 96f, 1f);
    static final float CONVOY_ASPECT = 100f / 150f;
    static final float ATTACKER_ASPECT = 68f / 96f;
    static final float DEFENDER_ASPECT = 52f / 96f;
    static final int NEBULA_PUFFS = 10;
    static final int DENSE_NEBULA_PUFFS = 18;
    static final float NEBULA_ALPHA = 0.6f;
    static final int DEBRIS_PIECES = 5;
    static final int SHEET_SIDE = 4;
    static final int CLOUD_SHEET_SIDE = 2;
    static final int SHEET_CELLS = 16;
    static final float FLARE_SECONDS = 2.4f;
    static final int FLARES = 3;
    static final float AURORA_INNER_SHARE = 1.1f;
    static final float AURORA_OUTER_SHARE = 1.7f;
    static final int CONVOY_SHIPS = 3;
    static final float CONVOY_SPACING_DEGREES = 7f;
    static final float STORM_ARC_DEGREES = 80f;
    static final int STORM_PUFFS = 7;
    static final float NEBULA_ORBIT_DEGREES_PER_SECOND = 1f;
    static final float STORM_FLASH_SECONDS = 1.3f;
    static final float BATTLE_SHOT_SECONDS = 0.45f;
    static final int BATTLE_SHIPS = 3;
    static final float SCAN_SECONDS = 2.5f;
    static final float RADAR_DEGREES_PER_SECOND = 90f;
    static final float RADAR_WIDTH_DEGREES = 35f;
    static final float PULSAR_WIDTH_DEGREES = 4f;
    static final float RELAY_DASH_LENGTH = 8f;
    static final Color ACCRETION_COLOUR = new Color(255, 150, 60);
    static final Color AURORA_COLOUR = new Color(110, 255, 200);
    static final Color AURORA_SHIFT_COLOUR = new Color(180, 120, 255);
    static final Color PULSAR_COLOUR = new Color(170, 210, 255);
    static final Color WRECK_TINT = new Color(170, 165, 160);
    static final Color FORGE_GLOW = new Color(255, 140, 40);
    static final Color RADAR_COLOUR = new Color(120, 255, 140);
    static final Color RELAY_LINK_COLOUR = new Color(110, 200, 255);
    static final Color SHOT_COLOUR = new Color(255, 210, 120);
    static final Color SCAN_COLOUR = new Color(255, 190, 90);
    static final Color STORM_TINT = new Color(110, 125, 185);
    static final Color LIGHTNING_COLOUR = new Color(190, 210, 255);
    static final float ACCRETION_INNER_SHARE = 1.25f;
    static final float ACCRETION_OUTER_SHARE = 2.4f;
    static final float SCAN_START_SHARE = 1.1f;
    static final float SCAN_GROWTH_SHARE = 1.3f;
    static final int RELAY_SPRITE_PART = 0;
    static final int RELAY_LINK_PART = 1;
    private static final UnitCircle RING_CIRCLE = UnitCircle.of(40);
    private static final BandArc[] WHOLE = {BandArc.WHOLE};
    private static final BandArc[] HALVES = BandArc.split(2);

    private final SpriteCache spriteCache = new SpriteCache(AtlasSystemArt.class);
    private final Vector2f breachCentre = new Vector2f();
    private SpriteAPI breachRing;
    private final Vector2f bandCentre = new Vector2f();
    private final float[] relayPlaneX = new float[RELAY_SPRITES.size()];
    private final float[] relayPlaneY = new float[RELAY_SPRITES.size()];
    private final int[] relayPieces = new int[RELAY_SPRITES.size()];
    private int relayCount;
    private float pieceX;
    private float pieceY;
    private float piecePlaneX;
    private float piecePlaneY;
    private float pieceScale = 1f;

    static Layout layout(long seed, ChartSystemSketch sketch, List<String> anomalyIds) {
        List<Piece> pieces = new ArrayList<>();
        String nebulaSheet = nebulaSheet(sketch);
        List<Integer> orbiting = new ArrayList<>();
        for (int index = 0; index < sketch.planets().size(); index++) {
            if (sketch.planets().get(index).orbitRadius() > 0f) {
                orbiting.add(index);
            }
        }
        if (nebulaSheet != null) {
            addNebula(new Random(seed), anomalyIds.contains("dense_nebula") ? DENSE_NEBULA_PUFFS : NEBULA_PUFFS, pieces);
        }
        for (String anomalyId : anomalyIds) {
            addAnomaly(anomalyId, new Random(seed ^ ((long) anomalyId.hashCode() << 32)), orbiting, hasMagneticStar(sketch), pieces);
        }
        return new Layout(nebulaSheet, pieces);
    }

    static String nebulaSheet(ChartSystemSketch sketch) {
        for (ChartSystemSketch.Body star : sketch.stars()) {
            String typeId = star.typeId();
            if (typeId != null && typeId.startsWith("nebula_center")) {
                return typeId.contains("young") ? NEBULA_BLUE_SHEET : typeId.contains("old") ? NEBULA_AMBER_SHEET : NEBULA_SHEET;
            }
        }
        return null;
    }

    private static void addNebula(Random random, int puffs, List<Piece> pieces) {
        for (int index = 0; index < puffs; index++) {
            float spin = (1f + random.nextFloat() * 3f) * (random.nextBoolean() ? 1f : -1f);
            pieces.add(new Piece(Kind.NEBULA_PUFF, -1, (float) Math.sqrt(random.nextFloat()) * 0.95f, random.nextFloat() * 360f,
                    0.16f + random.nextFloat() * 0.18f, spin, random.nextInt(SHEET_CELLS)));
        }
    }

    private static int pickPlanet(Random random, List<Integer> orbiting) {
        return orbiting.isEmpty() ? -1 : orbiting.get(random.nextInt(orbiting.size()));
    }

    static boolean hasMagneticStar(ChartSystemSketch sketch) {
        if (sketch.systemType() == StarSystemType.NEBULA || sketch.stars().isEmpty()) {
            return false;
        }
        return !StarAnomalies.BLACK_HOLE.primaryStar().equals(sketch.stars().get(0).typeId());
    }

    private static void addAnomaly(String anomalyId, Random random, List<Integer> orbiting, boolean magneticStar, List<Piece> pieces) {
        switch (anomalyId) {
            case "black_hole" -> {
                pieces.add(new Piece(Kind.ACCRETION, -1, 0f, 0f, 0f, 0f, 0));
                for (int index = 0; index < 3; index++) {
                    pieces.add(new Piece(Kind.WRECK, -1, 0.25f + random.nextFloat() * 0.35f, random.nextFloat() * 360f, 0.035f,
                            6f + random.nextFloat() * 4f, random.nextInt(SHEET_CELLS)));
                }
            }
            case "neutron_star" -> {
                pieces.add(new Piece(Kind.PULSAR, -1, 0.95f, random.nextFloat() * 360f, 0f, 40f, 0));
                pieces.add(new Piece(Kind.RESEARCH_STATION, -1, 0.3f, random.nextFloat() * 360f, 0.06f, 8f, 0));
            }
            case "magnetic_storm" -> {
                if (magneticStar) {
                    pieces.add(new Piece(Kind.AURORA, -1, 0f, 0f, 0f, 0f, 0));
                }
                int planet = pickPlanet(random, orbiting);
                if (planet >= 0) {
                    pieces.add(new Piece(Kind.AURORA, planet, 0f, 0f, 0f, 0f, 1));
                }
            }
            case "stellar_flares" -> {
                for (int index = 0; index < FLARES; index++) {
                    pieces.add(new Piece(Kind.FLARE, -1, 0f, random.nextFloat() * 360f, 0f, 0f, index));
                }
            }
            case "spatial_breach" -> pieces.add(new Piece(Kind.BREACH, -1, 0.55f + random.nextFloat() * 0.25f, random.nextFloat() * 360f, 0.11f, 2f, 0));
            case "faction_skirmish" -> pieces.add(new Piece(Kind.BATTLE, -1, 0.5f + random.nextFloat() * 0.25f, random.nextFloat() * 360f, 0.035f, 3f,
                    random.nextInt(1000)));
            case "relay_network" -> {
                float start = random.nextFloat() * 360f;
                for (int index = 0; index < RELAY_SPRITES.size(); index++) {
                    pieces.add(new Piece(Kind.RELAY, -1, 0.3f + 0.2f * index, start + 120f * index + random.nextFloat() * 30f, 0.045f, 4f, index));
                }
            }
            case "supply_convoy" -> pieces.add(new Piece(Kind.CONVOY, -1, 0.65f + random.nextFloat() * 0.2f, random.nextFloat() * 360f, 0.04f, 5f, 0));
            case "anomalous_debris" -> {
                int clusters = 2 + random.nextInt(2);
                for (int index = 0; index < clusters; index++) {
                    pieces.add(new Piece(Kind.DEBRIS, -1, 0.4f + random.nextFloat() * 0.45f, random.nextFloat() * 360f, 0.03f,
                            2f + random.nextFloat() * 3f, random.nextInt(1000)));
                }
            }
            case "excavation_sites" -> {
                List<Integer> free = new ArrayList<>(orbiting);
                for (int index = 0; index < 2 && !free.isEmpty(); index++) {
                    pieces.add(new Piece(Kind.EXCAVATION, free.remove(random.nextInt(free.size())), 0f, random.nextFloat() * 360f, 0f, 0f, index));
                }
            }
            case "storm_front" -> pieces.add(new Piece(Kind.STORM, -1, 0.6f + random.nextFloat() * 0.15f, random.nextFloat() * 360f, 0.22f, 3f,
                    random.nextInt(1000)));
            case "derelict_forge" -> pieces.add(new Piece(Kind.FORGE, -1, 0.35f, random.nextFloat() * 360f, 0.07f, 7f, 0));
            case "listening_post" -> pieces.add(new Piece(Kind.LISTENING_POST, -1, 0.6f, random.nextFloat() * 360f, 0.065f, 4f, 0));
            default -> {
            }
        }
    }

    void renderNebula(Layout layout, Frame frame) {
        if (layout.nebulaSheet() == null) {
            return;
        }
        SpriteAPI sheet = spriteCache.sprite(layout.nebulaSheet());
        if (sheet == null) {
            return;
        }
        for (Piece piece : layout.pieces()) {
            if (piece.kind() == Kind.NEBULA_PUFF) {
                aroundStar(piece, frame, NEBULA_ORBIT_DEGREES_PER_SECOND);
                float size = piece.size() * frame.radius * 2f * pieceScale;
                drawCell(sheet, piece.variant(), pieceX, pieceY, size, piece.angle() + frame.elapsedSeconds * piece.speed(), Color.WHITE,
                        NEBULA_ALPHA * frame.alpha, false);
            }
        }
    }

    void queueAnomalies(Layout layout, Frame frame, DepthOrder<AtlasSystemPreview.Layer> order) {
        relayCount = 0;
        if (frame.radius < MIN_ART_RADIUS) {
            return;
        }
        List<Piece> pieces = layout.pieces();
        for (int index = 0; index < pieces.size(); index++) {
            queuePiece(pieces.get(index), index, frame, order);
        }
        queueRelayLinks(frame, order);
    }

    private void queuePiece(Piece piece, int index, Frame frame, DepthOrder<AtlasSystemPreview.Layer> order) {
        switch (piece.kind()) {
            case NEBULA_PUFF -> {
            }
            case ACCRETION -> queueAround(order, frame, index, frame.starPlaneY, frame.starPlanePixels * (ACCRETION_INNER_SHARE + ACCRETION_OUTER_SHARE) / 2f);
            case PULSAR -> {
                for (int side = 0; side < 2; side++) {
                    double beam = Math.toRadians(pulsarSweep(piece, frame) + side * 180f);
                    queue(order, frame, index, side, frame.starPlaneY + (float) Math.sin(beam) * pulsarLength(piece, frame) / 2f);
                }
            }
            case AURORA -> queueAround(order, frame, index, onPlanet(piece, frame) ? frame.planetPlaneY[piece.planet()] : frame.starPlaneY,
                    auroraBodyPlanePixels(piece, frame) * (AURORA_INNER_SHARE + AURORA_OUTER_SHARE) / 2f);
            case FLARE -> queue(order, frame, index, 0, frame.starPlaneY + (float) Math.sin(flareAngle(piece, frame)) * frame.starPlanePixels);
            case EXCAVATION -> {
                if (onPlanet(piece, frame)) {
                    queueAround(order, frame, index, frame.planetPlaneY[piece.planet()],
                            frame.planetPlanePixels[piece.planet()] * (SCAN_START_SHARE + SCAN_GROWTH_SHARE / 2f));
                }
            }
            case CONVOY -> {
                for (int ship = 0; ship < CONVOY_SHIPS; ship++) {
                    aroundStar(convoyShipAngle(piece, frame, ship), piece.orbit() * frame.reach, frame);
                    queue(order, frame, index, ship, piecePlaneY);
                }
            }
            case STORM -> {
                for (int puff = 0; puff < STORM_PUFFS; puff++) {
                    placeStormPuff(piece, frame, puff);
                    queue(order, frame, index, puff, piecePlaneY);
                }
            }
            case RELAY -> {
                aroundStar(piece, frame, piece.speed());
                if (relayCount < relayPlaneX.length) {
                    relayPlaneX[relayCount] = piecePlaneX;
                    relayPlaneY[relayCount] = piecePlaneY;
                    relayPieces[relayCount] = index;
                    relayCount++;
                }
                queue(order, frame, index, RELAY_SPRITE_PART, piecePlaneY);
            }
            default -> {
                aroundStar(piece, frame, piece.speed());
                queue(order, frame, index, 0, piecePlaneY);
            }
        }
    }

    private void queueRelayLinks(Frame frame, DepthOrder<AtlasSystemPreview.Layer> order) {
        if (relayCount < 2) {
            return;
        }
        int links = relayCount == 2 ? 1 : relayCount;
        for (int link = 0; link < links; link++) {
            int next = (link + 1) % relayCount;
            queue(order, frame, relayPieces[link], RELAY_LINK_PART + link, (relayPlaneY[link] + relayPlaneY[next]) / 2f);
        }
    }

    private static void queue(DepthOrder<AtlasSystemPreview.Layer> order, Frame frame, int pieceIndex, int part, float planeY) {
        AtlasSystemPreview.Layer layer = AtlasSystemPreview.Layer.ANOMALY;
        order.add(layer, pieceIndex, part, frame.camera.tilted() ? planeY : layer.untiltedDepth);
    }

    private static void queueAround(DepthOrder<AtlasSystemPreview.Layer> order, Frame frame, int pieceIndex, float centreDepth, float radius) {
        BandArc[] arcs = arcs(frame);
        for (int part = 0; part < arcs.length; part++) {
            queue(order, frame, pieceIndex, part, AtlasSystemPreview.arcDepth(centreDepth, radius, arcs[part]));
        }
    }

    private static BandArc[] arcs(Frame frame) {
        return frame.camera.tilted() ? HALVES : WHOLE;
    }

    void renderPiece(Layout layout, int pieceIndex, int part, Frame frame) {
        Piece piece = layout.pieces().get(pieceIndex);
        switch (piece.kind()) {
            case ACCRETION -> drawAccretion(frame, arcs(frame)[part]);
            case WRECK -> drawWreck(piece, frame);
            case PULSAR -> drawPulsarBeam(piece, frame, part);
            case RESEARCH_STATION -> drawOrbitingSprite(piece, frame, RESEARCH_STATION_SPRITE, 1f, null);
            case AURORA -> drawAurora(piece, frame, arcs(frame)[part]);
            case FLARE -> drawFlare(piece, frame);
            case BREACH -> drawBreach(piece, frame);
            case BATTLE -> drawBattle(piece, frame);
            case RELAY -> drawRelayPart(piece, frame, part);
            case CONVOY -> drawConvoyShip(piece, frame, part);
            case DEBRIS -> drawDebris(piece, frame);
            case EXCAVATION -> drawExcavation(piece, frame, part);
            case STORM -> drawStormPuff(piece, frame, part);
            case FORGE -> drawOrbitingSprite(piece, frame, FORGE_SPRITE, 1f, FORGE_GLOW);
            case LISTENING_POST -> drawListeningPost(piece, frame);
            default -> {
            }
        }
    }

    private static float orbitAngle(Piece piece, Frame frame, float degreesPerSecond) {
        return frame.stillOrbits ? piece.angle() : travelAngle(piece, frame, degreesPerSecond);
    }

    private static float travelAngle(Piece piece, Frame frame, float degreesPerSecond) {
        return piece.angle() + frame.elapsedSeconds * degreesPerSecond;
    }

    private static boolean onPlanet(Piece piece, Frame frame) {
        return piece.planet() >= 0 && piece.planet() < frame.planetX.length;
    }

    private void aroundStar(Piece piece, Frame frame, float degreesPerSecond) {
        aroundStar(orbitAngle(piece, frame, degreesPerSecond), piece.orbit() * frame.reach, frame);
    }

    private void aroundStar(float angleDegrees, float distance, Frame frame) {
        double angle = Math.toRadians(angleDegrees);
        onPlane(frame.starPlaneX + (float) Math.cos(angle) * distance, frame.starPlaneY + (float) Math.sin(angle) * distance, frame);
    }

    private void onPlane(float planeX, float planeY, Frame frame) {
        piecePlaneX = planeX;
        piecePlaneY = planeY;
        pieceX = frame.camera.screenX(planeX, planeY);
        pieceY = frame.camera.screenY(planeY);
        pieceScale = frame.camera.sizeAt(planeY);
    }

    private float orbitHeading(float orbitAngleDegrees, Frame frame) {
        return frame.camera.screenHeading(piecePlaneX, piecePlaneY, orbitAngleDegrees + 90f);
    }

    private static void planeVertex(Frame frame, float planeX, float planeY) {
        GL11.glVertex2f(frame.camera.screenX(planeX, planeY), frame.camera.screenY(planeY));
    }

    private static void drawCell(SpriteAPI sheet, int cell, float x, float y, float size, float angle, Color tint, float alpha, boolean additive) {
        SpriteDraw.drawCellAtCenter(sheet, SHEET_SIDE, SHEET_SIDE, cell, x, y, size, size, angle, tint, alpha, additive);
    }

    private void drawSprite(String path, float x, float y, float height, float aspect, float facingDegrees, Color tint, float alpha) {
        SpriteDraw.drawAtCenter(spriteCache, path, x, y, height * aspect, height, tint, alpha, facingDegrees - 90f);
    }

    private void drawGlow(float x, float y, float size, Color colour, float alpha) {
        SpriteDraw.drawAdditiveAtCenter(spriteCache, GLOW, x, y, size, size, colour, alpha);
    }

    private void drawAccretion(Frame frame, BandArc arc) {
        SpriteAPI texture = spriteCache.texture(AURORA_TEXTURE);
        if (texture == null) {
            return;
        }
        bandCentre.set(frame.starPlaneX, frame.starPlaneY);
        AuroraBeltRenderer.renderArcOnPlane(texture, new RadialBand(bandCentre, frame.starPlanePixels * ACCRETION_INNER_SHARE,
                frame.starPlanePixels * ACCRETION_OUTER_SHARE), ACCRETION_COLOUR, 0.8f * frame.alpha, frame.elapsedSeconds * 3f, 1f, arc, frame.camera);
    }

    private void drawWreck(Piece piece, Frame frame) {
        SpriteAPI sheet = spriteCache.sprite(DEBRIS_SHEET);
        if (sheet != null) {
            aroundStar(piece, frame, piece.speed());
            drawCell(sheet, piece.variant(), pieceX, pieceY, piece.size() * frame.radius * pieceScale, frame.elapsedSeconds * 20f + piece.angle(),
                    WRECK_TINT, frame.alpha, false);
        }
    }

    private static float pulsarSweep(Piece piece, Frame frame) {
        return piece.angle() + frame.elapsedSeconds * piece.speed();
    }

    private static float pulsarLength(Piece piece, Frame frame) {
        return piece.orbit() * frame.reach;
    }

    private static void drawPulsarBeam(Piece piece, Frame frame, int side) {
        double centre = Math.toRadians(pulsarSweep(piece, frame) + side * 180f);
        double half = Math.toRadians(PULSAR_WIDTH_DEGREES);
        float length = pulsarLength(piece, frame);
        try (GlScope beam = GlScope.flat().additive()) {
            GL11.glBegin(GL11.GL_TRIANGLES);
            Misc.setColor(PULSAR_COLOUR, 0.55f * frame.alpha);
            planeVertex(frame, frame.starPlaneX, frame.starPlaneY);
            Misc.setColor(PULSAR_COLOUR, 0f);
            planeVertex(frame, frame.starPlaneX + (float) Math.cos(centre - half) * length, frame.starPlaneY + (float) Math.sin(centre - half) * length);
            planeVertex(frame, frame.starPlaneX + (float) Math.cos(centre + half) * length, frame.starPlaneY + (float) Math.sin(centre + half) * length);
            GL11.glEnd();
        }
    }

    private static float auroraBodyPlanePixels(Piece piece, Frame frame) {
        return onPlanet(piece, frame) ? frame.planetPlanePixels[piece.planet()] : frame.starPlanePixels;
    }

    private void drawAurora(Piece piece, Frame frame, BandArc arc) {
        SpriteAPI texture = spriteCache.texture(AURORA_TEXTURE);
        if (texture == null) {
            return;
        }
        boolean onPlanet = onPlanet(piece, frame);
        float bodyPlanePixels = auroraBodyPlanePixels(piece, frame);
        float shift = Fade.pulse(frame.elapsedSeconds * 0.7f + piece.variant());
        bandCentre.set(onPlanet ? frame.planetPlaneX[piece.planet()] : frame.starPlaneX, onPlanet ? frame.planetPlaneY[piece.planet()] : frame.starPlaneY);
        AuroraBeltRenderer.renderArcOnPlane(texture, new RadialBand(bandCentre, bodyPlanePixels * AURORA_INNER_SHARE, bodyPlanePixels * AURORA_OUTER_SHARE),
                Misc.interpolateColor(AURORA_COLOUR, AURORA_SHIFT_COLOUR, shift), 0.75f * frame.alpha, frame.elapsedSeconds, 1f, arc, frame.camera);
    }

    private static float flareCycle(Piece piece, Frame frame) {
        return frame.elapsedSeconds / FLARE_SECONDS + piece.variant() / (float) FLARES;
    }

    private static double flareAngle(Piece piece, Frame frame) {
        return Math.toRadians(piece.angle() + Math.floor(flareCycle(piece, frame)) * FlareWeather.GOLDEN_ANGLE_DEGREES);
    }

    private void drawFlare(Piece piece, Frame frame) {
        float cycle = flareCycle(piece, frame);
        float progress = cycle - (float) Math.floor(cycle);
        StarFlareGlow.drawRising(spriteCache, frame.starX, frame.starY, frame.starPixels, flareAngle(piece, frame), frame.camera.flattening(), progress,
                StarFlareGlow.MAP_RISE, frame.starColour, frame.alpha);
    }

    private void drawBreach(Piece piece, Frame frame) {
        SpriteAPI halo = spriteCache.sprite(BreachCorePlugin.HALO_TEXTURE);
        if (breachRing == null) {
            breachRing = BreachCorePlugin.loadRing();
        }
        if (halo == null || breachRing == null) {
            return;
        }
        aroundStar(piece, frame, piece.speed());
        breachCentre.set(pieceX, pieceY);
        float haloDiameter = 2f * piece.size() * frame.radius * pieceScale;
        BreachCorePlugin.drawCore(halo, breachRing, breachCentre, haloDiameter / BreachCorePlugin.HALO_SIZE, frame.alpha, frame.elapsedSeconds);
    }

    private void drawOrbitingSprite(Piece piece, Frame frame, String path, float aspect, Color glow) {
        float orbitAngle = orbitAngle(piece, frame, piece.speed());
        aroundStar(orbitAngle, piece.orbit() * frame.reach, frame);
        float size = piece.size() * frame.radius * pieceScale;
        if (glow != null) {
            float pulse = (float) Math.sin(frame.elapsedSeconds * 2f) * 0.25f + 0.75f;
            drawGlow(pieceX, pieceY, size * 2.4f, glow, 0.6f * pulse * frame.alpha);
        }
        drawSprite(path, pieceX, pieceY, size, aspect, orbitHeading(orbitAngle, frame), Color.WHITE, frame.alpha);
    }

    private void drawBattle(Piece piece, Frame frame) {
        float orbitAngle = orbitAngle(piece, frame, piece.speed());
        aroundStar(orbitAngle, piece.orbit() * frame.reach, frame);
        float size = piece.size() * frame.radius;
        double axis = Math.toRadians(orbitAngle + 90f);
        float axisX = (float) Math.cos(axis);
        float axisY = (float) Math.sin(axis);
        float centreX = piecePlaneX;
        float centreY = piecePlaneY;
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? -1f : 1f;
            for (int ship = 0; ship < BATTLE_SHIPS; ship++) {
                float bob = (float) Math.sin(frame.elapsedSeconds * 1.3f + ship + side * 2f) * size * 0.15f;
                float spread = (ship - 1) * size * 1.3f;
                onPlane(centreX + axisX * sign * size * 2.5f - axisY * spread + axisX * bob,
                        centreY + axisY * sign * size * 2.5f + axisX * spread + axisY * bob, frame);
                float heading = orbitHeading(orbitAngle, frame);
                drawSprite(side == 0 ? ATTACKER_SPRITE : DEFENDER_SPRITE, pieceX, pieceY, size * pieceScale, side == 0 ? ATTACKER_ASPECT : DEFENDER_ASPECT,
                        side == 0 ? heading : heading + 180f, Color.WHITE, frame.alpha);
            }
        }
        float shotCycle = frame.elapsedSeconds / BATTLE_SHOT_SECONDS;
        int shot = (int) Math.floor(shotCycle) + piece.variant();
        float fade = 1f - (shotCycle - (float) Math.floor(shotCycle));
        int from = Math.floorMod(shot * 7, BATTLE_SHIPS);
        int to = Math.floorMod(shot * 11 + 1, BATTLE_SHIPS);
        float firing = shot % 2 == 0 ? -1f : 1f;
        onPlane(centreX + axisX * firing * size * 2.5f - axisY * (from - 1) * size * 1.3f,
                centreY + axisY * firing * size * 2.5f + axisX * (from - 1) * size * 1.3f, frame);
        float fromX = pieceX;
        float fromY = pieceY;
        onPlane(centreX - axisX * firing * size * 2.5f - axisY * (to - 1) * size * 1.3f,
                centreY - axisY * firing * size * 2.5f + axisX * (to - 1) * size * 1.3f, frame);
        try (GlScope shotScope = GlScope.flat().additive().lineWidth(1.5f)) {
            GL11.glBegin(GL11.GL_LINES);
            Misc.setColor(SHOT_COLOUR, fade * frame.alpha);
            GL11.glVertex2f(fromX, fromY);
            GL11.glVertex2f(pieceX, pieceY);
            GL11.glEnd();
        }
        drawGlow(pieceX, pieceY, size * 1.4f * pieceScale, SHOT_COLOUR, fade * 0.8f * frame.alpha);
    }

    private void drawRelayPart(Piece piece, Frame frame, int part) {
        if (part == RELAY_SPRITE_PART) {
            aroundStar(piece, frame, piece.speed());
            drawSprite(RELAY_SPRITES.get(piece.variant()), pieceX, pieceY, piece.size() * frame.radius * pieceScale, RELAY_ASPECTS.get(piece.variant()),
                    piece.angle() + frame.elapsedSeconds * 15f, Color.WHITE, frame.alpha);
        } else {
            drawRelayLink(frame, part - RELAY_LINK_PART);
        }
    }

    private void drawRelayLink(Frame frame, int link) {
        if (link < 0 || link >= relayCount) {
            return;
        }
        int next = (link + 1) % relayCount;
        float pulse = (float) Math.sin(frame.elapsedSeconds * 2.5f) * 0.15f + 0.3f;
        try (GlScope links = GlScope.flat().additive().lineWidth(1f)) {
            GL11.glBegin(GL11.GL_LINES);
            Misc.setColor(RELAY_LINK_COLOUR, pulse * frame.alpha);
            drawDashes(frame, relayPlaneX[link], relayPlaneY[link], relayPlaneX[next], relayPlaneY[next]);
            GL11.glEnd();
        }
    }

    private static void drawDashes(Frame frame, float fromX, float fromY, float toX, float toY) {
        float deltaX = toX - fromX;
        float deltaY = toY - fromY;
        float length = (float) Math.hypot(deltaX, deltaY);
        if (length <= 0f) {
            return;
        }
        float stepX = deltaX / length;
        float stepY = deltaY / length;
        for (float start = 0f; start < length; start += RELAY_DASH_LENGTH * 2f) {
            float end = Math.min(start + RELAY_DASH_LENGTH, length);
            planeVertex(frame, fromX + stepX * start, fromY + stepY * start);
            planeVertex(frame, fromX + stepX * end, fromY + stepY * end);
        }
    }

    private static float convoyShipAngle(Piece piece, Frame frame, int ship) {
        return travelAngle(piece, frame, piece.speed()) - ship * CONVOY_SPACING_DEGREES;
    }

    private void drawConvoyShip(Piece piece, Frame frame, int ship) {
        float shipAngle = convoyShipAngle(piece, frame, ship);
        aroundStar(shipAngle, piece.orbit() * frame.reach, frame);
        drawSprite(CONVOY_SPRITE, pieceX, pieceY, piece.size() * frame.radius * pieceScale, CONVOY_ASPECT, orbitHeading(shipAngle, frame), Color.WHITE,
                frame.alpha);
    }

    private void drawDebris(Piece piece, Frame frame) {
        SpriteAPI sheet = spriteCache.sprite(DEBRIS_SHEET);
        SpriteAPI glowSheet = spriteCache.sprite(DEBRIS_GLOW_SHEET);
        if (sheet == null) {
            return;
        }
        aroundStar(piece, frame, piece.speed());
        float size = piece.size() * frame.radius;
        float centreX = piecePlaneX;
        float centreY = piecePlaneY;
        for (int index = 0; index < DEBRIS_PIECES; index++) {
            double angle = Math.toRadians(piece.variant() * 37f + index * 72f + frame.elapsedSeconds * piece.speed());
            float distance = size * (0.6f + (index % 3) * 0.55f);
            onPlane(centreX + (float) Math.cos(angle) * distance, centreY + (float) Math.sin(angle) * distance, frame);
            int cell = (piece.variant() + index * 5) % SHEET_CELLS;
            float spin = (index % 2 == 0 ? 1f : -1f) * frame.elapsedSeconds * piece.speed() * 4f + index * 50f;
            drawCell(sheet, cell, pieceX, pieceY, size * pieceScale, spin, Color.WHITE, frame.alpha, false);
            if (glowSheet != null) {
                float flicker = (float) Math.sin(frame.elapsedSeconds * 3f + index) * 0.3f + 0.5f;
                drawCell(glowSheet, cell, pieceX, pieceY, size * pieceScale, spin, Color.WHITE, flicker * frame.alpha, true);
            }
        }
    }

    private void drawExcavation(Piece piece, Frame frame, int part) {
        if (!onPlanet(piece, frame)) {
            return;
        }
        int planet = piece.planet();
        BandArc[] arcs = arcs(frame);
        float cycle = frame.elapsedSeconds / SCAN_SECONDS + piece.variant() * 0.5f;
        float progress = cycle - (float) Math.floor(cycle);
        float ringRadius = frame.planetPlanePixels[planet] * (SCAN_START_SHARE + SCAN_GROWTH_SHARE * progress);
        try (GlScope scan = GlScope.flat().additive()) {
            drawScanArc(frame, frame.planetPlaneX[planet], frame.planetPlaneY[planet], ringRadius, arcs[part], (1f - progress) * 0.7f * frame.alpha);
        }
        if (part == arcs.length - 1) {
            float planetPixels = frame.planetPixels[planet];
            double siteAngle = Math.toRadians(piece.angle());
            float glint = (float) Math.sin(frame.elapsedSeconds * 4f + piece.variant()) * 0.3f + 0.7f;
            drawGlow(frame.planetX[planet] + (float) Math.cos(siteAngle) * planetPixels * 0.8f,
                    frame.planetY[planet] + (float) Math.sin(siteAngle) * planetPixels * 0.8f, Math.max(6f, planetPixels * 0.7f), SCAN_COLOUR,
                    glint * frame.alpha);
        }
    }

    private static void drawScanArc(Frame frame, float planeX, float planeY, float radius, BandArc arc, float alpha) {
        int segments = RING_CIRCLE.segments();
        int first = arc.index() * segments / arc.count();
        int last = (arc.index() + 1) * segments / arc.count();
        GL11.glLineWidth(1.5f);
        Misc.setColor(SCAN_COLOUR, alpha);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int step = first; step <= last; step++) {
            int point = step % segments;
            planeVertex(frame, planeX + RING_CIRCLE.cos(point) * radius, planeY + RING_CIRCLE.sin(point) * radius);
        }
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }

    private float placeStormPuff(Piece piece, Frame frame, int puff) {
        float size = piece.size() * frame.radius * 2f;
        float across = puff / (float) (STORM_PUFFS - 1) * 2f - 1f;
        float wobble = (float) Math.sin(puff * 1.7f + frame.elapsedSeconds * 0.5f) * size * 0.15f;
        aroundStar(travelAngle(piece, frame, piece.speed()) + across * STORM_ARC_DEGREES / 2f, piece.orbit() * frame.reach + wobble, frame);
        return across;
    }

    private void drawStormPuff(Piece piece, Frame frame, int puff) {
        SpriteAPI clouds = spriteCache.sprite(CLOUD_SHEET);
        if (clouds == null) {
            return;
        }
        int flashing = Math.floorMod((int) Math.floor(frame.elapsedSeconds / STORM_FLASH_SECONDS) * 3 + piece.variant(), STORM_PUFFS);
        float flash = 1f - (frame.elapsedSeconds / STORM_FLASH_SECONDS - (float) Math.floor(frame.elapsedSeconds / STORM_FLASH_SECONDS));
        float across = placeStormPuff(piece, frame, puff);
        float size = piece.size() * frame.radius * 2f * pieceScale;
        float edgeFade = 1f - across * across * 0.6f;
        SpriteDraw.drawCellAtCenter(clouds, CLOUD_SHEET_SIDE, CLOUD_SHEET_SIDE, puff + piece.variant(), pieceX, pieceY, size, size,
                puff * 47f + frame.elapsedSeconds * 4f, STORM_TINT, 0.45f * edgeFade * frame.alpha, false);
        if (puff == flashing) {
            drawGlow(pieceX, pieceY, size * 0.8f, LIGHTNING_COLOUR, flash * edgeFade * frame.alpha);
        }
    }

    private void drawListeningPost(Piece piece, Frame frame) {
        drawOrbitingSprite(piece, frame, POST_SPRITE, 1f, null);
        float sweepRadius = piece.size() * frame.radius * 2.2f;
        float sweep = frame.elapsedSeconds * RADAR_DEGREES_PER_SECOND;
        int steps = 8;
        try (GlScope radar = GlScope.flat().additive()) {
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            Misc.setColor(RADAR_COLOUR, 0.35f * frame.alpha);
            planeVertex(frame, piecePlaneX, piecePlaneY);
            for (int step = 0; step <= steps; step++) {
                float share = step / (float) steps;
                double angle = Math.toRadians(sweep - RADAR_WIDTH_DEGREES * share);
                Misc.setColor(RADAR_COLOUR, 0.3f * (1f - share) * frame.alpha);
                planeVertex(frame, piecePlaneX + (float) Math.cos(angle) * sweepRadius, piecePlaneY + (float) Math.sin(angle) * sweepRadius);
            }
            GL11.glEnd();
        }
    }
}
