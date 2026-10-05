package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SmoothZoom;
import exiledsector.ui.belt.AuroraBeltRenderer;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledsector.ui.belt.WormholeBandRenderer;
import exiledsector.ui.util.ColorUtil;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.UnitCircle;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_CONNECTOR_GLOW_HALO_ALPHA;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_CONNECTOR_GLOW_HALO_THICKNESS;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_CONNECTOR_GLOW_LINE_THICKNESS;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_CONNECTOR_LINE_THICKNESS;
import static exiledsector.ui.node.SkillTreeNodeGeometry.RING_DULL_ALPHA;
import static exiledsector.ui.node.SkillTreeNodeGeometry.RING_DULL_COLOR;
import static exiledsector.ui.node.SkillTreeNodeGeometry.beltInnerRadius;
import static exiledsector.ui.node.SkillTreeNodeGeometry.beltOuterRadius;
import static exiledsector.ui.node.SkillTreeNodeGeometry.donutGapRadius;
import static exiledsector.ui.node.SkillTreeNodeGeometry.donutRadius;

final class SkillTreeNodeRingRenderer {

    private static final int RING_SEGMENTS = 32;
    private static final UnitCircle RING_CIRCLE = UnitCircle.of(RING_SEGMENTS);
    private static final float RING_LINE_THICKNESS = 1.5f;

    private static final String[] RING_STACK_TEXTURES = {
            "graphics/fx/wormhole_ring_bright2.png",
            "graphics/fx/wormhole_ring_bright3.png"
    };

    private static final float RING_INSTANCE_MIN_ROTATION_SPEED_DEG = 20f;
    private static final float RING_INSTANCE_MAX_ROTATION_SPEED_DEG = 60f;
    private static final float RING_INSTANCE_JITTER_RATIO = 0.05f;
    private static final float RING_INSTANCE_BASE_ALPHA = 0.5f;
    private static final float RING_MIN_RADIUS_FRACTION = 0.62f;
    private static final float UNALLOCATED_ALPHA_MULT = 0.45f;

    private static final Color RING_PINK_COLOR = new Color(255, 60, 220);
    private static final float RING_PINK_SCALE_RATIO = 0.85f;

    private static final String GLOW_TEXTURE_PATH = "graphics/fx/star_halo.png";
    private static final Color AMBIENT_GLOW_COLOR = new Color(255, 170, 255);
    private static final float AMBIENT_GLOW_ALPHA = 1f;
    private static final float AMBIENT_GLOW_SIZE_RATIO = 3.2f;

    private static final float NOTABLE_RING_OUTER_RADIUS_RATIO = 1.05f;
    private static final float NOTABLE_RING_RADIUS_DECAY = 0.88f;
    private static final int NOTABLE_RING_COUNT = 10;

    private static final String DEFAULT_KEYSTONE_RING_BELT_PATH = "graphics/planets/aurorae.png";
    private static final float KEYSTONE_BELT_WIDTH_RATIO = 1.1f;

    private static final String AURORA_TEXTURE_PATH = "graphics/planets/aurorae.png";
    private static final Color DEFAULT_AURORA_COLOR = new Color(140, 120, 255);

    private static final float PULSE_DURATION = 0.5f;
    private static final float PULSE_START_RADIUS_FRACTION = 1f;
    private static final float PULSE_END_RADIUS_FRACTION = 2.2f;

    private static final float BREATHING_PERIOD_SECONDS = 2.2f;
    private static final float BREATHING_MIN_ALPHA = 0.35f;
    private static final float BREATHING_MAX_ALPHA = 1f;
    private static final int BREATHING_BRIGHTNESS_PASSES = 2;

    private static final float WORMHOLE_MIN_SCALE = 0.25f;

    private static final String WORMHOLE_CORONA_TEXTURE_PATH = "graphics/fx/wormhole_corona.png";
    private static final float WORMHOLE_CORONA_SIZE_RATIO = 2.2f;
    private static final int WORMHOLE_CORONA_COUNT = 6;
    private static final float WORMHOLE_CORONA_ORBIT_RATIO = 0.12f;
    private static final float WORMHOLE_CORONA_ROTATION_SPEED_DEG = 12f;
    private static final float WORMHOLE_CORONA_PULSE_SPEED_DEG = 90f;
    private static final float WORMHOLE_CORONA_PULSE_SIZE_RATIO = 0.1f;

