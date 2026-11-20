package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.tree.AtlasBodyArt.Field;
import exiledsector.charts.tree.AtlasBodyArt.Flare;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import exiledsector.ui.atlas.AtlasBodyPainter.Placement;
import exiledsector.ui.belt.AuroraBeltRenderer;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

final class AtlasFieldPainter {

    private static final String DOT_PATH = "graphics/fx/hit_glow.png";
    private static final String POLE_PATH = AtlasSystemArt.GLOW;
    private static final Color AURORA_COLOUR = AtlasSystemArt.AURORA_COLOUR;
    private static final Color AURORA_SHIFT_COLOUR = AtlasSystemArt.AURORA_SHIFT_COLOUR;
    private static final Color SPARK_COLOUR = new Color(220, 255, 250);
    private static final float AURORA_SHIFT_RATE = 0.7f;
    private static final float AURORA_WAVE_RATE = 1.4f;
    private static final float AXIS_SEED_TURN = 2f;
    private static final float AXIS_WOBBLE_DEGREES = 6f;
    private static final float AXIS_WOBBLE_RATE = 0.15f;
    private static final float LOOP_BREATH = 0.04f;
    private static final float LOOP_BREATH_RATE = 0.9f;
    private static final float DOT_SIZE = 0.11f;
    private static final float DOT_PULSE_GROWTH = 1.2f;
    private static final float DOT_BASE_ALPHA = 0.16f;
    private static final float DOT_PULSE_ALPHA = 0.75f;
    private static final float POLE_SIZE = 0.75f;
    private static final float POLE_BASE_ALPHA = 0.25f;
    private static final float POLE_ARRIVAL_ALPHA = 0.55f;
    private static final float POLE_SURGE_ALPHA = 0.6f;
    private static final float SPARK_SLOT_SECONDS = 0.6f;
    private static final float SPARK_CHANCE_MULT = 0.5f;
    private static final float SPARK_SIZE = 0.4f;
    private static final int SPARK_SALT = 23;

    private final SpriteCache sprites = new SpriteCache(AtlasFieldPainter.class);
    private final Vector2f bandCentre = new Vector2f();
    private MagneticWeather.Style style;
    private Color lineColour;
    private float glowAlpha;
    private float fieldSeconds;
    private float realSeconds;
    private float flashChance;
    private int seed;
    private float centreX;
    private float centreY;
    private float radius;
    private float axisCos;
    private float axisSin;
    private float surge;

    void paint(Field field, Flare flare, Color colour, Placement placement, Lighting lighting, float seconds, float alpha) {
        if (field == Field.NONE || alpha <= 0f) {
            return;
        }
        style = MagneticWeather.styleOf(field);
        lineColour = AtlasBodyPainter.tintFor(lighting, colour);
        glowAlpha = alpha * FlareWeather.glowShare(lighting);
        fieldSeconds = placement.stormSeconds();
        realSeconds = seconds;
        flashChance = StormWeather.flashChance(lighting);
        seed = (int) placement.seedDegrees();
        centreX = placement.screenX();
        centreY = placement.screenY();
        radius = placement.screenRadius();
        surge = surgeOf(flare);
        double axis = Math.toRadians(seed * AXIS_SEED_TURN + AXIS_WOBBLE_DEGREES * Math.sin(fieldSeconds * AXIS_WOBBLE_RATE + seed));
        axisCos = (float) Math.cos(axis);
        axisSin = (float) Math.sin(axis);
        paintAurora(lighting);
        paintLoops();
        if (style.poleGlows()) {
            paintPoles(lighting);
        }
    }

    private float surgeOf(Flare flare) {
        return switch (style.surge()) {
            case STORM -> MagneticWeather.stormSurge(seed, realSeconds, flashChance);
            case FLARE -> MagneticWeather.flareSurge(seed, fieldSeconds, FlareWeather.styleOf(flare));
            case NONE -> 0f;
        };
    }

