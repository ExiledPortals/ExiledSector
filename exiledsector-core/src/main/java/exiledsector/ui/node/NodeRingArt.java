package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TexturedQuadBatch;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static exiledsector.ui.node.NodeMetrics.NODE_CONNECTOR_GLOW_HALO_ALPHA;
import static exiledsector.ui.node.NodeMetrics.NODE_CONNECTOR_GLOW_HALO_THICKNESS;
import static exiledsector.ui.node.NodeMetrics.NODE_CONNECTOR_GLOW_LINE_THICKNESS;
import static exiledsector.ui.node.NodeMetrics.NODE_CONNECTOR_LINE_THICKNESS;
import static exiledsector.ui.node.NodeMetrics.RING_DULL_ALPHA;
import static exiledsector.ui.node.NodeMetrics.RING_DULL_COLOR;
import static exiledsector.ui.node.NodeMetrics.donutGapRadius;
import static exiledsector.ui.node.NodeMetrics.donutRadius;

public final class NodeRingArt {

    public interface EdgeShape {

        float distance(float cos, float sin);
    }

    public record NotableLook(boolean lit, boolean breathing, Color ringTint, Color glowTint, Color accent) {
    }

    public static final int RING_SEGMENTS = 32;
    public static final float UNALLOCATED_ALPHA_MULT = 0.45f;
    private static final UnitCircle RING_CIRCLE = UnitCircle.of(RING_SEGMENTS);
    private static final float RING_LINE_THICKNESS = 1.5f;
    public static final float[] CIRCLE_OUTLINE_RADII = outlineRadii((cos, sin) -> 1f);

    private static final String[] RING_STACK_TEXTURES = {
            "graphics/fx/wormhole_ring_bright2.png",
            "graphics/fx/wormhole_ring_bright3.png"
    };

    private static final float RING_INSTANCE_MIN_ROTATION_SPEED_DEG = 20f;
    private static final float RING_INSTANCE_MAX_ROTATION_SPEED_DEG = 60f;
    private static final float RING_INSTANCE_JITTER_RATIO = 0.05f;
    private static final float RING_INSTANCE_BASE_ALPHA = 0.5f;
    private static final float RING_MIN_RADIUS_FRACTION = 0.62f;

    public static final Color RING_PINK_COLOR = new Color(255, 60, 220);
    private static final float RING_PINK_SCALE_RATIO = 0.85f;

    private static final String GLOW_TEXTURE_PATH = "graphics/fx/star_halo.png";
    public static final Color AMBIENT_GLOW_COLOR = new Color(255, 170, 255);
    private static final float AMBIENT_GLOW_ALPHA = 1f;
    private static final float AMBIENT_GLOW_SIZE_RATIO = 3.2f;

    private static final float NOTABLE_RING_OUTER_RADIUS_RATIO = 1.05f;
    private static final float NOTABLE_RING_RADIUS_DECAY = 0.88f;
    private static final int NOTABLE_RING_COUNT = 10;

    private static final float PULSE_DURATION = 0.5f;
    private static final float PULSE_START_RADIUS_FRACTION = 1f;
    private static final float PULSE_END_RADIUS_FRACTION = 2.2f;

    private static final float BREATHING_PERIOD_SECONDS = 2.2f;
    private static final float BREATHING_MIN_ALPHA = 0.35f;
    private static final float BREATHING_MAX_ALPHA = 1f;
    private static final int BREATHING_BRIGHTNESS_PASSES = 2;

    private static final String PINK_STACK_SUFFIX = "_pink";

    private final SpriteCache spriteCache = new SpriteCache(NodeRingArt.class);
    private final TexturedQuadBatch quads = new TexturedQuadBatch();
    private final Map<String, Float> pulseElapsed = new HashMap<>();
    private final Map<String, List<RingInstance>> ringStacks = new HashMap<>();
    private float breathingPhase = 0f;
    private float animationElapsedSeconds = 0f;

    public static float[] outlineRadii(EdgeShape shape) {
        float[] radii = new float[RING_SEGMENTS];
        for (int i = 0; i < RING_SEGMENTS; i++) {
            radii[i] = shape.distance(RING_CIRCLE.cos(i), RING_CIRCLE.sin(i));
        }
        return radii;
    }

    public void advance(float amount) {
        breathingPhase = (breathingPhase + amount) % BREATHING_PERIOD_SECONDS;
        animationElapsedSeconds += amount;

        if (!pulseElapsed.isEmpty()) {
            Iterator<Map.Entry<String, Float>> pulseEntries = pulseElapsed.entrySet().iterator();
            while (pulseEntries.hasNext()) {
                Map.Entry<String, Float> pulseEntry = pulseEntries.next();
                float pulseSeconds = pulseEntry.getValue() + amount;
                if (pulseSeconds >= PULSE_DURATION) {
                    pulseEntries.remove();
                } else {
                    pulseEntry.setValue(pulseSeconds);
                }
            }
        }
    }