    private static final float WORMHOLE_RING_OUTER_RADIUS_RATIO = 1.55f;
    private static final int WORMHOLE_RING_COUNT = 16;
    private static final float WORMHOLE_RING_RADIUS_DECAY = 0.94f;

    private static final String WORMHOLE_BAND_TEXTURE_PATH = "graphics/fx/portal_textures_small.png";
    private static final float WORMHOLE_BAND_INNER_RADIUS_RATIO = 0.45f;
    private static final float WORMHOLE_BAND_THICKNESS_RATIO = 0.35f;
    private static final float WORMHOLE_BAND_ROTATION_SPEED_DEG = 5f;
    private static final float WORMHOLE_BAND_ALPHA = 0.75f;

    private static final String WORMHOLE_GLOW_TEXTURE_PATH = "graphics/fx/hit_glow.png";
    private static final float WORMHOLE_GLOW_SIZE_RATIO = 1.3f;
    private static final float WORMHOLE_GLOW_ALPHA = 0.67f;

    private static final float REACH_FOOTPRINT_RATIO = 2f;
    private static final float KEYSTONE_BELT_REACH_MARGIN = 1.3f;

    private static final Function<String, List<RingInstance>> NOTABLE_RINGS =
            id -> generateRingInstances(id, NOTABLE_RING_COUNT, NOTABLE_RING_RADIUS_DECAY);
    private static final Function<String, List<RingInstance>> NOTABLE_PINK_RINGS =
            id -> generateRingInstances(id + "_pink", NOTABLE_RING_COUNT, NOTABLE_RING_RADIUS_DECAY);
    private static final Function<String, List<RingInstance>> WORMHOLE_RINGS =
            id -> generateRingInstances(id, WORMHOLE_RING_COUNT, WORMHOLE_RING_RADIUS_DECAY);

    private final SkillTreePanelStyle style;
    private final SpriteCache spriteCache = new SpriteCache(SkillTreeNodeRingRenderer.class);
    private final Map<String, Float> pulseElapsed = new HashMap<>();
    private final Map<String, List<RingInstance>> ringStacks = new HashMap<>();
    private final Map<String, List<RingInstance>> pinkRingStacks = new HashMap<>();
    private final WormholeOpenness wormholeOpenness;
    private final Map<String, Color> wormholeColors = new HashMap<>();
    private final Map<String, Color> ringBeltColors = new HashMap<>();
    private float breathingPhase = 0f;
    private float elapsedSeconds = 0f;

    SkillTreeNodeRingRenderer(SkillTreePanelStyle style, WormholeOpenness wormholeOpenness) {
        this.style = style;
        this.wormholeOpenness = wormholeOpenness;
    }

