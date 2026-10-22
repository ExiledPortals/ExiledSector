package exiledsector.ui.cargo;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledlib.skills.skilleffect.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TexturedQuadBatch;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;

public final class CacheIconFx {

    public static final String STILL_PATH = "graphics/icons/socketables/exiledSector_modspec_cache.png";
    static final String ROCK_TEXTURE = "graphics/asteroids/asteroid4.png";
    static final String GLOW_TEXTURE = "graphics/fx/glow64.png";
    static final String ARC_TEXTURE = "graphics/fx/emp_arcs.png";
    static final String STARBURST_TEXTURE = "graphics/fx/starburst_glow1.png";
    static final String DUST_TEXTURE = "graphics/fx/smoke32.png";
    static final float CYCLE_SECONDS = 6f;
    static final float SCAN_START = 0.06f;
    static final float SCAN_END = 0.26f;
    static final float[] ALL_CLEAR_BLINKS = {0.3f, 0.34f};
    static final float BLINK_HALF = 0.012f;
    static final float HEAT_START = 0.37f;
    static final float CRACK_KEY = 0.42f;
    static final float OPEN_SPAN = 0.05f;
    static final float CHIP_RISE_START = 0.45f;
    static final float CHIP_STAGGER = 0.04f;
    static final float CHIP_RISE_SPAN = 0.08f;
    static final float CHIP_SINK_START = 0.7f;
    static final float CHIP_SINK_STAGGER = 0.02f;
    static final float CHIP_SINK_SPAN = 0.06f;
    static final float CLOSE_START = 0.78f;
    static final float CLOSE_END = 0.86f;
    static final float CRACKLE_START = CRACK_KEY - 0.015f;
    static final float CRACKLE_END = CRACK_KEY + 0.07f;
    static final float DUST_SPAN = 0.12f;
    static final int MAX_CHIPS = 3;
    static final float[][][] CHIP_LAYOUTS = {{{0f, 22f}}, {{-11f, 21f}, {11f, 21f}}, {{-16f, 18f}, {0f, 25f}, {16f, 18f}}};
    static final float[][] SEAM = {{0.53f, 0f}, {0.49f, 0.1f}, {0.56f, 0.22f}, {0.51f, 0.31f}, {0.47f, 0.45f}, {0.55f, 0.57f}, {0.5f, 0.66f},
            {0.46f, 0.8f}, {0.51f, 0.9f}, {0.49f, 1f}};
    static final float ROCK_WIDTH = 50f;
    static final float ROCK_HEIGHT = ROCK_WIDTH * 83f / 88f;
    static final float ROCK_CENTRE_Y = -10f;
    static final float HALF_TILT_DEGREES = 9f;
    static final float HALF_SLIDE = 2.2f;
    private static final float DESIGN_SIZE = 80f;
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int WHOLE = 2;
    private static final Color ROCK_TINT = new Color(150, 142, 140);
    private static final Color SCAN_COLOR = new Color(150, 215, 255);
    private static final Color DUST_COLOR = new Color(165, 155, 145);
    private static final float DRIFT_DEGREES = 3.5f;
    private static final float DRIFT_RATE = 0.55f;
    private static final float BOB = 0.7f;
    private static final float BOB_RATE = 0.9f;
    private static final float SEAM_IDLE = 0.22f;
    private static final float EDGE_DEPTH = 0.14f;
    private static final float SCAN_REACH = 36f;
    private static final float SCAN_BAND = 5f;
    private static final float SCAN_TRAIL = 9f;
    private static final int SCAN_ROWS = 16;
    private static final float BEACON_U = 0.7f;
    private static final float BEACON_V = 0.24f;
    private static final float BEACON_PERIOD = 1.1f;
    private static final float BEACON_ON = 0.12f;
    private static final float BEACON_SIZE = 6f;
    private static final float CRACKLE_STEP_SECONDS = 0.035f;
    private static final float CRACKLE_SIZE = 34f;
    private static final float CRACKLE_SHARE = 0.45f;
    private static final int CRACKLE_SEGMENTS = 16;
    private static final UnitCircle CRACKLE_CIRCLE = UnitCircle.of(CRACKLE_SEGMENTS);
    private static final float STARBURST_SIZE = 46f;
    private static final int DUST_PUFFS = 6;
    private static final float DUST_TRAVEL = 8f;
    private static final float MOUTH_V = 0.12f;
    private static final float CHIP_RADIUS = 8f;
    private static final float CHIP_START_SCALE = 0.35f;
    private static final float CHIP_BOB = 0.8f;
    private static final float CHIP_TINT = 0.25f;
    private static final int CHIP_SCANLINES = 5;
    private static final float CHIP_SCAN_RATE = 0.8f;
    private static final float GLITCH_STEP_SECONDS = 0.09f;
    private static final float GLITCH_CHANCE = 0.07f;
    private static final float GLITCH_REACH = 1.8f;
    private static final float HINGE_X = (SEAM[SEAM.length - 1][0] - 0.5f) * ROCK_WIDTH;
    private static final float HINGE_Y = ROCK_CENTRE_Y - ROCK_HEIGHT / 2f;
    private static final float[] POSE = new float[6];
    private static final float[] PART_COS = {1f, 1f, 1f};
    private static final float[] PART_SIN = new float[3];
    private static final float[] PART_SLIDE = new float[3];
    private static final float[] PLACED = new float[2];
    private static final float[] CHIP_X = new float[MAX_CHIPS];
    private static final float[] CHIP_Y = new float[MAX_CHIPS];
    private static final float[] CHIP_R = new float[MAX_CHIPS];
    private static final float[] CHIP_ALPHA = new float[MAX_CHIPS];
    private static final boolean[] CHIP_GLITCHED = new boolean[MAX_CHIPS];
    private static final TexturedQuadBatch DUST_BATCH = new TexturedQuadBatch();

