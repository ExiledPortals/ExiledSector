package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.HoloShapes;
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

public final class NodeRingArt {

    public interface EdgeShape {

        float distance(float cos, float sin);
    }

    public record NotableLook(boolean lit, boolean breathing, Color ringTint, Color glowTint, Color accent) {
    }

    public static final int RING_SEGMENTS = 32;
    public static final float UNALLOCATED_ALPHA_MULT = 0.45f;
    private static final UnitCircle RING_CIRCLE = UnitCircle.of(RING_SEGMENTS);

    private static final String[] RING_STACK_TEXTURES = {
            "graphics/fx/wormhole_ring_bright2.png",
            "graphics/fx/wormhole_ring_bright3.png"
    };

    private static final float RING_INSTANCE_MIN_ROTATION_SPEED_DEG = 20f;
    private static final float RING_INSTANCE_MAX_ROTATION_SPEED_DEG = 60f;
    private static final float RING_INSTANCE_JITTER_RATIO = 0.05f;
    private static final float RING_INSTANCE_BASE_ALPHA = 0.5f;
    private static final float RING_MIN_RADIUS_FRACTION = 0.62f;
    private static final float RING_MIN_SIZE_JITTER = 0.9f;
    private static final float RING_SIZE_JITTER_RANGE = 0.2f;
    private static final float RING_TEXTURE_INNER_EDGE = 0.85f;
    private static final float PORTAL_GLOW_REACH = 1.8f;
    private static final float PORTAL_GLOW_ALPHA = 0.5f;

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

    private static final String PINK_STACK_SUFFIX = "_pink";

    private final SpriteCache spriteCache = new SpriteCache(NodeRingArt.class);
    private final TexturedQuadBatch quads = new TexturedQuadBatch();
    private final Map<String, Float> pulseElapsed = new HashMap<>();
    private final Map<String, List<RingInstance>> ringStacks = new HashMap<>();
    private float animationElapsedSeconds = 0f;

    public static float[] outlineRadii(EdgeShape shape) {
        float[] radii = new float[RING_SEGMENTS];
        for (int i = 0; i < RING_SEGMENTS; i++) {
            radii[i] = shape.distance(RING_CIRCLE.cos(i), RING_CIRCLE.sin(i));
        }
        return radii;
    }

    public void advance(float amount) {
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

    public float pulseProgress(String nodeId) {
        Float pulseSeconds = pulseElapsed.get(nodeId);
        return pulseSeconds == null ? -1f : Math.min(1f, pulseSeconds / PULSE_DURATION);
    }

    public static float pulseRadiusFraction(float progress) {
        return PULSE_START_RADIUS_FRACTION + (PULSE_END_RADIUS_FRACTION - PULSE_START_RADIUS_FRACTION) * progress;
    }

    public float breathing() {
        double phase = animationElapsedSeconds * 2.0 * Math.PI / BREATHING_PERIOD_SECONDS;
        return BREATHING_MIN_ALPHA + (BREATHING_MAX_ALPHA - BREATHING_MIN_ALPHA) * (float) (0.5 + 0.5 * Math.sin(phase));
    }

    public boolean isPulsing(String nodeId) {
        return pulseElapsed.containsKey(nodeId);
    }

    public void drawPortalRings(float cx, float cy, float innerRadius, String nodeId, NotableLook look, float alphaMult) {
        GL11.glEnable(GL11.GL_BLEND);
        float stateAlpha = look.lit() ? 1f : UNALLOCATED_ALPHA_MULT;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        HoloShapes.radialGlow(cx, cy, innerRadius, innerRadius * PORTAL_GLOW_REACH, look.glowTint(), PORTAL_GLOW_ALPHA * stateAlpha * alphaMult);
        drawNotableRings(cx, cy, portalRingSize(innerRadius), nodeId, stateAlpha * alphaMult, look.ringTint());
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static float portalRingSize(float innerRadius) {
        return innerRadius / (NOTABLE_RING_OUTER_RADIUS_RATIO * RING_PINK_SCALE_RATIO * RING_MIN_RADIUS_FRACTION * RING_MIN_SIZE_JITTER
                * RING_TEXTURE_INNER_EDGE);
    }

    public static float portalReach(float innerRadius) {
        return portalRingSize(innerRadius) * NOTABLE_RING_OUTER_RADIUS_RATIO * (RING_MIN_SIZE_JITTER + RING_SIZE_JITTER_RANGE)
                * (1f + RING_INSTANCE_JITTER_RATIO);
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
            instance.sizeJitter = RING_MIN_SIZE_JITTER + random.nextFloat() * RING_SIZE_JITTER_RANGE;
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
