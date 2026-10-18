package exiledsector.ui.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public final class CargoIconFx {

    public static final float BURST_SECONDS = 1.1f;
    static final float ICON_INSET = 0.84f;
    private static final float GLOW_INNER = 0.15f;
    private static final float GLOW_OUTER = 0.98f;
    private static final float HOVER_GLOW_BOOST = 0.35f;
    private static final float BREATH_SPEED = 1.7f;
    private static final float BREATH_DEPTH = 0.22f;
    private static final float HEARTBEAT_PERIOD = 2.6f;
    private static final float HEARTBEAT_WIDTH = 0.07f;
    private static final float RING_RADIUS = 0.86f;
    private static final float RING_DEGREES_PER_SECOND = 22f;
    private static final float RING_ALPHA = 0.45f;
    private static final float TICK_RADIUS = 0.95f;
    private static final float TICK_DEGREES_PER_SECOND = 13f;
    private static final float TICK_ALPHA = 0.35f;
    private static final int SPARKLE_SLOTS = 2;
    private static final float SPARKLE_PERIOD = 2.4f;
    private static final float SPARKLE_LIFE = 0.55f;
    private static final float SPARKLE_SPREAD = 0.68f;
    private static final float SPARKLE_ARM = 0.2f;
    private static final int ARC_SLOTS = 2;
    private static final float ARC_PERIOD = 1.4f;
    private static final float ARC_LIFE = 0.28f;
    private static final float ARC_RESHAPE_SECONDS = 0.05f;
    private static final int ARC_POINTS = 9;
    private static final float ARC_JITTER = 0.07f;
    private static final float ARC_INSET = 0.96f;
    private static final float SWEEP_PERIOD = 3.4f;
    private static final float SWEEP_SECONDS = 1.2f;
    private static final float SWEEP_BAND = 0.32f;
    private static final float SWEEP_SLANT = 0.55f;
    private static final float SWEEP_ROW_PIXELS = 2f;
    private static final float SWEEP_ALPHA = 0.4f;
    private static final float BRACKET_ARM = 0.26f;
    private static final float BADGE_SIZE = 3.5f;
    private static final float BADGE_MARGIN = 7f;
    private static final Color SPARK_COLOR = new Color(255, 250, 235);
    private static final Color ARC_CORE_COLOR = new Color(235, 245, 255);
    private static final long START_NANOS = System.nanoTime();
    private static final Map<String, Float> ASPECTS = new HashMap<>();

    private CargoIconFx() {
    }

    public static float seconds() {
        return (System.nanoTime() - START_NANOS) / 1_000_000_000f;
    }

    public static void render(SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h,
                              float alphaMult, float glowMult, int phaseSeed, float burstAge) {
        ItemAnimationSetting.Mode mode = ItemAnimationSetting.mode();
        float size = Math.min(w, h);
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        if (mode == ItemAnimationSetting.Mode.OFF || look == null) {
            drawIcon(sprites, iconPath, cx, cy, size, alphaMult);
            return;
        }
        boolean full = mode == ItemAnimationSetting.Mode.FULL;
        float half = size / 2f;
        float t = seconds() + hash(phaseSeed, 1, 1) * SPARKLE_PERIOD * 4f;
        drawBackLayer(look, cx, cy, half, t, full, alphaMult, glowMult);
        float iconSize = look.framed() ? size * ICON_INSET : size;
        drawIcon(sprites, iconPath, cx, cy, iconSize, alphaMult);
        drawFrame(look, x, y, w, h, cx, cy, half, alphaMult);
        if (full) {
            drawFrontLayer(look, x, y, w, h, cx, cy, half, iconSize / 2f, t, phaseSeed, burstAge, alphaMult);
        }
    }

    private static void drawBackLayer(CargoIconLook look, float cx, float cy, float half, float t, boolean full, float alphaMult,
                                      float glowMult) {
        boolean animatedRings = full && (look.rings() || look.ticks());
        if (look.glow() <= 0f && !animatedRings) {
            return;
        }
        try (GlScope scope = GlScope.flat().additive()) {
            if (look.glow() > 0f) {
                float pulse = full ? pulse(look, t) : 1f;
                float glowAlpha = look.glow() * pulse * alphaMult * (1f + HOVER_GLOW_BOOST * glowMult);
                HoloShapes.radialGlow(cx, cy, half * GLOW_INNER, half * GLOW_OUTER, look.color(), glowAlpha);
            }
            if (full && look.rings()) {
                HoloShapes.arcSegments(cx, cy, half * RING_RADIUS, 6, 0.55f, t * RING_DEGREES_PER_SECOND, 1.5f, look.color(),
                        RING_ALPHA * alphaMult);
            }
            if (full && look.ticks()) {
                HoloShapes.ticks(cx, cy, half * TICK_RADIUS, 32, 4, 4f, 2f, -t * TICK_DEGREES_PER_SECOND, look.color(),
                        TICK_ALPHA * alphaMult);
            }
        }
    }

    static float pulse(CargoIconLook look, float t) {
        if (look.heartbeat()) {
            return 0.55f + 0.45f * heartbeat(t);
        }
        return 1f - BREATH_DEPTH * (0.5f + 0.5f * (float) Math.sin(t * BREATH_SPEED));
    }

    static float heartbeat(float t) {
        float cycle = t % HEARTBEAT_PERIOD;
        return Math.max(bump(cycle, 0.12f), 0.7f * bump(cycle, 0.42f));
    }

    private static float bump(float value, float centre) {
        float distance = (value - centre) / HEARTBEAT_WIDTH;
        return (float) Math.exp(-distance * distance);
    }

    private static void drawFrame(CargoIconLook look, float x, float y, float w, float h, float cx, float cy, float half, float alphaMult) {
        if (!look.framed() && look.badgeColor() == null) {
            return;
        }
        try (GlScope scope = GlScope.flat()) {
            if (look.framed()) {
                HoloShapes.brackets(cx, cy, half - 1.5f, half * BRACKET_ARM, look.color(), 0.85f * alphaMult);
            }
            if (look.badgeColor() != null) {
                HoloShapes.diamond(x + w - BADGE_MARGIN, y + h - BADGE_MARGIN, BADGE_SIZE + 1f, Color.BLACK, 0.7f * alphaMult);
                HoloShapes.diamond(x + w - BADGE_MARGIN, y + h - BADGE_MARGIN, BADGE_SIZE, look.badgeColor(), alphaMult);
            }
        }
    }

    private static void drawFrontLayer(CargoIconLook look, float x, float y, float w, float h, float cx, float cy, float half,
                                       float iconHalf, float t, int phaseSeed, float burstAge, float alphaMult) {
        boolean bursting = burstAge >= 0f && burstAge < BURST_SECONDS;
        if (!look.sparkles() && !look.arcs() && !look.prismatic() && !bursting) {
            return;
        }
        try (GlScope scope = GlScope.flat().additive()) {
            if (look.sparkles()) {
                drawSparkles(cx, cy, half, t, phaseSeed, alphaMult);
            }
            if (look.arcs()) {
                drawArcs(look.color(), cx, cy, half * ARC_INSET, t, phaseSeed, alphaMult);
            }
            if (look.prismatic()) {
                drawSweep(cx, cy, iconHalf, t, alphaMult);
            }
            if (bursting) {
                drawBurst(look, x, y, w, h, cx, cy, half, burstAge / BURST_SECONDS, alphaMult);
            }
        }
    }

    private static void drawSparkles(float cx, float cy, float half, float t, int phaseSeed, float alphaMult) {
        for (int slot = 0; slot < SPARKLE_SLOTS; slot++) {
            float local = t + slot * SPARKLE_PERIOD / SPARKLE_SLOTS;
            int cycle = (int) Math.floor(local / SPARKLE_PERIOD);
            float phase = (local - cycle * SPARKLE_PERIOD) / SPARKLE_LIFE;
            if (phase >= 1f) {
                continue;
            }
            float sparkX = cx + (hash(phaseSeed, slot * 2, cycle) * 2f - 1f) * half * SPARKLE_SPREAD;
            float sparkY = cy + (hash(phaseSeed, slot * 2 + 1, cycle) * 2f - 1f) * half * SPARKLE_SPREAD;
            float envelope = (float) Math.sin(phase * Math.PI);
            float arm = half * SPARKLE_ARM * envelope;
            float diagonal = arm * 0.4f;
            HoloShapes.line(sparkX - arm, sparkY, sparkX + arm, sparkY, 1.5f, SPARK_COLOR, envelope * alphaMult);
            HoloShapes.line(sparkX, sparkY - arm, sparkX, sparkY + arm, 1.5f, SPARK_COLOR, envelope * alphaMult);
            HoloShapes.line(sparkX - diagonal, sparkY - diagonal, sparkX + diagonal, sparkY + diagonal, 1f, SPARK_COLOR,
                    0.6f * envelope * alphaMult);
            HoloShapes.line(sparkX - diagonal, sparkY + diagonal, sparkX + diagonal, sparkY - diagonal, 1f, SPARK_COLOR,
                    0.6f * envelope * alphaMult);
        }
    }

    private static void drawArcs(Color color, float cx, float cy, float boxHalf, float t, int phaseSeed, float alphaMult) {
        for (int slot = 0; slot < ARC_SLOTS; slot++) {
            float local = t + slot * ARC_PERIOD * 0.5f + hash(phaseSeed, 7, slot) * ARC_PERIOD;
            int cycle = (int) Math.floor(local / ARC_PERIOD);
            float age = local - cycle * ARC_PERIOD;
            if (age >= ARC_LIFE) {
                continue;
            }
            float start = hash(phaseSeed, 11 + slot, cycle) * 4f;
            float span = 0.35f + hash(phaseSeed, 23 + slot, cycle) * 0.55f;
            int shape = (int) (age / ARC_RESHAPE_SECONDS);
            float fade = (1f - age / ARC_LIFE) * (0.6f + 0.4f * hash(phaseSeed, 31 + shape, cycle));
            drawArcStrip(cx, cy, boxHalf, start, span, phaseSeed, cycle * 97 + shape, 4f, color, 0.3f * fade * alphaMult);
            drawArcStrip(cx, cy, boxHalf, start, span, phaseSeed, cycle * 97 + shape, 1.5f, ARC_CORE_COLOR, 0.9f * fade * alphaMult);
        }
    }

    private static void drawArcStrip(float cx, float cy, float boxHalf, float start, float span, int phaseSeed, int shapeKey, float width,
                                     Color color, float alpha) {
        GL11.glLineWidth(width);
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        int last = ARC_POINTS - 1;
        for (int i = 0; i <= last; i++) {
            float position = start + span * i / last;
            float jitter = i == 0 || i == last ? 0f : (hash(phaseSeed, shapeKey, i) * 2f - 1f) * ARC_JITTER;
            int edge = edgeOf(position);
            float pointX = perimeterX(position) + jitter * (edge == 1 ? -1f : edge == 3 ? 1f : 0f);
            float pointY = perimeterY(position) + jitter * (edge == 0 ? 1f : edge == 2 ? -1f : 0f);
            GL11.glVertex2f(cx + pointX * boxHalf, cy + pointY * boxHalf);
        }
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }

    private static void drawSweep(float cx, float cy, float iconHalf, float t, float alphaMult) {
        float cycle = t % SWEEP_PERIOD;
        if (cycle >= SWEEP_SECONDS) {
            return;
        }
        float progress = cycle / SWEEP_SECONDS;
        float bandAlpha = SWEEP_ALPHA * (float) Math.sin(progress * Math.PI) * alphaMult;
        float centreOffset = -1.6f + 3.2f * progress;
        float bandHalf = iconHalf * SWEEP_BAND;
        float left = cx - iconHalf;
        float right = cx + iconHalf;
        GL11.glBegin(GL11.GL_QUADS);
        for (float rowBottom = cy - iconHalf; rowBottom < cy + iconHalf; rowBottom += SWEEP_ROW_PIXELS) {
            float rowTop = Math.min(rowBottom + SWEEP_ROW_PIXELS, cy + iconHalf);
            float rowFraction = (rowBottom - cy) / iconHalf;
            float bandCentre = cx + (centreOffset - rowFraction * SWEEP_SLANT) * iconHalf;
            int rgb = Color.HSBtoRGB(0.5f + rowFraction * 0.35f + t * 0.15f, 0.45f, 1f);
            float red = ((rgb >> 16) & 0xFF) / 255f;
            float green = ((rgb >> 8) & 0xFF) / 255f;
            float blue = (rgb & 0xFF) / 255f;
            sweepQuad(bandCentre - bandHalf, bandCentre, rowBottom, rowTop, left, right, bandCentre, bandHalf, red, green, blue, bandAlpha);
            sweepQuad(bandCentre, bandCentre + bandHalf, rowBottom, rowTop, left, right, bandCentre, bandHalf, red, green, blue, bandAlpha);
        }
        GL11.glEnd();
    }

    private static void sweepQuad(float fromX, float toX, float rowBottom, float rowTop, float left, float right, float bandCentre,
                                  float bandHalf, float red, float green, float blue, float bandAlpha) {
        float clampedFrom = Math.max(left, Math.min(right, fromX));
        float clampedTo = Math.max(left, Math.min(right, toX));
        if (clampedTo <= clampedFrom) {
            return;
        }
        float fromAlpha = bandAlpha * (1f - Math.abs(clampedFrom - bandCentre) / bandHalf);
        float toAlpha = bandAlpha * (1f - Math.abs(clampedTo - bandCentre) / bandHalf);
        GL11.glColor4f(red, green, blue, fromAlpha);
        GL11.glVertex2f(clampedFrom, rowBottom);
        GL11.glColor4f(red, green, blue, toAlpha);
        GL11.glVertex2f(clampedTo, rowBottom);
        GL11.glVertex2f(clampedTo, rowTop);
        GL11.glColor4f(red, green, blue, fromAlpha);
        GL11.glVertex2f(clampedFrom, rowTop);
    }

    private static void drawBurst(CargoIconLook look, float x, float y, float w, float h, float cx, float cy, float half, float progress,
                                  float alphaMult) {
        float remaining = 1f - progress;
        float flash = remaining * remaining;
        Misc.setColor(look.color(), 0.45f * flash * alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
        HoloShapes.radialGlow(cx, cy, 0f, half * (0.6f + 0.5f * progress), SPARK_COLOR, 0.7f * flash * alphaMult);
        HoloShapes.circle(cx, cy, half * (0.45f + 0.85f * progress), look.color(), 2.5f, remaining * alphaMult);
        if (look.arcs() && progress > 0.2f) {
            float late = (progress - 0.2f) / 0.8f;
            HoloShapes.circle(cx, cy, half * (0.45f + 0.85f * late), SPARK_COLOR, 1.5f, (1f - late) * alphaMult);
        }
    }

    static void drawIcon(SpriteCache sprites, String iconPath, float cx, float cy, float size, float alphaMult) {
        if (iconPath == null) {
            return;
        }
        SpriteAPI sprite = sprites.sprite(iconPath);
        if (sprite == null) {
            return;
        }
        float aspect = aspect(iconPath);
        float drawWidth = aspect >= 1f ? size : size * aspect;
        float drawHeight = aspect >= 1f ? size / aspect : size;
        SpriteDraw.drawAtCenter(sprite, cx, cy, drawWidth, drawHeight, null, alphaMult, 0f);
    }

    private static float aspect(String iconPath) {
        Float known = ASPECTS.get(iconPath);
        if (known != null) {
            return known;
        }
        SpriteAPI original = Global.getSettings().getSprite(iconPath);
        float measured = original == null || original.getHeight() <= 0f ? 1f : original.getWidth() / original.getHeight();
        ASPECTS.put(iconPath, measured);
        return measured;
    }

    static int edgeOf(float position) {
        float wrapped = position % 4f;
        if (wrapped < 0f) {
            wrapped += 4f;
        }
        return Math.min(3, (int) wrapped);
    }

    static float perimeterX(float position) {
        int edge = edgeOf(position);
        float along = position - (float) Math.floor(position);
        return switch (edge) {
            case 0 -> -1f + 2f * along;
            case 1 -> 1f;
            case 2 -> 1f - 2f * along;
            default -> -1f;
        };
    }

    static float perimeterY(float position) {
        int edge = edgeOf(position);
        float along = position - (float) Math.floor(position);
        return switch (edge) {
            case 0 -> -1f;
            case 1 -> -1f + 2f * along;
            case 2 -> 1f;
            default -> 1f - 2f * along;
        };
    }

    static float hash(int seed, int first, int second) {
        long mixed = seed * 0x9E3779B97F4A7C15L ^ first * 0xBF58476D1CE4E5B9L ^ second * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        mixed *= 0x7FB5D329728EA185L;
        mixed ^= mixed >>> 27;
        mixed *= 0x81DADEF4BC2DD44DL;
        mixed ^= mixed >>> 33;
        return (mixed >>> 40) / (float) (1 << 24);
    }
}