    public float elapsedSeconds() {
        return animationElapsedSeconds;
    }

    public void startPulse(String nodeId) {
        pulseElapsed.put(nodeId, 0f);
    }

    public boolean isPulsing(String nodeId) {
        return pulseElapsed.containsKey(nodeId);
    }

    public void drawNotable(float cx, float cy, float footprintSize, float zoom, String nodeId, NotableLook look, float alphaMult) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float stateAlpha = look.lit() ? 1f : UNALLOCATED_ALPHA_MULT;
        drawNotableRings(cx, cy, footprintSize, nodeId, stateAlpha * alphaMult, look.ringTint());
        drawAmbientGlow(cx, cy, footprintSize, stateAlpha * alphaMult, look.glowTint());

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float ringRadius = donutRadius(footprintSize);
        drawDonut(cx, cy, ringRadius, 1f, zoom, look.lit(), look.accent(), alphaMult);
        if (look.breathing()) {
            drawBreathingOutline(cx, cy, ringRadius, NODE_CONNECTOR_GLOW_LINE_THICKNESS * zoom, CIRCLE_OUTLINE_RADII, look.accent(), alphaMult);
        }
        drawPulseOutline(cx, cy, footprintSize / 2f, nodeId, zoom, CIRCLE_OUTLINE_RADII, look.accent(), alphaMult);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public void drawNotableRings(float cx, float cy, float footprintSize, String nodeId, float alpha, Color tint) {
        float outerRadius = footprintSize * NOTABLE_RING_OUTER_RADIUS_RATIO;
        List<RingInstance> whiteRings = ringStack(nodeId, NOTABLE_RING_COUNT, NOTABLE_RING_RADIUS_DECAY);
        List<RingInstance> tintedRings = ringStack(nodeId + PINK_STACK_SUFFIX, NOTABLE_RING_COUNT, NOTABLE_RING_RADIUS_DECAY);
        for (int textureIndex = 0; textureIndex < RING_STACK_TEXTURES.length; textureIndex++) {
            SpriteAPI texture = spriteCache.texture(RING_STACK_TEXTURES[textureIndex]);
            if (texture == null) continue;
            try (TexturedQuadBatch batch = quads.beginAdditive(texture)) {
                addRingQuads(batch, textureIndex, cx, cy, outerRadius, whiteRings, Color.WHITE, 1f, alpha);
                addRingQuads(batch, textureIndex, cx, cy, outerRadius, tintedRings, tint, RING_PINK_SCALE_RATIO, alpha);
            }
        }
    }

    public void drawRingStack(float cx, float cy, float outerRadius, String stackKey, int ringCount, float radiusDecay, Color ringColor,
                              float stackAlpha) {
        List<RingInstance> instances = ringStack(stackKey, ringCount, radiusDecay);
        for (int textureIndex = 0; textureIndex < RING_STACK_TEXTURES.length; textureIndex++) {
            SpriteAPI texture = spriteCache.texture(RING_STACK_TEXTURES[textureIndex]);
            if (texture == null) continue;
            try (TexturedQuadBatch batch = quads.beginAdditive(texture)) {
                addRingQuads(batch, textureIndex, cx, cy, outerRadius, instances, ringColor, 1f, stackAlpha);
            }
        }
    }

    private List<RingInstance> ringStack(String stackKey, int ringCount, float radiusDecay) {
        return ringStacks.computeIfAbsent(stackKey, key -> generateRingInstances(key, ringCount, radiusDecay));
    }

    private void addRingQuads(TexturedQuadBatch batch, int textureIndex, float cx, float cy, float outerRadius, List<RingInstance> instances,
                              Color ringColor, float scaleRatio, float stackAlpha) {
        float instanceAlpha = RING_INSTANCE_BASE_ALPHA * stackAlpha;

        for (RingInstance instance : instances) {
            if (instance.textureIndex != textureIndex) continue;

            float instanceRadius = outerRadius * scaleRatio * instance.radiusFraction;
            float jitterMag = instanceRadius * RING_INSTANCE_JITTER_RATIO;

            float angle = instance.baseAngleDeg + animationElapsedSeconds * instance.rotationSpeedDeg;
            float wanderRad = instance.jitterPhase + animationElapsedSeconds * instance.jitterSpeed;
            float jx = (float) Math.cos(wanderRad) * jitterMag;
            float jy = (float) Math.sin(wanderRad) * jitterMag;
            float spriteSize = instanceRadius * 2f * instance.sizeJitter;

            batch.quadAtCenter(cx + jx, cy + jy, spriteSize, spriteSize, angle, ringColor, instanceAlpha);
        }
    }

    public void drawAmbientGlow(float cx, float cy, float footprintSize, float alpha, Color glowColor) {
        float size = footprintSize * AMBIENT_GLOW_SIZE_RATIO;
        SpriteDraw.drawAdditiveAtCenter(spriteCache, GLOW_TEXTURE_PATH, cx, cy, size, size, glowColor, AMBIENT_GLOW_ALPHA * alpha);
    }