    private CacheIconFx() {
    }

    static void render(CacheLook cache, SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h,
                       float alphaMult, int phaseSeed) {
        boolean animated = ItemAnimationSetting.mode() == ItemAnimationSetting.Mode.FULL;
        float size = Math.min(w, h);
        float unit = size / DESIGN_SIZE;
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        boolean framed = look.framed();
        float scale = (framed ? CargoIconFx.ICON_INSET : 1f) * unit;
        Color color = look.color();
        SpriteAPI rock = animated ? sprites.texture(ROCK_TEXTURE) : null;
        if (rock == null) {
            SpriteAPI still = iconPath == null ? null : sprites.sprite(iconPath);
            if (still != null) {
                SpriteDraw.drawAtCenter(still, cx, cy, DESIGN_SIZE * scale, DESIGN_SIZE * scale, null, alphaMult, 0f);
            }
            if (framed) {
                AnchorIconFx.drawFrame(color, cx, cy, unit, alphaMult);
                AnchorIconFx.drawLights(sprites, color, cx, cy, unit, AnchorIconFx.IDLE_WAVE, alphaMult);
            }
            return;
        }
        float t = CargoIconFx.seconds() + CargoIconFx.hash(phaseSeed, 1, 1) * CYCLE_SECONDS;
        float cycle = cycleOf(t);
        float open = openness(cycle);
        float flash = DoctrineIconFx.flash(cycle, CRACK_KEY);
        pose(cx, cy, scale, t, open);

        drawCavity(sprites, color, open, alphaMult);
        drawRock(rock, color, open, cycle, alphaMult);
        drawSeam(color, seamHeat(cycle, t) * (1f - AnchorIconFx.smooth(0f, 0.15f, open)), scale, alphaMult);
        drawBeacon(sprites, color, beaconLevel(cycle, t) * (1f - AnchorIconFx.smooth(0f, 0.2f, open)), scale, alphaMult);
        drawCrackle(sprites, color, crackleAlpha(cycle), phaseSeed, t, scale, alphaMult);
        drawBurst(sprites, color, flash, t, scale, alphaMult);
        drawDust(sprites, cycle, scale, alphaMult);
        drawChips(sprites, cache.chipIconPaths(), color, cx, cy, scale, unit, cycle, t, phaseSeed, alphaMult);
        if (framed) {
            AnchorIconFx.drawFrame(flash > 0.01f ? AnchorIconFx.lighten(color, 0.6f * flash) : color, cx, cy, unit, alphaMult);
            AnchorIconFx.drawLights(sprites, color, cx, cy, unit, DoctrineIconFx.wave(cycle, CRACK_KEY), alphaMult);
        }
    }

