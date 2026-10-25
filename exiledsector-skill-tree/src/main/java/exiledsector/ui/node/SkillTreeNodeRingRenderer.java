package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SmoothZoom;
import exiledsector.ui.belt.AuroraBeltRenderer;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledsector.ui.util.ColorUtil;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static exiledsector.ui.node.SkillTreeNodeGeometry.beltInnerRadius;
import static exiledsector.ui.node.SkillTreeNodeGeometry.beltOuterRadius;
import static exiledsector.ui.node.NodeMetrics.donutRadius;

final class SkillTreeNodeRingRenderer {

    private static final float[] SOCKET_OUTLINE_RADII =
            NodeRingArt.outlineRadii((cos, sin) -> SkillTreeSocketRenderer.visibleEdgeDistance(2f, cos, sin));

    private static final String DEFAULT_KEYSTONE_RING_BELT_PATH = "graphics/planets/aurorae.png";
    private static final float KEYSTONE_BELT_WIDTH_RATIO = 1.1f;

    private static final String AURORA_TEXTURE_PATH = "graphics/planets/aurorae.png";
    private static final Color DEFAULT_AURORA_COLOR = new Color(140, 120, 255);

    private static final float GRANT_RING_ALPHA = 0.9f;

    private static final float REACH_FOOTPRINT_RATIO = 2f;
    private static final float KEYSTONE_BELT_REACH_MARGIN = 1.3f;

    private final SkillTreePanelStyle panelStyle;
    private final SpriteCache spriteCache = new SpriteCache(SkillTreeNodeRingRenderer.class);
    private final NodeRingArt ringArt = new NodeRingArt();
    private final WormholeArt wormholeArt = new WormholeArt(ringArt);
    private final NodeBeams beams = new NodeBeams(ringArt);
    private final WormholeOpenness wormholeOpenness;
    private final Map<String, Color> wormholeColors = new HashMap<>();
    private final Map<String, Color> ringBeltColors = new HashMap<>();

    SkillTreeNodeRingRenderer(SkillTreePanelStyle panelStyle, WormholeOpenness wormholeOpenness) {
        this.panelStyle = panelStyle;
        this.wormholeOpenness = wormholeOpenness;
    }

    void advance(float amount) {
        ringArt.advance(amount);
    }

    void startPulse(String nodeId) {
        ringArt.startPulse(nodeId);
    }

    boolean isPulsing(String nodeId) {
        return ringArt.isPulsing(nodeId);
    }

    NodeBeams beams() {
        return beams;
    }

    float reach(float footprintSize, SkillNode node) {
        float reachRadius = footprintSize * REACH_FOOTPRINT_RATIO;
        SkillTier tier = node.getType().getTier();
        if (tier == SkillTier.KEYSTONE || tier == SkillTier.SOCKET) {
            reachRadius = Math.max(reachRadius, beltOuterRadius(footprintSize, resolveRingBeltWidth(node)) * KEYSTONE_BELT_REACH_MARGIN);
        }
        return reachRadius;
    }

    void draw(float cx, float cy, float footprintSize, float alphaMult, RingState ringState, float zoom, SkillNode node) {
        draw(cx, cy, footprintSize, alphaMult, ringState, zoom, node, null, null);
    }