    public void drawDonut(float cx, float cy, float radius, float scale, float zoom, boolean lit, Color accent, float alphaMult) {
        if (lit) {
            GL11.glLineWidth(NODE_CONNECTOR_GLOW_HALO_THICKNESS * scale * zoom);
            drawOutline(cx, cy, radius, CIRCLE_OUTLINE_RADII, accent, alphaMult * NODE_CONNECTOR_GLOW_HALO_ALPHA);
            GL11.glLineWidth(NODE_CONNECTOR_GLOW_LINE_THICKNESS * scale * zoom);
            drawOutline(cx, cy, radius, CIRCLE_OUTLINE_RADII, accent, alphaMult);
            return;
        }

        float gapRadius = donutGapRadius(scale, zoom);
        GL11.glLineWidth(NODE_CONNECTOR_LINE_THICKNESS * scale * zoom);
        drawOutline(cx, cy, radius - gapRadius, CIRCLE_OUTLINE_RADII, RING_DULL_COLOR, alphaMult * RING_DULL_ALPHA);
        drawOutline(cx, cy, radius + gapRadius, CIRCLE_OUTLINE_RADII, RING_DULL_COLOR, alphaMult * RING_DULL_ALPHA);
    }

    public void drawBreathingOutline(float cx, float cy, float ringRadius, float lineWidth, float[] outlineRadii, Color accent, float alphaMult) {
        float breathingT = (float) (0.5 + 0.5 * Math.sin(2 * Math.PI * breathingPhase / BREATHING_PERIOD_SECONDS));
        float breathingAlpha = (BREATHING_MIN_ALPHA + (BREATHING_MAX_ALPHA - BREATHING_MIN_ALPHA) * breathingT) * alphaMult;
        GL11.glLineWidth(lineWidth);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        for (int pass = 0; pass < BREATHING_BRIGHTNESS_PASSES; pass++) {
            drawOutline(cx, cy, ringRadius, outlineRadii, accent, breathingAlpha);
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void drawPulseOutline(float cx, float cy, float halfSize, String nodeId, float zoom, float[] outlineRadii, Color accent, float alphaMult) {
        Float pulseSeconds = pulseElapsed.get(nodeId);
        if (pulseSeconds == null) return;

        GL11.glLineWidth(RING_LINE_THICKNESS * zoom);
        float progress = pulseSeconds / PULSE_DURATION;
        float radiusFraction = PULSE_START_RADIUS_FRACTION + (PULSE_END_RADIUS_FRACTION - PULSE_START_RADIUS_FRACTION) * progress;
        drawOutline(cx, cy, halfSize * radiusFraction, outlineRadii, accent, (1f - progress) * alphaMult);
    }

    public static void drawOutline(float cx, float cy, float radius, float[] outlineRadii, Color color, float alpha) {
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float vertexRadius = radius * outlineRadii[i];
            GL11.glVertex2f(cx + RING_CIRCLE.cos(i) * vertexRadius, cy + RING_CIRCLE.sin(i) * vertexRadius);
        }
        GL11.glEnd();
    }

    private static List<RingInstance> generateRingInstances(String seedKey, int count, float radiusDecay) {
        Random random = new Random(seedKey.hashCode());
        List<RingInstance> instances = new ArrayList<>(count);
        float rawMin = (float) Math.pow(radiusDecay, count - 1.0);
        float rawRange = 1f - rawMin;
        for (int i = 0; i < count; i++) {
            RingInstance instance = new RingInstance();
            instance.baseAngleDeg = random.nextFloat() * 360f;
            float speed = RING_INSTANCE_MIN_ROTATION_SPEED_DEG
                    + random.nextFloat() * (RING_INSTANCE_MAX_ROTATION_SPEED_DEG - RING_INSTANCE_MIN_ROTATION_SPEED_DEG);
            instance.rotationSpeedDeg = random.nextBoolean() ? speed : -speed;
            float raw = (float) Math.pow(radiusDecay, i);
            float t = rawRange > 0.0001f ? (raw - rawMin) / rawRange : 1f;
            instance.radiusFraction = RING_MIN_RADIUS_FRACTION + (1f - RING_MIN_RADIUS_FRACTION) * t;
            instance.jitterPhase = random.nextFloat() * (float) (Math.PI * 2);
            instance.jitterSpeed = 0.5f + random.nextFloat();
            instance.textureIndex = i % RING_STACK_TEXTURES.length;
            instance.sizeJitter = 0.9f + random.nextFloat() * 0.2f;
            instances.add(instance);
        }
        return instances;
    }

    private static final class RingInstance {
        float baseAngleDeg;
        float rotationSpeedDeg;
        float radiusFraction;
        float jitterPhase;
        float jitterSpeed;
        int textureIndex;
        float sizeJitter;
    }
}