    private void paintAurora(Lighting lighting) {
        SpriteAPI aurora = sprites.texture(AtlasSystemArt.AURORA_TEXTURE);
        if (aurora == null) {
            return;
        }
        float shift = Fade.pulse(fieldSeconds * AURORA_SHIFT_RATE + seed);
        Color auroraColour = AtlasBodyPainter.tintFor(lighting, Misc.interpolateColor(AURORA_COLOUR, AURORA_SHIFT_COLOUR, shift));
        float auroraAlpha = Math.min(1f, style.auroraAlpha() * MagneticWeather.shimmer(seed, fieldSeconds) + surge * (1f - style.auroraAlpha()));
        float outer = style.auroraOuter() + (style.auroraOuter() - style.auroraInner()) * surge * MagneticWeather.SURGE_AURORA_SWELL;
        bandCentre.set(centreX, centreY);
        AuroraBeltRenderer.render(aurora, new RadialBand(bandCentre, radius * style.auroraInner(), radius * outer), auroraColour,
                glowAlpha * auroraAlpha, fieldSeconds * AURORA_WAVE_RATE, 1f);
    }

    private void paintLoops() {
        float swell = 1f + surge * MagneticWeather.SURGE_LOOP_SWELL;
        float dotSize = radius * DOT_SIZE;
        for (int loop = 0; loop < style.loopsPerSide(); loop++) {
            float breath = 1f + LOOP_BREATH * (float) Math.sin(fieldSeconds * LOOP_BREATH_RATE + loop * 1.3f + seed);
            float reach = MagneticWeather.loopReach(style, loop) * breath * swell;
            for (int side = 0; side < 2; side++) {
                int loopSeed = seed + loop * 31 + side * 7;
                float direction = side == 0 ? 1f : -1f;
                int sparkSlot = StormWeather.slotOf(loopSeed, realSeconds, SPARK_SLOT_SECONDS);
                float spark = style.sparks() ? StormWeather.flash(loopSeed, realSeconds, SPARK_SLOT_SECONDS, flashChance * SPARK_CHANCE_MULT) : 0f;
                int sparkDot = spark > 0f ? StormWeather.pick(loopSeed, sparkSlot, MagneticWeather.DOTS_PER_LOOP) : -1;
                for (int dot = 0; dot < MagneticWeather.DOTS_PER_LOOP; dot++) {
                    float along = MagneticWeather.dotAlong(dot);
                    float flowAlong = side == 0 ? along : 1f - along;
                    float pulse = MagneticWeather.flowGlow(flowAlong, style.flowSeconds(), fieldSeconds, loopSeed);
                    float across = MagneticWeather.loopAcross(reach, along) * direction * radius;
                    float axial = MagneticWeather.loopAxial(reach, along) * radius;
                    float dotX = centreX + across * axisCos - axial * axisSin;
                    float dotY = centreY + across * axisSin + axial * axisCos;
                    float size = dotSize * (1f + pulse * DOT_PULSE_GROWTH);
                    float dotAlpha = DOT_BASE_ALPHA + pulse * DOT_PULSE_ALPHA + surge * DOT_BASE_ALPHA;
                    SpriteDraw.drawAdditiveAtCenter(sprites, DOT_PATH, dotX, dotY, size, size, lineColour, glowAlpha * Math.min(1f, dotAlpha));
                    if (dot == sparkDot) {
                        float sparkSize = radius * SPARK_SIZE;
                        SpriteDraw.drawAdditiveAtCenter(sprites, DOT_PATH, dotX, dotY, sparkSize, sparkSize, SPARK_COLOUR, glowAlpha * spark);
                    }
                }
            }
        }
    }

    private void paintPoles(Lighting lighting) {
        Color poleColour = AtlasBodyPainter.tintFor(lighting, AURORA_COLOUR);
        float size = radius * POLE_SIZE;
        for (int pole = 0; pole < 2; pole++) {
            float direction = pole == 0 ? 1f : -1f;
            float arrival = 0f;
            for (int loop = 0; loop < style.loopsPerSide(); loop++) {
                int arrivingSide = pole == 0 ? 1 : 0;
                arrival = Math.max(arrival, MagneticWeather.flowGlow(1f, style.flowSeconds(), fieldSeconds, seed + loop * 31 + arrivingSide * 7));
            }
            float poleAlpha = POLE_BASE_ALPHA * MagneticWeather.shimmer(seed + pole, fieldSeconds) + arrival * POLE_ARRIVAL_ALPHA
                    + surge * POLE_SURGE_ALPHA;
            float poleX = centreX - axisSin * radius * direction;
            float poleY = centreY + axisCos * radius * direction;
            SpriteDraw.drawAdditiveAtCenter(sprites, POLE_PATH, poleX, poleY, size, size, poleColour, glowAlpha * Math.min(1f, poleAlpha));
        }
    }
}
