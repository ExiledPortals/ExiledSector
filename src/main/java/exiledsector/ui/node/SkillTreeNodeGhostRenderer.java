package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.ui.util.SpriteCache;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class SkillTreeNodeGhostRenderer {

    static final String GHOST_TEXTURE_PATH = "graphics/icons/fleet_triangle.png";

    private static final int GHOST_COUNT = 3;
    private static final float GHOST_SIZE_RATIO = 0.4f;
    private static final float GHOST_SCATTER_RADIUS_RATIO = 0.3f;
    private static final float GHOST_MIN_BLINK_PERIOD = 1.2f;
    private static final float GHOST_MAX_BLINK_PERIOD = 2.6f;
    private static final float GHOST_MIN_ALPHA = 0.15f;
    private static final float GHOST_MAX_ALPHA = 0.9f;

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeNodeGhostRenderer.class);
    private final Map<String, List<GhostInstance>> ghostsByNode = new HashMap<>();
    private float elapsedSeconds = 0f;

    void advance(float amount) {
        elapsedSeconds += amount;
    }

    void draw(float cx, float cy, float footprintSize, float alphaMult, String nodeId) {
        SpriteAPI sprite = spriteCache.sprite(GHOST_TEXTURE_PATH);
        if (sprite == null) return;

        List<GhostInstance> ghosts = ghostsByNode.computeIfAbsent(nodeId, id -> generateGhosts(id));
        Color color = GhostFlight.color();
        float size = footprintSize * GHOST_SIZE_RATIO;

        for (GhostInstance ghost : ghosts) {
            float blinkT = (float) (0.5 + 0.5 * Math.sin(2 * Math.PI * (elapsedSeconds / ghost.blinkPeriod + ghost.blinkPhase)));
            float alpha = (GHOST_MIN_ALPHA + (GHOST_MAX_ALPHA - GHOST_MIN_ALPHA) * blinkT) * alphaMult;

            sprite.setSize(size, size);
            sprite.setAngle(ghost.rotationDeg);
            sprite.setColor(color);
            sprite.setAlphaMult(alpha);
            sprite.renderAtCenter(cx + ghost.offsetXFraction * footprintSize, cy + ghost.offsetYFraction * footprintSize);
        }
    }

    private static List<GhostInstance> generateGhosts(String seedKey) {
        Random random = new Random(seedKey.hashCode());
        List<GhostInstance> ghosts = new ArrayList<>(GHOST_COUNT);
        for (int i = 0; i < GHOST_COUNT; i++) {
            GhostInstance ghost = new GhostInstance();
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float radiusFraction = GHOST_SCATTER_RADIUS_RATIO * random.nextFloat();
            ghost.offsetXFraction = (float) Math.cos(angle) * radiusFraction;
            ghost.offsetYFraction = (float) Math.sin(angle) * radiusFraction;
            ghost.rotationDeg = random.nextFloat() * 360f;
            ghost.blinkPeriod = GHOST_MIN_BLINK_PERIOD + random.nextFloat() * (GHOST_MAX_BLINK_PERIOD - GHOST_MIN_BLINK_PERIOD);
            ghost.blinkPhase = random.nextFloat();
            ghosts.add(ghost);
        }
        return ghosts;
    }

    private static final class GhostInstance {
        float offsetXFraction;
        float offsetYFraction;
        float rotationDeg;
        float blinkPeriod;
        float blinkPhase;
    }
}