    static float cycleOf(float seconds) {
        float cycle = seconds % CYCLE_SECONDS / CYCLE_SECONDS;
        return cycle < 0f ? cycle + 1f : cycle;
    }

    static float openness(float cycle) {
        float progress = Math.max(0f, Math.min(1f, (cycle - CRACK_KEY) / OPEN_SPAN));
        return backOut(progress) * (1f - AnchorIconFx.smooth(CLOSE_START, CLOSE_END, cycle));
    }

    static float backOut(float progress) {
        float overshoot = 1.70158f;
        float shifted = progress - 1f;
        return 1f + (overshoot + 1f) * shifted * shifted * shifted + overshoot * shifted * shifted;
    }

    static float scanProgress(float cycle) {
        return (cycle - SCAN_START) / (SCAN_END - SCAN_START);
    }

    static float beaconLevel(float cycle, float seconds) {
        for (float blink : ALL_CLEAR_BLINKS) {
            if (Math.abs(cycle - blink) < BLINK_HALF) {
                return 1f;
            }
        }
        boolean runningDark = cycle >= SCAN_START && cycle < CLOSE_END;
        return runningDark || seconds % BEACON_PERIOD >= BEACON_ON ? 0f : 0.8f;
    }

    static float seamHeat(float cycle, float seconds) {
        float idle = SEAM_IDLE * (0.7f + 0.3f * (float) Math.sin(seconds * 1.6f));
        float heat = cycle < CRACK_KEY ? AnchorIconFx.smooth(HEAT_START, CRACK_KEY, cycle) : 0f;
        return Math.max(idle, heat);
    }

    static float crackleAlpha(float cycle) {
        if (cycle < CRACKLE_START || cycle >= CRACKLE_END) {
            return 0f;
        }
        float remaining = 1f - (cycle - CRACKLE_START) / (CRACKLE_END - CRACKLE_START);
        return remaining * remaining;
    }

    static float dustAge(float cycle, float start) {
        float age = (cycle - start) / DUST_SPAN;
        return age >= 0f && age < 1f ? age : -1f;
    }

    static int chipCount(List<String> chipIconPaths) {
        return Math.min(MAX_CHIPS, chipIconPaths.size());
    }

    static float chipPresence(float cycle, int index, int count) {
        float rise = Math.max(0f, Math.min(1f, (cycle - CHIP_RISE_START - index * CHIP_STAGGER) / CHIP_RISE_SPAN));
        float sinkStart = CHIP_SINK_START + (count - 1 - index) * CHIP_SINK_STAGGER;
        float sink = Math.max(0f, Math.min(1f, (cycle - sinkStart) / CHIP_SINK_SPAN));
        float risen = 1f - (1f - rise) * (1f - rise) * (1f - rise);
        return risen * (1f - sink * sink * sink);
    }

    static float rockX(float u) {
        return (u - 0.5f) * ROCK_WIDTH;
    }

    static float rockY(float v) {
        return ROCK_CENTRE_Y + (0.5f - v) * ROCK_HEIGHT;
    }

    private static float seamFade(float v) {
        return AnchorIconFx.smooth(0f, 0.15f, v) * (1f - AnchorIconFx.smooth(0.85f, 1f, v));
    }

    private static void pose(float cx, float cy, float scale, float t, float open) {
        double drift = Math.toRadians(DRIFT_DEGREES * Math.sin(t * DRIFT_RATE));
        POSE[0] = cx;
        POSE[1] = cy;
        POSE[2] = scale;
        POSE[3] = (float) Math.cos(drift);
        POSE[4] = (float) Math.sin(drift);
        POSE[5] = BOB * (float) Math.sin(t * BOB_RATE);
        for (int part = LEFT; part <= RIGHT; part++) {
            float side = part == LEFT ? -1f : 1f;
            double tilt = Math.toRadians(-side * HALF_TILT_DEGREES * open);
            PART_COS[part] = (float) Math.cos(tilt);
            PART_SIN[part] = (float) Math.sin(tilt);
            PART_SLIDE[part] = side * HALF_SLIDE * open;
        }
    }