    void advance(float amount) {
        breathingPhase = (breathingPhase + amount) % BREATHING_PERIOD_SECONDS;
        elapsedSeconds += amount;

        if (!pulseElapsed.isEmpty()) {
            Iterator<Map.Entry<String, Float>> it = pulseElapsed.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Float> entry = it.next();
                float elapsed = entry.getValue() + amount;
                if (elapsed >= PULSE_DURATION) {
                    it.remove();
                } else {
                    entry.setValue(elapsed);
                }
            }
        }
    }

    void startPulse(String nodeId) {
        pulseElapsed.put(nodeId, 0f);
    }

    float reach(float footprintSize, SkillNode node) {
        float reach = footprintSize * REACH_FOOTPRINT_RATIO;
        if (node.getType().getTier() == SkillTier.KEYSTONE) {
            reach = Math.max(reach, beltOuterRadius(footprintSize, resolveRingBeltWidth(node)) * KEYSTONE_BELT_REACH_MARGIN);
        }
        return reach;
    }

    void draw(float cx, float cy, float footprintSize, float alphaMult, RingState state, float zoom, SkillNode node) {
        boolean allocated = state.allocated;
        SkillTier tier = node.getType().getTier();
        String nodeId = node.getId();
        float half = footprintSize / 2f;
        float scale = tier.getSizeMultiplier();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float ringRadius = donutRadius(footprintSize);
        if (tier == SkillTier.NOTABLE) {
            float stateAlpha = allocated ? 1f : UNALLOCATED_ALPHA_MULT;
            drawNotableRingStack(cx, cy, footprintSize * NOTABLE_RING_OUTER_RADIUS_RATIO, nodeId, stateAlpha * alphaMult);
            drawAmbientGlow(cx, cy, footprintSize, stateAlpha, alphaMult);
        } else if (tier == SkillTier.KEYSTONE) {
            float stateAlpha = allocated ? 1f : UNALLOCATED_ALPHA_MULT;
            String ringBeltPath = resolveRingBeltPath(node);
            float ringBeltWidth = resolveRingBeltWidth(node);
            if (AURORA_TEXTURE_PATH.equals(ringBeltPath)) {
                drawKeystoneAuroraBelt(cx, cy, footprintSize, ringBeltWidth, resolveRingBeltColor(node), stateAlpha, alphaMult, zoom);
            } else {
                drawKeystoneRingBelt(cx, cy, footprintSize, ringBeltWidth, ringBeltPath, stateAlpha, alphaMult);
            }
        } else if (tier == SkillTier.WORMHOLE) {
            drawWormhole(cx, cy, footprintSize, alphaMult, nodeId, resolveWormholeColor(node));
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        if (tier != SkillTier.WORMHOLE && tier != SkillTier.SOCKET) {
            drawNodeDonut(cx, cy, ringRadius, scale, zoom, allocated, alphaMult);
            if (state.breathing) {
                drawBreathingOutline(cx, cy, ringRadius, scale, zoom, alphaMult);
            }
            drawPulseOutline(cx, cy, half, nodeId, zoom, alphaMult);
        }

        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawBreathingOutline(float cx, float cy, float ringRadius, float scale, float zoom, float alphaMult) {
        float breathingT = (float) (0.5 + 0.5 * Math.sin(2 * Math.PI * breathingPhase / BREATHING_PERIOD_SECONDS));
        float breathingAlpha = (BREATHING_MIN_ALPHA + (BREATHING_MAX_ALPHA - BREATHING_MIN_ALPHA) * breathingT) * alphaMult;
        GL11.glLineWidth(NODE_CONNECTOR_GLOW_LINE_THICKNESS * scale * zoom);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        for (int pass = 0; pass < BREATHING_BRIGHTNESS_PASSES; pass++) {
            drawRingOutline(cx, cy, ringRadius, style.getAccentColor(), breathingAlpha);
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void drawPulseOutline(float cx, float cy, float half, String nodeId, float zoom, float alphaMult) {
        Float pulseSeconds = pulseElapsed.get(nodeId);
        if (pulseSeconds == null) return;

        GL11.glLineWidth(RING_LINE_THICKNESS * zoom);
        float progress = pulseSeconds / PULSE_DURATION;
        float radiusFraction = PULSE_START_RADIUS_FRACTION + (PULSE_END_RADIUS_FRACTION - PULSE_START_RADIUS_FRACTION) * progress;
        drawRingOutline(cx, cy, half * radiusFraction, style.getAccentColor(), (1f - progress) * alphaMult);
    }

    private void drawNodeDonut(float cx, float cy, float radius, float scale, float zoom, boolean allocated, float alphaMult) {
        if (allocated) {
            GL11.glLineWidth(NODE_CONNECTOR_GLOW_HALO_THICKNESS * scale * zoom);
            drawRingOutline(cx, cy, radius, style.getAccentColor(), alphaMult * NODE_CONNECTOR_GLOW_HALO_ALPHA);
            GL11.glLineWidth(NODE_CONNECTOR_GLOW_LINE_THICKNESS * scale * zoom);
            drawRingOutline(cx, cy, radius, style.getAccentColor(), alphaMult);
            return;
        }

        float gapRadius = donutGapRadius(scale, zoom);
        GL11.glLineWidth(NODE_CONNECTOR_LINE_THICKNESS * scale * zoom);
        drawRingOutline(cx, cy, radius - gapRadius, RING_DULL_COLOR, alphaMult * RING_DULL_ALPHA);
        drawRingOutline(cx, cy, radius + gapRadius, RING_DULL_COLOR, alphaMult * RING_DULL_ALPHA);
    }

    private void drawNotableRingStack(float cx, float cy, float outerRadius, String nodeId, float alpha) {
        drawRingStackPass(cx, cy, outerRadius, ringStacks.computeIfAbsent(nodeId, NOTABLE_RINGS), Color.WHITE, 1f, alpha);
        drawRingStackPass(cx, cy, outerRadius, pinkRingStacks.computeIfAbsent(nodeId, NOTABLE_PINK_RINGS),
                RING_PINK_COLOR, RING_PINK_SCALE_RATIO, alpha);
    }

    private void drawRingStackPass(float cx, float cy, float outerRadius, List<RingInstance> instances,
                                    Color color, float scaleRatio, float stackAlpha) {
        float alpha = RING_INSTANCE_BASE_ALPHA * stackAlpha;

        for (RingInstance instance : instances) {
            String path = RING_STACK_TEXTURES[instance.textureIndex];

            float radius = outerRadius * scaleRatio * instance.radiusFraction;
            float jitterMag = radius * RING_INSTANCE_JITTER_RATIO;

            float angle = instance.baseAngleDeg + elapsedSeconds * instance.rotationSpeedDeg;
            float wanderRad = instance.jitterPhase + elapsedSeconds * instance.jitterSpeed;
            float jx = (float) Math.cos(wanderRad) * jitterMag;
            float jy = (float) Math.sin(wanderRad) * jitterMag;
            float size = radius * 2f * instance.sizeJitter;

            SpriteDraw.drawAdditiveAtCenter(spriteCache, path, cx + jx, cy + jy, size, size, color, alpha, angle);
        }
    }

    private void drawAmbientGlow(float cx, float cy, float footprintSize, float stateAlpha, float alphaMult) {
        float size = footprintSize * AMBIENT_GLOW_SIZE_RATIO;

        SpriteDraw.drawAdditiveAtCenter(spriteCache, GLOW_TEXTURE_PATH, cx, cy, size, size,
                AMBIENT_GLOW_COLOR, AMBIENT_GLOW_ALPHA * stateAlpha * alphaMult);
    }

    private void drawKeystoneRingBelt(float cx, float cy, float footprintSize, float widthRatio, String ringArtPath, float stateAlpha, float alphaMult) {
        SpriteAPI sprite = spriteCache.texture(ringArtPath);
        if (sprite == null) return;
        RadialBand band = new RadialBand(new Vector2f(cx, cy), beltInnerRadius(footprintSize), beltOuterRadius(footprintSize, widthRatio));
        RingBeltRenderer.render(sprite, band, Color.WHITE, stateAlpha * alphaMult);
    }

    private void drawKeystoneAuroraBelt(float cx, float cy, float footprintSize, float widthRatio, Color tint, float stateAlpha, float alphaMult,
                                        float zoom) {
        SpriteAPI sprite = spriteCache.texture(AURORA_TEXTURE_PATH);
        if (sprite == null) return;
        AuroraBeltRenderer.render(sprite,
                new RadialBand(new Vector2f(cx, cy), beltInnerRadius(footprintSize), beltOuterRadius(footprintSize, widthRatio)),
                tint, stateAlpha * alphaMult, elapsedSeconds, SmoothZoom.MAX_ZOOM / zoom);
    }

    private void drawWormhole(float cx, float cy, float footprintSize, float alphaMult, String nodeId, Color color) {
        float rawOpenness = wormholeOpenness.of(nodeId);
        float openness = rawOpenness * rawOpenness;
        float visualScale = WORMHOLE_MIN_SCALE + (1f - WORMHOLE_MIN_SCALE) * openness;
        float baseRadius = footprintSize / 2f * visualScale;

        drawWormholeCorona(cx, cy, baseRadius, color, alphaMult);

        if (openness < 1f) {
            drawWormholeBands(cx, cy, baseRadius, color, (1f - openness) * WORMHOLE_BAND_ALPHA * alphaMult);
        }

        if (openness > 0f) {
            drawRingStackPass(cx, cy, baseRadius * 2f * WORMHOLE_RING_OUTER_RADIUS_RATIO,
                    ringStacks.computeIfAbsent(nodeId, WORMHOLE_RINGS),
                    color, 1f, openness * alphaMult);
        }

        if (openness < 1f) {
            drawWormholeGlow(cx, cy, baseRadius, color, alphaMult, openness);
        }
    }

    private void drawWormholeCorona(float cx, float cy, float baseRadius, Color color, float alphaMult) {
        float size = baseRadius * 2f * WORMHOLE_CORONA_SIZE_RATIO;
        float orbit = baseRadius * WORMHOLE_CORONA_ORBIT_RATIO;

        for (int i = 0; i < WORMHOLE_CORONA_COUNT; i++) {
            float baseAngle = 360f * i / WORMHOLE_CORONA_COUNT;
            float angle = baseAngle + elapsedSeconds * WORMHOLE_CORONA_ROTATION_SPEED_DEG;
            float rad = (float) Math.toRadians(angle);
            float pulse = 1f + (float) Math.sin(Math.toRadians(elapsedSeconds * WORMHOLE_CORONA_PULSE_SPEED_DEG + baseAngle)) * WORMHOLE_CORONA_PULSE_SIZE_RATIO;

            SpriteDraw.drawAdditiveAtCenter(spriteCache, WORMHOLE_CORONA_TEXTURE_PATH,
                    cx + (float) Math.cos(rad) * orbit, cy + (float) Math.sin(rad) * orbit,
                    size * pulse, size * pulse, color, alphaMult, angle);
        }
    }

    private void drawWormholeBands(float cx, float cy, float baseRadius, Color color, float alphaMult) {
        SpriteAPI texture = spriteCache.texture(WORMHOLE_BAND_TEXTURE_PATH);
        if (texture == null) return;
        float innerRadius = baseRadius * WORMHOLE_BAND_INNER_RADIUS_RATIO;
        float outerRadius = innerRadius + baseRadius * WORMHOLE_BAND_THICKNESS_RATIO;
        float rotationA = (elapsedSeconds * WORMHOLE_BAND_ROTATION_SPEED_DEG) % 360f;
        float rotationB = (-elapsedSeconds * WORMHOLE_BAND_ROTATION_SPEED_DEG) % 360f;

        RadialBand band = new RadialBand(new Vector2f(cx, cy), innerRadius, outerRadius);
        WormholeBandRenderer.render(texture, band, 0, rotationA, color, alphaMult, elapsedSeconds);
        WormholeBandRenderer.render(texture, band, 1, rotationB, color, alphaMult, elapsedSeconds);
    }

    private void drawWormholeGlow(float cx, float cy, float baseRadius, Color color, float alphaMult, float openness) {
        float size = baseRadius * 2f * WORMHOLE_GLOW_SIZE_RATIO;
        float closedness = 1f - openness;
        float alpha = WORMHOLE_GLOW_ALPHA * closedness * closedness * closedness * alphaMult;

        //intentionally rendering two because it's prettier
        SpriteDraw.drawAdditiveAtCenter(spriteCache, WORMHOLE_GLOW_TEXTURE_PATH, cx, cy, size, size, color, alpha);
        SpriteDraw.drawAdditiveAtCenter(spriteCache, WORMHOLE_GLOW_TEXTURE_PATH, cx, cy, size, size, color, alpha);
    }

    private Color resolveWormholeColor(SkillNode node) {
        return parsedColor(wormholeColors, node, "wormholeColor", node.getWormholeColor(), Color.WHITE);
    }

    private static Color parsedColor(Map<String, Color> cache, SkillNode node, String fieldName, String hex, Color fallback) {
        Color color = cache.get(node.getId());
        if (color == null) {
            color = ColorUtil.parseHexColor(hex, fallback,
                    Logger.getLogger(SkillTreeNodeRingRenderer.class), fieldName + " on node \"" + node.getId() + "\"");
            cache.put(node.getId(), color);
        }
        return color;
    }

    private static String resolveRingBeltPath(SkillNode node) {
        String path = node.getRingBeltPath();
        return path != null && !path.isEmpty() ? path : DEFAULT_KEYSTONE_RING_BELT_PATH;
    }

    private Color resolveRingBeltColor(SkillNode node) {
        return parsedColor(ringBeltColors, node, "ringBeltColor", node.getRingBeltColor(), DEFAULT_AURORA_COLOR);
    }

    private static float resolveRingBeltWidth(SkillNode node) {
        Float width = node.getRingBeltWidth();
        return width != null && width > 0f ? width : KEYSTONE_BELT_WIDTH_RATIO;
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

    enum RingState {
        IDLE(false, false), BREATHING(false, true), ALLOCATED(true, false), ALLOCATED_BREATHING(true, true);

        private static final RingState[] BY_FLAGS = values();

        final boolean allocated;
        final boolean breathing;

        RingState(boolean allocated, boolean breathing) {
            this.allocated = allocated;
            this.breathing = breathing;
        }

        static RingState of(boolean allocated, boolean breathing) {
            return BY_FLAGS[(allocated ? 2 : 0) + (breathing ? 1 : 0)];
        }
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

    private void drawRingOutline(float cx, float cy, float radius, Color color, float alpha) {
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            GL11.glVertex2f(cx + RING_CIRCLE.cos(i) * radius, cy + RING_CIRCLE.sin(i) * radius);
        }
        GL11.glEnd();
    }
}
