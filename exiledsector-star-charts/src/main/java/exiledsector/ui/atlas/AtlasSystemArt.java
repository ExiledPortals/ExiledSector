package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.anomaly.breach.BreachFieldRenderer;
import exiledsector.charts.layout.sketch.ChartSystemSketch;
import exiledsector.ui.belt.AuroraBeltRenderer;
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
        NEBULA_PUFF, ACCRETION, WRECK, PULSAR, RESEARCH_STATION, ASTEROID, AURORA, FLARE, BREACH, BOSS, BATTLE, RELAY, CONVOY, DEBRIS,
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
        float radius;
        float reach;
        float starX;
        float starY;
        float starPixels;
        Color starColour = Color.WHITE;
        float elapsedSeconds;
        float alpha;
        float[] planetX = new float[0];
        float[] planetY = new float[0];
        float[] planetPixels = new float[0];

        void ensurePlanets(int count) {
            if (planetX.length < count) {
                planetX = new float[count];
                planetY = new float[count];
                planetPixels = new float[count];
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
    static final String BOSS_SPRITE = "graphics/ships/onslaught/onslaught_base.png";
    static final String CONVOY_SPRITE = "graphics/ships/buffalo/buffalo_base.png";
    static final String ATTACKER_SPRITE = "graphics/ships/lasher/lasher_base.png";
    static final String DEFENDER_SPRITE = "graphics/ships/wolf/wolf_base.png";
    static final List<String> RELAY_SPRITES = List.of("graphics/ships/nav_buoy.png", "graphics/ships/com_relay.png", "graphics/ships/sensor_array.png");
    static final List<Float> RELAY_ASPECTS = List.of(1f, 177f / 96f, 1f);
    static final List<String> ASTEROID_SPRITES = List.of("graphics/asteroids/asteroid1.png", "graphics/asteroids/asteroid2.png",
            "graphics/asteroids/asteroid3.png", "graphics/asteroids/asteroid4.png");
    static final float BOSS_ASPECT = 288f / 384f;
    static final float CONVOY_ASPECT = 100f / 150f;
    static final float ATTACKER_ASPECT = 68f / 96f;
    static final float DEFENDER_ASPECT = 52f / 96f;
    static final int NEBULA_PUFFS = 10;
    static final int DENSE_NEBULA_PUFFS = 18;
    static final float NEBULA_ALPHA = 0.6f;
    static final int ASTEROIDS_PER_BELT = 14;
    static final int DEBRIS_PIECES = 5;
    static final float SHEET_CELL = 0.25f;
    static final int SHEET_CELLS = 16;
    static final float FLARE_SECONDS = 2.4f;
    static final int FLARES = 3;
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
    static final Color ACCRETION_COLOUR = new Color(255, 150, 60);
    static final Color AURORA_COLOUR = new Color(110, 255, 200);
    static final Color AURORA_SHIFT_COLOUR = new Color(180, 120, 255);
    static final Color PULSAR_COLOUR = new Color(170, 210, 255);
    static final Color WRECK_TINT = new Color(170, 165, 160);
    static final Color BOSS_GLOW = new Color(255, 60, 40);
    static final Color FORGE_GLOW = new Color(255, 140, 40);
    static final Color RADAR_COLOUR = new Color(120, 255, 140);
    static final Color RELAY_LINK_COLOUR = new Color(110, 200, 255);
    static final Color SHOT_COLOUR = new Color(255, 210, 120);
    static final Color SCAN_COLOUR = new Color(255, 190, 90);
    static final Color STORM_TINT = new Color(110, 125, 185);
    static final Color LIGHTNING_COLOUR = new Color(190, 210, 255);
    private static final UnitCircle RING_CIRCLE = UnitCircle.of(40);

    private final SpriteCache spriteCache = new SpriteCache(AtlasSystemArt.class);
    private final Vector2f breachCentre = new Vector2f();
    private final Vector2f bandCentre = new Vector2f();
    private final float[] relayX = new float[RELAY_SPRITES.size()];
    private final float[] relayY = new float[RELAY_SPRITES.size()];
    private int relayCount;
    private float pieceX;
    private float pieceY;

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
            addAnomaly(anomalyId, new Random(seed ^ ((long) anomalyId.hashCode() << 32)), orbiting, pieces);
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

    private static void addAnomaly(String anomalyId, Random random, List<Integer> orbiting, List<Piece> pieces) {
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
            case "asteroid_storm" -> {
                for (float belt : new float[]{0.45f + random.nextFloat() * 0.12f, 0.7f + random.nextFloat() * 0.15f}) {
                    float speed = 3f + random.nextFloat() * 2f;
                    for (int index = 0; index < ASTEROIDS_PER_BELT; index++) {
                        float angle = 360f * index / ASTEROIDS_PER_BELT + random.nextFloat() * 15f;
                        pieces.add(new Piece(Kind.ASTEROID, -1, belt + (random.nextFloat() - 0.5f) * 0.04f, angle,
                                0.015f + random.nextFloat() * 0.02f, speed, random.nextInt(ASTEROID_SPRITES.size())));
                    }
                }
            }
            case "magnetic_storm" -> {
                pieces.add(new Piece(Kind.AURORA, -1, 0f, 0f, 0f, 0f, 0));
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
            case "powerful_boss" -> pieces.add(new Piece(Kind.BOSS, -1, 0.4f, random.nextFloat() * 360f, 0.09f, 6f, 0));
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
                float size = piece.size() * frame.radius * 2f;
                drawCell(sheet, piece.variant(), pieceX, pieceY, size, piece.angle() + frame.elapsedSeconds * piece.speed(), Color.WHITE,
                        NEBULA_ALPHA * frame.alpha, false);
            }
        }
    }

    void renderAnomalies(Layout layout, Frame frame) {
        if (frame.radius < MIN_ART_RADIUS || layout.pieces().isEmpty()) {
            return;
        }
        relayCount = 0;
        for (Piece piece : layout.pieces()) {
            switch (piece.kind()) {
                case ACCRETION -> drawAccretion(frame);
                case WRECK -> drawWreck(piece, frame);
                case PULSAR -> drawPulsar(piece, frame);
                case RESEARCH_STATION -> drawOrbitingSprite(piece, frame, RESEARCH_STATION_SPRITE, 1f, null);
                case ASTEROID -> drawAsteroid(piece, frame);
                case AURORA -> drawAurora(piece, frame);
                case FLARE -> drawFlare(piece, frame);
                case BREACH -> drawBreach(piece, frame);
                case BOSS -> drawOrbitingSprite(piece, frame, BOSS_SPRITE, BOSS_ASPECT, BOSS_GLOW);
                case BATTLE -> drawBattle(piece, frame);
                case RELAY -> drawRelay(piece, frame);
                case CONVOY -> drawConvoy(piece, frame);
                case DEBRIS -> drawDebris(piece, frame);
                case EXCAVATION -> drawExcavation(piece, frame);
                case STORM -> drawStorm(piece, frame);
                case FORGE -> drawOrbitingSprite(piece, frame, FORGE_SPRITE, 1f, FORGE_GLOW);
                case LISTENING_POST -> drawListeningPost(piece, frame);
                default -> {
                }
            }
        }
        drawRelayLinks(frame);
    }

    private static float orbitAngle(Piece piece, Frame frame, float degreesPerSecond) {
        return piece.angle() + frame.elapsedSeconds * degreesPerSecond;
    }

    private void aroundStar(Piece piece, Frame frame, float degreesPerSecond) {
        aroundStar(orbitAngle(piece, frame, degreesPerSecond), piece.orbit() * frame.reach, frame);
    }

    private void aroundStar(float angleDegrees, float distance, Frame frame) {
        double angle = Math.toRadians(angleDegrees);
        pieceX = frame.starX + (float) Math.cos(angle) * distance;
        pieceY = frame.starY + (float) Math.sin(angle) * distance;
    }

    private void drawCell(SpriteAPI sheet, int cell, float x, float y, float size, float angle, Color tint, float alpha, boolean additive) {
        sheet.setTexX((cell % 4) * SHEET_CELL * sheet.getTextureWidth());
        sheet.setTexY((cell / 4) * SHEET_CELL * sheet.getTextureHeight());
        sheet.setTexWidth(SHEET_CELL * sheet.getTextureWidth());
        sheet.setTexHeight(SHEET_CELL * sheet.getTextureHeight());
        sheet.setSize(size, size);
        sheet.setAngle(angle);
        sheet.setColor(tint);
        sheet.setAlphaMult(alpha);
        if (additive) {
            sheet.setAdditiveBlend();
        } else {
            sheet.setNormalBlend();
        }
        sheet.renderAtCenter(x, y);
    }

    private void drawSprite(String path, float x, float y, float height, float aspect, float facingDegrees, Color tint, float alpha) {
        SpriteDraw.drawAtCenter(spriteCache, path, x, y, height * aspect, height, tint, alpha, facingDegrees - 90f);
    }

    private void drawGlow(float x, float y, float size, Color colour, float alpha) {
        SpriteDraw.drawAdditiveAtCenter(spriteCache, GLOW, x, y, size, size, colour, alpha);
    }

    private void drawAccretion(Frame frame) {
        SpriteAPI texture = spriteCache.texture(AURORA_TEXTURE);
        if (texture == null) {
            return;
        }
        bandCentre.set(frame.starX, frame.starY);
        AuroraBeltRenderer.render(texture, new RadialBand(bandCentre, frame.starPixels * 1.25f, frame.starPixels * 2.4f), ACCRETION_COLOUR,
                0.8f * frame.alpha, frame.elapsedSeconds * 3f, 1f);
    }

    private void drawWreck(Piece piece, Frame frame) {
        SpriteAPI sheet = spriteCache.sprite(DEBRIS_SHEET);
        if (sheet != null) {
            aroundStar(piece, frame, piece.speed());
            drawCell(sheet, piece.variant(), pieceX, pieceY, piece.size() * frame.radius, frame.elapsedSeconds * 20f + piece.angle(), WRECK_TINT,
                    frame.alpha, false);
        }
    }

    private void drawPulsar(Piece piece, Frame frame) {
        float sweep = piece.angle() + frame.elapsedSeconds * piece.speed();
        float length = piece.orbit() * frame.reach;
        try (GlScope beam = GlScope.flat().additive()) {
            for (int side = 0; side < 2; side++) {
                double centre = Math.toRadians(sweep + side * 180f);
                double half = Math.toRadians(PULSAR_WIDTH_DEGREES);
                GL11.glBegin(GL11.GL_TRIANGLES);
                Misc.setColor(PULSAR_COLOUR, 0.55f * frame.alpha);
                GL11.glVertex2f(frame.starX, frame.starY);
                Misc.setColor(PULSAR_COLOUR, 0f);
                GL11.glVertex2f(frame.starX + (float) Math.cos(centre - half) * length, frame.starY + (float) Math.sin(centre - half) * length);
                GL11.glVertex2f(frame.starX + (float) Math.cos(centre + half) * length, frame.starY + (float) Math.sin(centre + half) * length);
                GL11.glEnd();
            }
        }
    }

    private void drawAsteroid(Piece piece, Frame frame) {
        aroundStar(piece, frame, piece.speed());
        float size = piece.size() * frame.radius;
        SpriteDraw.drawAtCenter(spriteCache, ASTEROID_SPRITES.get(piece.variant()), pieceX, pieceY, size, size, Color.WHITE, frame.alpha,
                piece.angle() * 3f + frame.elapsedSeconds * piece.speed() * 6f);
    }

    private void drawAurora(Piece piece, Frame frame) {
        SpriteAPI texture = spriteCache.texture(AURORA_TEXTURE);
        if (texture == null) {
            return;
        }
        boolean onPlanet = piece.planet() >= 0 && piece.planet() < frame.planetX.length;
        float bodyX = onPlanet ? frame.planetX[piece.planet()] : frame.starX;
        float bodyY = onPlanet ? frame.planetY[piece.planet()] : frame.starY;
        float bodyPixels = onPlanet ? frame.planetPixels[piece.planet()] : frame.starPixels;
        float shift = (float) Math.sin(frame.elapsedSeconds * 0.7f + piece.variant()) * 0.5f + 0.5f;
        bandCentre.set(bodyX, bodyY);
        AuroraBeltRenderer.render(texture, new RadialBand(bandCentre, bodyPixels * 1.1f, bodyPixels * 1.7f),
                Misc.interpolateColor(AURORA_COLOUR, AURORA_SHIFT_COLOUR, shift), 0.75f * frame.alpha, frame.elapsedSeconds, 1f);
    }

    private void drawFlare(Piece piece, Frame frame) {
        float cycle = frame.elapsedSeconds / FLARE_SECONDS + piece.variant() / (float) FLARES;
        float progress = cycle - (float) Math.floor(cycle);
        double angle = Math.toRadians(piece.angle() + Math.floor(cycle) * 137.5);
        float distance = frame.starPixels * (1f + 0.7f * progress);
        drawGlow(frame.starX + (float) Math.cos(angle) * distance, frame.starY + (float) Math.sin(angle) * distance,
                frame.starPixels * (0.6f + 1.4f * progress), frame.starColour, (1f - progress) * 0.9f * frame.alpha);
    }

    private void drawBreach(Piece piece, Frame frame) {
        aroundStar(piece, frame, piece.speed());
        breachCentre.set(pieceX, pieceY);
        BreachFieldRenderer.render(breachCentre, piece.size() * frame.radius, frame.alpha, frame.elapsedSeconds, 1f);
    }

    private void drawOrbitingSprite(Piece piece, Frame frame, String path, float aspect, Color glow) {
        float size = piece.size() * frame.radius;
        float orbitAngle = orbitAngle(piece, frame, piece.speed());
        aroundStar(orbitAngle, piece.orbit() * frame.reach, frame);
        if (glow != null) {
            float pulse = (float) Math.sin(frame.elapsedSeconds * 2f) * 0.25f + 0.75f;
            drawGlow(pieceX, pieceY, size * 2.4f, glow, 0.6f * pulse * frame.alpha);
        }
        drawSprite(path, pieceX, pieceY, size, aspect, orbitAngle + 90f, Color.WHITE, frame.alpha);
    }

    private void drawBattle(Piece piece, Frame frame) {
        float orbitAngle = orbitAngle(piece, frame, piece.speed());
        aroundStar(orbitAngle, piece.orbit() * frame.reach, frame);
        float size = piece.size() * frame.radius;
        double axis = Math.toRadians(orbitAngle + 90f);
        float axisX = (float) Math.cos(axis);
        float axisY = (float) Math.sin(axis);
        float centreX = pieceX;
        float centreY = pieceY;
        float facing = orbitAngle + 90f;
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? -1f : 1f;
            for (int ship = 0; ship < BATTLE_SHIPS; ship++) {
                float bob = (float) Math.sin(frame.elapsedSeconds * 1.3f + ship + side * 2f) * size * 0.15f;
                float spread = (ship - 1) * size * 1.3f;
                float shipX = centreX + axisX * sign * size * 2.5f - axisY * spread + axisX * bob;
                float shipY = centreY + axisY * sign * size * 2.5f + axisX * spread + axisY * bob;
                drawSprite(side == 0 ? ATTACKER_SPRITE : DEFENDER_SPRITE, shipX, shipY, size, side == 0 ? ATTACKER_ASPECT : DEFENDER_ASPECT,
                        side == 0 ? facing : facing + 180f, Color.WHITE, frame.alpha);
            }
        }
        float shotCycle = frame.elapsedSeconds / BATTLE_SHOT_SECONDS;
        int shot = (int) Math.floor(shotCycle) + piece.variant();
        float fade = 1f - (shotCycle - (float) Math.floor(shotCycle));
        int from = Math.floorMod(shot * 7, BATTLE_SHIPS);
        int to = Math.floorMod(shot * 11 + 1, BATTLE_SHIPS);
        float firing = shot % 2 == 0 ? -1f : 1f;
        float fromX = centreX + axisX * firing * size * 2.5f - axisY * (from - 1) * size * 1.3f;
        float fromY = centreY + axisY * firing * size * 2.5f + axisX * (from - 1) * size * 1.3f;
        float toX = centreX - axisX * firing * size * 2.5f - axisY * (to - 1) * size * 1.3f;
        float toY = centreY - axisY * firing * size * 2.5f + axisX * (to - 1) * size * 1.3f;
        try (GlScope shotScope = GlScope.flat().additive().lineWidth(1.5f)) {
            GL11.glBegin(GL11.GL_LINES);
            Misc.setColor(SHOT_COLOUR, fade * frame.alpha);
            GL11.glVertex2f(fromX, fromY);
            GL11.glVertex2f(toX, toY);
            GL11.glEnd();
        }
        drawGlow(toX, toY, size * 1.4f, SHOT_COLOUR, fade * 0.8f * frame.alpha);
    }

    private void drawRelay(Piece piece, Frame frame) {
        float size = piece.size() * frame.radius;
        aroundStar(piece, frame, piece.speed());
        drawSprite(RELAY_SPRITES.get(piece.variant()), pieceX, pieceY, size, RELAY_ASPECTS.get(piece.variant()),
                piece.angle() + frame.elapsedSeconds * 15f, Color.WHITE, frame.alpha);
        if (relayCount < relayX.length) {
            relayX[relayCount] = pieceX;
            relayY[relayCount] = pieceY;
            relayCount++;
        }
    }

    private void drawRelayLinks(Frame frame) {
        if (relayCount < 2) {
            return;
        }
        float pulse = (float) Math.sin(frame.elapsedSeconds * 2.5f) * 0.15f + 0.3f;
        try (GlScope links = GlScope.flat().additive().lineWidth(1f)) {
            GL11.glEnable(GL11.GL_LINE_STIPPLE);
            GL11.glLineStipple(2, (short) 0x0F0F);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            Misc.setColor(RELAY_LINK_COLOUR, pulse * frame.alpha);
            for (int index = 0; index < relayCount; index++) {
                GL11.glVertex2f(relayX[index], relayY[index]);
            }
            GL11.glEnd();
            GL11.glDisable(GL11.GL_LINE_STIPPLE);
        }
    }

    private void drawConvoy(Piece piece, Frame frame) {
        float size = piece.size() * frame.radius;
        float distance = piece.orbit() * frame.reach;
        float leadAngle = orbitAngle(piece, frame, piece.speed());
        for (int ship = 0; ship < CONVOY_SHIPS; ship++) {
            float shipAngle = leadAngle - ship * CONVOY_SPACING_DEGREES;
            aroundStar(shipAngle, distance, frame);
            drawSprite(CONVOY_SPRITE, pieceX, pieceY, size, CONVOY_ASPECT, shipAngle + 90f, Color.WHITE, frame.alpha);
        }
    }

    private void drawDebris(Piece piece, Frame frame) {
        SpriteAPI sheet = spriteCache.sprite(DEBRIS_SHEET);
        SpriteAPI glowSheet = spriteCache.sprite(DEBRIS_GLOW_SHEET);
        if (sheet == null) {
            return;
        }
        aroundStar(piece, frame, piece.speed());
        float size = piece.size() * frame.radius;
        float centreX = pieceX;
        float centreY = pieceY;
        for (int index = 0; index < DEBRIS_PIECES; index++) {
            double angle = Math.toRadians(piece.variant() * 37f + index * 72f + frame.elapsedSeconds * piece.speed());
            float distance = size * (0.6f + (index % 3) * 0.55f);
            float x = centreX + (float) Math.cos(angle) * distance;
            float y = centreY + (float) Math.sin(angle) * distance;
            int cell = (piece.variant() + index * 5) % SHEET_CELLS;
            float spin = (index % 2 == 0 ? 1f : -1f) * frame.elapsedSeconds * piece.speed() * 4f + index * 50f;
            drawCell(sheet, cell, x, y, size, spin, Color.WHITE, frame.alpha, false);
            if (glowSheet != null) {
                float flicker = (float) Math.sin(frame.elapsedSeconds * 3f + index) * 0.3f + 0.5f;
                drawCell(glowSheet, cell, x, y, size, spin, Color.WHITE, flicker * frame.alpha, true);
            }
        }
    }

    private void drawExcavation(Piece piece, Frame frame) {
        if (piece.planet() < 0 || piece.planet() >= frame.planetX.length) {
            return;
        }
        float planetX = frame.planetX[piece.planet()];
        float planetY = frame.planetY[piece.planet()];
        float planetPixels = frame.planetPixels[piece.planet()];
        float cycle = frame.elapsedSeconds / SCAN_SECONDS + piece.variant() * 0.5f;
        float progress = cycle - (float) Math.floor(cycle);
        float ringRadius = planetPixels * (1.1f + 1.3f * progress);
        try (GlScope scan = GlScope.flat().additive().lineWidth(1.5f)) {
            GL11.glBegin(GL11.GL_LINE_LOOP);
            Misc.setColor(SCAN_COLOUR, (1f - progress) * 0.7f * frame.alpha);
            for (int index = 0; index < RING_CIRCLE.segments(); index++) {
                GL11.glVertex2f(planetX + RING_CIRCLE.cos(index) * ringRadius, planetY + RING_CIRCLE.sin(index) * ringRadius);
            }
            GL11.glEnd();
        }
        double siteAngle = Math.toRadians(piece.angle());
        float glint = (float) Math.sin(frame.elapsedSeconds * 4f + piece.variant()) * 0.3f + 0.7f;
        drawGlow(planetX + (float) Math.cos(siteAngle) * planetPixels * 0.8f, planetY + (float) Math.sin(siteAngle) * planetPixels * 0.8f,
                Math.max(6f, planetPixels * 0.7f), SCAN_COLOUR, glint * frame.alpha);
    }

    private void drawStorm(Piece piece, Frame frame) {
        SpriteAPI clouds = spriteCache.sprite(CLOUD_SHEET);
        if (clouds == null) {
            return;
        }
        float centreAngle = orbitAngle(piece, frame, piece.speed());
        float distance = piece.orbit() * frame.reach;
        float size = piece.size() * frame.radius * 2f;
        int flashing = Math.floorMod((int) Math.floor(frame.elapsedSeconds / STORM_FLASH_SECONDS) * 3 + piece.variant(), STORM_PUFFS);
        float flash = 1f - (frame.elapsedSeconds / STORM_FLASH_SECONDS - (float) Math.floor(frame.elapsedSeconds / STORM_FLASH_SECONDS));
        for (int index = 0; index < STORM_PUFFS; index++) {
            float across = index / (float) (STORM_PUFFS - 1) * 2f - 1f;
            float wobble = (float) Math.sin(index * 1.7f + frame.elapsedSeconds * 0.5f) * size * 0.15f;
            aroundStar(centreAngle + across * STORM_ARC_DEGREES / 2f, distance + wobble, frame);
            float x = pieceX;
            float y = pieceY;
            float edgeFade = 1f - across * across * 0.6f;
            int cell = (index + piece.variant()) % 4;
            clouds.setTexX((cell % 2) * 0.5f * clouds.getTextureWidth());
            clouds.setTexY((cell / 2) * 0.5f * clouds.getTextureHeight());
            clouds.setTexWidth(0.5f * clouds.getTextureWidth());
            clouds.setTexHeight(0.5f * clouds.getTextureHeight());
            clouds.setSize(size, size);
            clouds.setAngle(index * 47f + frame.elapsedSeconds * 4f);
            clouds.setColor(STORM_TINT);
            clouds.setAlphaMult(0.45f * edgeFade * frame.alpha);
            clouds.setNormalBlend();
            clouds.renderAtCenter(x, y);
            if (index == flashing) {
                drawGlow(x, y, size * 0.8f, LIGHTNING_COLOUR, flash * edgeFade * frame.alpha);
            }
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
            GL11.glVertex2f(pieceX, pieceY);
            for (int step = 0; step <= steps; step++) {
                float share = step / (float) steps;
                double angle = Math.toRadians(sweep - RADAR_WIDTH_DEGREES * share);
                Misc.setColor(RADAR_COLOUR, 0.3f * (1f - share) * frame.alpha);
                GL11.glVertex2f(pieceX + (float) Math.cos(angle) * sweepRadius, pieceY + (float) Math.sin(angle) * sweepRadius);
            }
            GL11.glEnd();
        }
    }
}
