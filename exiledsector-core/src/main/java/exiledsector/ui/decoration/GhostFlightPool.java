package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.TexturedQuadBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class GhostFlightPool {

    public static final class LaunchTimer {

        private final float minSeconds;
        private final float maxSeconds;
        private final Random random;
        private float secondsUntilLaunch;

        public LaunchTimer(float minSeconds, float maxSeconds, Random random) {
            this.minSeconds = minSeconds;
            this.maxSeconds = maxSeconds;
            this.random = random;
            secondsUntilLaunch = random.nextFloat() * maxSeconds;
        }

        public boolean launchesAfter(float amount) {
            secondsUntilLaunch -= amount;
            if (secondsUntilLaunch > 0f) {
                return false;
            }
            secondsUntilLaunch = minSeconds + random.nextFloat() * (maxSeconds - minSeconds);
            return true;
        }
    }

    private final SpriteCache spriteCache = new SpriteCache(GhostFlightPool.class);
    private final TexturedQuadBatch ghostQuads = new TexturedQuadBatch();
    private final List<GhostFlight> flights = new ArrayList<>();

    public void add(GhostFlight flight) {
        flights.add(flight);
    }

    public void clear() {
        flights.clear();
    }

    public int size() {
        return flights.size();
    }

    public void advance(float amount) {
        flights.removeIf(flight -> flight.advance(amount));
    }

    public void draw(float ghostSize, TreeViewport viewport, float alphaMult) {
        if (alphaMult <= 0f || flights.isEmpty()) {
            return;
        }
        SpriteAPI sprite = spriteCache.sprite(GhostFlight.TEXTURE_PATH);
        if (sprite == null) {
            return;
        }
        try (TexturedQuadBatch quads = ghostQuads.beginNormal(sprite)) {
            for (GhostFlight flight : flights) {
                flight.draw(quads, GhostFlight.color(), ghostSize, viewport, alphaMult);
            }
        }
    }
}
