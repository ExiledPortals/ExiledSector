package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class SensorGhostRenderer {

    private static final float MIN_BLINK_PERIOD = 1.2f;
    private static final float MAX_BLINK_PERIOD = 2.6f;
    private static final float MIN_ALPHA = 0.15f;
    private static final float MAX_ALPHA = 0.9f;

    private final int ghostCount;
    private final float ghostSizeRatio;
    private final float scatterRadiusRatio;
    private final SpriteCache spriteCache = new SpriteCache(SensorGhostRenderer.class);
    private final TexturedQuadBatch ghostQuads = new TexturedQuadBatch();
    private final Map<String, List<Ghost>> ghostsByKey = new HashMap<>();
    private float elapsedSeconds = 0f;

    public SensorGhostRenderer(int ghostCount, float ghostSizeRatio, float scatterRadiusRatio) {
        this.ghostCount = ghostCount;
        this.ghostSizeRatio = ghostSizeRatio;
        this.scatterRadiusRatio = scatterRadiusRatio;
    }

    public void advance(float amount) {
        elapsedSeconds += amount;
    }

    public void draw(float cx, float cy, float footprintSize, float alphaMult, String seedKey) {
        SpriteAPI sprite = spriteCache.sprite(GhostFlight.TEXTURE_PATH);
        if (sprite == null) return;

        Color ghostColor = GhostFlight.color();
        float ghostSize = footprintSize * ghostSizeRatio;

        try (TexturedQuadBatch quads = ghostQuads.beginNormal(sprite)) {
            for (Ghost ghost : ghosts(seedKey)) {
                float blinkT = (float) (0.5 + 0.5 * Math.sin(2 * Math.PI * (elapsedSeconds / ghost.blinkPeriod() + ghost.blinkPhase())));
                float alpha = (MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * blinkT) * alphaMult;
                quads.quadAtCenter(cx + ghost.offsetXFraction() * footprintSize, cy + ghost.offsetYFraction() * footprintSize,
                        ghostSize, ghostSize, ghost.rotationDeg(), ghostColor, alpha);
            }
        }
    }

    public float ghostSizeRatio() {
        return ghostSizeRatio;
    }

    public List<Ghost> ghosts(String seedKey) {
        return ghostsByKey.computeIfAbsent(seedKey, this::generateGhosts);
    }

    private List<Ghost> generateGhosts(String seedKey) {
        Random random = new Random(seedKey.hashCode());
        List<Ghost> ghosts = new ArrayList<>(ghostCount);
        for (int i = 0; i < ghostCount; i++) {
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float radiusFraction = scatterRadiusRatio * random.nextFloat();
            float rotationDeg = random.nextFloat() * 360f;
            float blinkPeriod = MIN_BLINK_PERIOD + random.nextFloat() * (MAX_BLINK_PERIOD - MIN_BLINK_PERIOD);
            float blinkPhase = random.nextFloat();
            ghosts.add(new Ghost((float) Math.cos(angle) * radiusFraction, (float) Math.sin(angle) * radiusFraction,
                    rotationDeg, blinkPeriod, blinkPhase));
        }
        return List.copyOf(ghosts);
    }

    public record Ghost(float offsetXFraction, float offsetYFraction, float rotationDeg, float blinkPeriod, float blinkPhase) {
    }
}
