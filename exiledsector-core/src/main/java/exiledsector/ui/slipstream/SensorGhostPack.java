package exiledsector.ui.slipstream;

import exiledsector.ui.util.TexturedQuadBatch;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class SensorGhostPack {

    static final float LEADER_RADIUS_WIDTHS = 0.7f;
    static final int LEADER_BLIPS = 10;
    static final int MIN_COMPANIONS = 2;
    static final int MAX_COMPANIONS = 4;
    static final float COMPANION_RADIUS_WIDTHS = 0.2f;
    static final int COMPANION_BLIPS = 3;
    static final float MIN_COMPANION_ALONG_WIDTHS = -1f;
    static final float MAX_COMPANION_ALONG_WIDTHS = 0.6f;
    static final float COMPANION_LATERAL_WIDTHS = 0.35f;
    static final float COMPANION_SWAY_WIDTHS = 0.15f;
    static final float MIN_SWAY_PER_SECOND = 0.6f;
    static final float MAX_SWAY_PER_SECOND = 1.4f;
    static final float BLIP_WIDTHS = 0.08f;
    static final float MIN_BLIP_PIXELS = 8f;
    static final float MAX_BLIP_PIXELS = 24f;

    private final SlipstreamRoute route;
    private final Random random;
    private final List<Ghost> ghosts = new ArrayList<>();
    private final Vector2f position = new Vector2f();
    private float elapsed;
    private float leaderDistance;

    SensorGhostPack(SlipstreamRoute route, Random random) {
        this.route = route;
        this.random = random;
        float width = route.width();
        ghosts.add(new Ghost(new BlipCloud(width * LEADER_RADIUS_WIDTHS, LEADER_BLIPS), 0f, 0f, 0f, 0f, 0f));
        int companions = MIN_COMPANIONS + random.nextInt(MAX_COMPANIONS - MIN_COMPANIONS + 1);
        for (int i = 0; i < companions; i++) {
            float along = width * (MIN_COMPANION_ALONG_WIDTHS + random.nextFloat() * (MAX_COMPANION_ALONG_WIDTHS - MIN_COMPANION_ALONG_WIDTHS));
            float lateral = width * COMPANION_LATERAL_WIDTHS * (random.nextFloat() * 2f - 1f);
            float swayRate = MIN_SWAY_PER_SECOND + random.nextFloat() * (MAX_SWAY_PER_SECOND - MIN_SWAY_PER_SECOND);
            ghosts.add(new Ghost(new BlipCloud(width * COMPANION_RADIUS_WIDTHS, COMPANION_BLIPS), along, lateral, width * COMPANION_SWAY_WIDTHS,
                    swayRate, random.nextFloat() * 10f));
        }
    }

    void advance(float amount, float distance) {
        elapsed += amount;
        leaderDistance = distance;
        boolean flying = distance < route.length();
        for (Ghost ghost : ghosts) {
            ghost.cloud.advance(amount, flying, random);
        }
    }

    boolean isDone() {
        if (leaderDistance < route.length()) {
            return false;
        }
        for (Ghost ghost : ghosts) {
            if (ghost.cloud.count() > 0) {
                return false;
            }
        }
        return true;
    }

    void draw(TexturedQuadBatch quads, SlipstreamProjection projection, Color colour, float alphaMult) {
        float blipPixels = Math.max(MIN_BLIP_PIXELS, Math.min(MAX_BLIP_PIXELS, route.width() * BLIP_WIDTHS * projection.view().zoom()));
        for (Ghost ghost : ghosts) {
            float sway = (float) Math.sin(elapsed * ghost.swayRate + ghost.swayPhase) * ghost.swayReach;
            float along = Math.max(0f, Math.min(route.length(), leaderDistance + ghost.along + sway));
            route.offsetPoint(along, ghost.lateral + (float) Math.cos(elapsed * ghost.swayRate * 0.7f + ghost.swayPhase) * ghost.swayReach, position);
            ghost.cloud.draw(quads, projection, position.x, position.y, blipPixels, colour, alphaMult);
        }
    }

    private record Ghost(BlipCloud cloud, float along, float lateral, float swayReach, float swayRate, float swayPhase) {
    }
}