    private static void place(float localX, float localY, int part) {
        float fromHingeX = localX - HINGE_X;
        float fromHingeY = localY - HINGE_Y;
        float partX = HINGE_X + fromHingeX * PART_COS[part] - fromHingeY * PART_SIN[part] + PART_SLIDE[part];
        float partY = HINGE_Y + fromHingeX * PART_SIN[part] + fromHingeY * PART_COS[part];
        float fromCentreY = partY - ROCK_CENTRE_Y;
        PLACED[0] = POSE[0] + (partX * POSE[3] - fromCentreY * POSE[4]) * POSE[2];
        PLACED[1] = POSE[1] + (ROCK_CENTRE_Y + partX * POSE[4] + fromCentreY * POSE[3] + POSE[5]) * POSE[2];
    }

    private static void vertex(float u, float v, int part) {
        place(rockX(u), rockY(v), part);
        GL11.glVertex2f(PLACED[0], PLACED[1]);
    }

    private static void texturedVertex(SpriteAPI rock, float u, float v, int part) {
        GL11.glTexCoord2f(rock.getTexX() + u * rock.getTexWidth(), rock.getTexY() + (1f - v) * rock.getTexHeight());
        vertex(u, v, part);
    }

    private static void drawCavity(SpriteCache sprites, Color color, float open, float alphaMult) {
        if (open <= 0.01f) {
            return;
        }
        float scale = POSE[2];
        place(0f, ROCK_CENTRE_Y, WHOLE);
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_TEXTURE, PLACED[0], PLACED[1], 22f * scale * open, ROCK_HEIGHT * scale, color,
                Math.min(1f, open) * alphaMult);
        Color hot = AnchorIconFx.lighten(color, 0.65f);
        try (GlScope scope = GlScope.flat().additive()) {
            for (int side = 0; side < 2; side++) {
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                for (float[] seamPoint : SEAM) {
                    float fade = seamFade(seamPoint[1]) * alphaMult;
                    place(rockX(seamPoint[0]), rockY(seamPoint[1]), side == 0 ? LEFT : RIGHT);
                    float edgeX = PLACED[0];
                    float edgeY = PLACED[1];
                    place(rockX(seamPoint[0]), rockY(seamPoint[1]), side == 0 ? RIGHT : LEFT);
                    Misc.setColor(color, 0.7f * fade);
                    GL11.glVertex2f(edgeX, edgeY);
                    Misc.setColor(hot, 0.95f * fade);
                    GL11.glVertex2f((edgeX + PLACED[0]) / 2f, (edgeY + PLACED[1]) / 2f);
                }
                GL11.glEnd();
            }
        }
    }

    private static void drawRock(SpriteAPI rock, Color color, float open, float cycle, float alphaMult) {
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)) {
            rock.bindTexture();
            Misc.setColor(ROCK_TINT, alphaMult);
            for (int part = LEFT; part <= RIGHT; part++) {
                float outerU = part == LEFT ? 0f : 1f;
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                for (float[] seamPoint : SEAM) {
                    texturedVertex(rock, outerU, seamPoint[1], part);
                    texturedVertex(rock, seamPoint[0], seamPoint[1], part);
                }
                GL11.glEnd();
            }
            scope.additive();
            if (open > 0.01f) {
                drawEdgeLight(rock, color, Math.min(1f, open) * alphaMult);
            }
            float scan = scanProgress(cycle);
            if (scan > 0f && scan < 1f) {
                drawScanHighlight(rock, scan, alphaMult);
            }
        }
        float scan = scanProgress(cycle);
        if (scan > 0f && scan < 1f) {
            drawScanBar(scan, alphaMult);
        }
    }

    private static void drawEdgeLight(SpriteAPI rock, Color color, float alpha) {
        for (int part = LEFT; part <= RIGHT; part++) {
            float inward = part == LEFT ? -EDGE_DEPTH : EDGE_DEPTH;
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (float[] seamPoint : SEAM) {
                Misc.setColor(color, 0.9f * alpha);
                texturedVertex(rock, seamPoint[0], seamPoint[1], part);
                Misc.setColor(color, 0f);
                texturedVertex(rock, seamPoint[0] + inward, seamPoint[1], part);
            }
            GL11.glEnd();
        }
    }

    private static float scanBarY(float scan) {
        return SCAN_REACH - 2f * SCAN_REACH * scan;
    }

    private static float scanEnvelope(float scan) {
        return AnchorIconFx.smooth(0f, 0.1f, scan) * (1f - AnchorIconFx.smooth(0.9f, 1f, scan));
    }

    private static void drawScanHighlight(SpriteAPI rock, float scan, float alphaMult) {
        float barY = scanBarY(scan);
        float envelope = scanEnvelope(scan) * alphaMult;
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int row = 0; row <= SCAN_ROWS; row++) {
            float v = (float) row / SCAN_ROWS;
            float offset = (rockY(v) - barY) / SCAN_BAND;
            Misc.setColor(SCAN_COLOR, (float) Math.exp(-offset * offset) * 0.8f * envelope);
            texturedVertex(rock, 0f, v, WHOLE);
            texturedVertex(rock, 1f, v, WHOLE);
        }
        GL11.glEnd();
    }

    private static void drawScanBar(float scan, float alphaMult) {
        float scale = POSE[2];
        float barY = POSE[1] + scanBarY(scan) * scale;
        float left = POSE[0] - SCAN_REACH * scale;
        float right = POSE[0] + SCAN_REACH * scale;
        float envelope = scanEnvelope(scan) * alphaMult;
        try (GlScope scope = GlScope.flat().additive()) {
            GL11.glBegin(GL11.GL_QUADS);
            Misc.setColor(SCAN_COLOR, 0.2f * envelope);
            GL11.glVertex2f(left, barY);
            GL11.glVertex2f(right, barY);
            Misc.setColor(SCAN_COLOR, 0f);
            GL11.glVertex2f(right, barY + SCAN_TRAIL * scale);
            GL11.glVertex2f(left, barY + SCAN_TRAIL * scale);
            GL11.glEnd();
            HoloShapes.line(left, barY, right, barY, Math.max(1f, scale), SCAN_COLOR, 0.75f * envelope);
        }
    }

    private static void drawSeam(Color color, float heat, float scale, float alphaMult) {
        if (heat <= 0.01f) {
            return;
        }
        Color hot = AnchorIconFx.lighten(color, 0.3f + 0.4f * heat);
        try (GlScope scope = GlScope.flat().additive().lineWidth(Math.max(1f, 1.4f * scale))) {
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (float[] seamPoint : SEAM) {
                Misc.setColor(hot, heat * seamFade(seamPoint[1]) * alphaMult);
                vertex(seamPoint[0], seamPoint[1], WHOLE);
            }
            GL11.glEnd();
        }
    }

    private static void drawBeacon(SpriteCache sprites, Color color, float level, float scale, float alphaMult) {
        if (level <= 0.01f) {
            return;
        }
        place(rockX(BEACON_U), rockY(BEACON_V), RIGHT);
        float beaconSize = BEACON_SIZE * scale;
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_TEXTURE, PLACED[0], PLACED[1], beaconSize, beaconSize, AnchorIconFx.lighten(color, 0.3f),
                level * alphaMult);
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_TEXTURE, PLACED[0], PLACED[1], beaconSize * 0.4f, beaconSize * 0.4f, Color.WHITE,
                level * alphaMult);
    }

    private static void drawCrackle(SpriteCache sprites, Color color, float alpha, int phaseSeed, float t, float scale, float alphaMult) {
        SpriteAPI arcs = alpha > 0.01f ? sprites.texture(ARC_TEXTURE) : null;
        if (arcs == null) {
            return;
        }
        int step = (int) (t / CRACKLE_STEP_SECONDS);
        place(0f, ROCK_CENTRE_Y + 4f, WHOLE);
        float half = CRACKLE_SIZE * scale / 2f;
        Color tint = AnchorIconFx.lighten(color, 0.45f);
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
            arcs.bindTexture();
            for (int layer = 0; layer < 2; layer++) {
                float fromU = CargoIconFx.hash(phaseSeed, 211 + layer, step) * (1f - CRACKLE_SHARE);
                float fromV = CargoIconFx.hash(phaseSeed, 223 + layer, step) * (1f - CRACKLE_SHARE);
                float angle = CargoIconFx.hash(phaseSeed, 227 + layer, step) * HoloShapes.TWO_PI;
                float spinCos = (float) Math.cos(angle);
                float spinSin = (float) Math.sin(angle);
                GL11.glBegin(GL11.GL_TRIANGLE_FAN);
                Misc.setColor(tint, alpha * (layer == 0 ? 1f : 0.6f) * alphaMult);
                arcVertex(arcs, fromU, fromV, 0f, 0f, 0f, 0f, half);
                Misc.setColor(tint, 0f);
                for (int segment = 0; segment <= CRACKLE_SEGMENTS; segment++) {
                    int index = segment % CRACKLE_SEGMENTS;
                    float cos = CRACKLE_CIRCLE.cos(index);
                    float sin = CRACKLE_CIRCLE.sin(index);
                    arcVertex(arcs, fromU, fromV, cos, sin, cos * spinCos - sin * spinSin, cos * spinSin + sin * spinCos, half);
                }
                GL11.glEnd();
            }
        }
    }

    private static void arcVertex(SpriteAPI arcs, float fromU, float fromV, float textureX, float textureY, float screenX, float screenY,
                                  float half) {
        float alongU = 0.5f + 0.5f * textureX;
        float alongV = 0.5f + 0.5f * textureY;
        GL11.glTexCoord2f(arcs.getTexX() + (fromU + alongU * CRACKLE_SHARE) * arcs.getTexWidth(),
                arcs.getTexY() + (fromV + alongV * CRACKLE_SHARE) * arcs.getTexHeight());
        GL11.glVertex2f(PLACED[0] + screenX * half, PLACED[1] + screenY * half);
    }

    private static void drawBurst(SpriteCache sprites, Color color, float flash, float t, float scale, float alphaMult) {
        if (flash <= 0.01f) {
            return;
        }
        place(0f, rockY(MOUTH_V), WHOLE);
        float burstSize = STARBURST_SIZE * scale * (0.6f + 0.4f * flash);
        SpriteDraw.drawAdditiveAtCenter(sprites, STARBURST_TEXTURE, PLACED[0], PLACED[1], burstSize, burstSize, AnchorIconFx.lighten(color, 0.5f),
                flash * alphaMult, t * 20f % 360f);
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_TEXTURE, PLACED[0], PLACED[1], burstSize * 0.7f, burstSize * 0.7f, color, flash * alphaMult);
    }

    private static void drawDust(SpriteCache sprites, float cycle, float scale, float alphaMult) {
        float crackAge = dustAge(cycle, CRACK_KEY);
        float closeAge = dustAge(cycle, CLOSE_END);
        if (crackAge < 0f && closeAge < 0f) {
            return;
        }
        SpriteAPI dust = sprites.texture(DUST_TEXTURE);
        if (dust == null) {
            return;
        }
        float age = crackAge >= 0f ? crackAge : closeAge;
        float strength = crackAge >= 0f ? 1f : 0.55f;
        float eased = 1f - (1f - age) * (1f - age);
        float fade = (1f - age) * (1f - age) * 0.5f * strength * alphaMult;
        try (TexturedQuadBatch batch = DUST_BATCH.beginNormal(dust)) {
            for (int puff = 0; puff < DUST_PUFFS; puff++) {
                float[] seamPoint = SEAM[1 + puff % (SEAM.length - 2)];
                float side = puff % 2 == 0 ? -1f : 1f;
                place(rockX(seamPoint[0]) + side * DUST_TRAVEL * strength * eased, rockY(seamPoint[1]) + 3f * eased, WHOLE);
                float puffSize = (4f + 7f * eased) * strength * scale;
                batch.quadAtCenter(PLACED[0], PLACED[1], puffSize, puffSize, puff * 67f + age * 40f, DUST_COLOR, fade);
            }
        }
    }

    private static void drawChips(SpriteCache sprites, List<String> chipIconPaths, Color color, float cx, float cy, float scale, float unit,
                                  float cycle, float t, int phaseSeed, float alphaMult) {
        int count = chipCount(chipIconPaths);
        if (count == 0) {
            return;
        }
        place(0f, rockY(MOUTH_V), WHOLE);
        float mouthX = PLACED[0];
        float mouthY = PLACED[1];
        boolean anyShown = false;
        int glitchStep = (int) (t / GLITCH_STEP_SECONDS);
        for (int index = 0; index < count; index++) {
            float presence = chipPresence(cycle, index, count);
            CHIP_ALPHA[index] = presence * alphaMult;
            if (presence <= 0.01f) {
                continue;
            }
            anyShown = true;
            float[] hover = CHIP_LAYOUTS[count - 1][index];
            float hoverX = cx + hover[0] * scale;
            float hoverY = cy + (hover[1] + CHIP_BOB * presence * (float) Math.sin(t * 2.1f + index * 2.1f)) * scale;
            CHIP_GLITCHED[index] = CargoIconFx.hash(phaseSeed, 300 + index, glitchStep) < GLITCH_CHANCE;
            float jitter = CHIP_GLITCHED[index] ? (CargoIconFx.hash(phaseSeed, 310 + index, glitchStep) * 2f - 1f) * GLITCH_REACH * scale : 0f;
            CHIP_X[index] = mouthX + (hoverX - mouthX) * presence + jitter;
            CHIP_Y[index] = mouthY + (hoverY - mouthY) * presence;
            CHIP_R[index] = CHIP_RADIUS * scale * (CHIP_START_SCALE + (1f - CHIP_START_SCALE) * presence);
        }
        if (!anyShown) {
            return;
        }
        Color light = AnchorIconFx.lighten(color, 0.45f);
        try (GlScope scope = GlScope.flat().additive()) {
            GL11.glBegin(GL11.GL_TRIANGLES);
            for (int index = 0; index < count; index++) {
                if (CHIP_ALPHA[index] <= 0.01f * alphaMult) {
                    continue;
                }
                Misc.setColor(light, 0.3f * CHIP_ALPHA[index]);
                GL11.glVertex2f(mouthX, mouthY);
                Misc.setColor(light, 0.05f * CHIP_ALPHA[index]);
                GL11.glVertex2f(CHIP_X[index] - CHIP_R[index], CHIP_Y[index]);
                GL11.glVertex2f(CHIP_X[index] + CHIP_R[index], CHIP_Y[index]);
            }
            GL11.glEnd();
        }
        Color iconTint = DoctrineIconFx.mix(Color.WHITE, light, CHIP_TINT);
        for (int index = 0; index < count; index++) {
            if (CHIP_ALPHA[index] <= 0.01f * alphaMult) {
                continue;
            }
            float diameter = CHIP_R[index] * 2f;
            SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_TEXTURE, CHIP_X[index], CHIP_Y[index], diameter * 1.6f, diameter * 1.6f, color,
                    0.55f * CHIP_ALPHA[index]);
            SpriteDraw.drawAtCenter(sprites, chipIconPaths.get(index), CHIP_X[index], CHIP_Y[index], diameter, diameter, iconTint,
                    0.92f * CHIP_ALPHA[index]);
            if (CHIP_GLITCHED[index]) {
                SpriteDraw.drawAdditiveAtCenter(sprites, chipIconPaths.get(index), CHIP_X[index] + GLITCH_REACH * scale, CHIP_Y[index], diameter,
                        diameter, color, 0.5f * CHIP_ALPHA[index]);
            }
        }
        try (GlScope scope = GlScope.flat().additive()) {
            float scroll = t * CHIP_SCAN_RATE % 1f;
            for (int index = 0; index < count; index++) {
                if (CHIP_ALPHA[index] <= 0.01f * alphaMult) {
                    continue;
                }
                drawChipScanlines(CHIP_X[index], CHIP_Y[index], CHIP_R[index], scroll, light, 0.22f * CHIP_ALPHA[index]);
                HoloShapes.circle(CHIP_X[index], CHIP_Y[index], CHIP_R[index] + 0.6f * scale, light, Math.max(1f, unit), 0.85f * CHIP_ALPHA[index]);
            }
        }
    }

    private static void drawChipScanlines(float chipX, float chipY, float radius, float scroll, Color color, float alpha) {
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINES);
        for (int line = 0; line < CHIP_SCANLINES; line++) {
            float offset = 1f - 2f * ((line + scroll) / CHIP_SCANLINES);
            float chord = (float) Math.sqrt(Math.max(0f, 1f - offset * offset)) * radius;
            GL11.glVertex2f(chipX - chord, chipY + offset * radius);
            GL11.glVertex2f(chipX + chord, chipY + offset * radius);
        }
        GL11.glEnd();
    }
}