    void draw(float cx, float cy, float footprintSize, float alphaMult, RingState ringState, float zoom, SkillNode node, Color doctrineColor,
              Color grantColor) {
        boolean allocated = ringState.allocated;
        SkillTier tier = node.getType().getTier();
        String nodeId = node.getId();
        float halfSize = footprintSize / 2f;
        float scale = tier.getSizeMultiplier();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float ringRadius = donutRadius(footprintSize);
        if (tier == SkillTier.NOTABLE) {
            float stateAlpha = allocated ? 1f : NodeRingArt.UNALLOCATED_ALPHA_MULT;
            ringArt.drawNotableRings(cx, cy, footprintSize, nodeId, stateAlpha * alphaMult,
                    doctrineColor == null ? NodeRingArt.RING_PINK_COLOR : doctrineColor);
            ringArt.drawAmbientGlow(cx, cy, footprintSize, stateAlpha * alphaMult,
                    doctrineColor == null ? NodeRingArt.AMBIENT_GLOW_COLOR : doctrineColor);
        } else if (tier == SkillTier.KEYSTONE) {
            float stateAlpha = allocated ? 1f : NodeRingArt.UNALLOCATED_ALPHA_MULT;
            String ringBeltPath = resolveRingBeltPath(node);
            float ringBeltWidth = resolveRingBeltWidth(node);
            if (AURORA_TEXTURE_PATH.equals(ringBeltPath)) {
                drawKeystoneAuroraBelt(cx, cy, footprintSize, ringBeltWidth, resolveRingBeltColor(node), stateAlpha, alphaMult, zoom);
            } else {
                drawKeystoneRingBelt(cx, cy, footprintSize, ringBeltWidth, ringBeltPath, stateAlpha, alphaMult);
            }
        } else if (tier.isWormhole()) {
            drawWormhole(cx, cy, footprintSize, alphaMult, nodeId, resolveWormholeColor(node), wormholeOpenness.of(nodeId));
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        if (tier.isWormhole()) {
            return;
        }
        float widthScale = NodeBeams.widthScale(zoom) * scale;
        Color accent = panelStyle.getAccentColor();
        if (tier == SkillTier.SOCKET) {
            if (ringState.breathing) {
                beams.nodeRing(cx, cy, halfSize, SOCKET_OUTLINE_RADII, NodeBeams.RingLight.BREATHING, accent, nodeId, alphaMult, widthScale);
            } else {
                beams.pulse(cx, cy, halfSize, SOCKET_OUTLINE_RADII, accent, nodeId, alphaMult, widthScale);
            }
            return;
        }
        beams.nodeRing(cx, cy, ringRadius, null, ringLight(ringState), accent, nodeId, alphaMult, widthScale);
        if (ringState == RingState.ALLOCATED_BREATHING) {
            beams.ring(cx, cy, ringRadius, null, beams.lit(accent), beams.lightAlpha(NodeBeams.RingLight.BREATHING) * alphaMult, widthScale);
        }
        if (grantColor != null) {
            beams.ring(cx, cy, ringRadius + 2f * NodeBeams.ringEdge(widthScale), null, beams.lit(grantColor), GRANT_RING_ALPHA * alphaMult,
                    widthScale);
        }
    }

    private static NodeBeams.RingLight ringLight(RingState ringState) {
        if (ringState.allocated) {
            return NodeBeams.RingLight.LIT;
        }
        return ringState.breathing ? NodeBeams.RingLight.BREATHING : NodeBeams.RingLight.DULL;
    }

    void drawDoctrineSocketBelt(float cx, float cy, float footprintSize, Color doctrineColor, boolean live, float alphaMult, float zoom) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawKeystoneAuroraBelt(cx, cy, footprintSize, KEYSTONE_BELT_WIDTH_RATIO, doctrineColor, live ? 1f : NodeRingArt.UNALLOCATED_ALPHA_MULT,
                alphaMult, zoom);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
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
                tint, stateAlpha * alphaMult, ringArt.elapsedSeconds(), SmoothZoom.MAX_ZOOM / zoom);
    }

    void drawSocketWormhole(float cx, float cy, float footprintSize, float alphaMult, String socketId, SkillNode exit, float rawOpenness) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        drawWormhole(cx, cy, footprintSize, alphaMult, socketId, resolveWormholeColor(exit), rawOpenness);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawWormhole(float cx, float cy, float footprintSize, float alphaMult, String nodeId, Color wormholeColor, float rawOpenness) {
        wormholeArt.draw(cx, cy, footprintSize, alphaMult, nodeId, wormholeColor, rawOpenness);
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
}
