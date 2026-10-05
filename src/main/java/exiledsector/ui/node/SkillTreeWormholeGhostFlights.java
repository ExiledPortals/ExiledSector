package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class SkillTreeWormholeGhostFlights {

    static final float MIN_SECONDS_BETWEEN_FLIGHTS = 10f;
    static final float MAX_SECONDS_BETWEEN_FLIGHTS = 25f;
    private static final float FLIGHT_SPEED = 1400f;
    private static final float GHOST_SIZE_RATIO = 0.35f;

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeWormholeGhostFlights.class);
    private final Random random;
    private final Map<String, float[]> secondsUntilNextFlight = new HashMap<>();
    private final List<GhostFlight> flights = new ArrayList<>();

    SkillTreeWormholeGhostFlights(Random random) {
        this.random = random;
    }

    void advance(float amount, ShipSkillData data) {
        flights.removeIf(flight -> flight.advance(amount));
        for (SkillTreeTopology.WormholePair pair : SkillTree.topology().wormholePairs()) {
            if (data.isAllocated(pair.first().getId()) && data.isAllocated(pair.second().getId())) {
                scheduleFlights(pair, amount);
            }
        }
    }

    int activeFlightCount() {
        return flights.size();
    }

    private void scheduleFlights(SkillTreeTopology.WormholePair pair, float amount) {
        String key = pair.first().getId();
        float[] remaining = secondsUntilNextFlight.get(key);
        if (remaining == null) {
            remaining = new float[]{random.nextFloat() * MAX_SECONDS_BETWEEN_FLIGHTS};
            secondsUntilNextFlight.put(key, remaining);
        }
        remaining[0] -= amount;
        if (remaining[0] <= 0f) {
            boolean forwards = random.nextBoolean();
            flights.add(forwards ? launch(pair.first(), pair.second()) : launch(pair.second(), pair.first()));
            remaining[0] = MIN_SECONDS_BETWEEN_FLIGHTS
                    + random.nextFloat() * (MAX_SECONDS_BETWEEN_FLIGHTS - MIN_SECONDS_BETWEEN_FLIGHTS);
        }
    }

    void launchFrom(SkillNode from, SkillNode to) {
        flights.add(launch(from, to));
    }

    private GhostFlight launch(SkillNode from, SkillNode to) {
        return GhostFlight.between(from.getOffsetX(), from.getOffsetY(), to.getOffsetX(), to.getOffsetY(), FLIGHT_SPEED, random);
    }

    void draw(TreeViewport viewport, float alphaMult) {
        SpriteAPI sprite = flights.isEmpty() ? null : spriteCache.sprite(GhostFlight.TEXTURE_PATH);
        if (sprite == null) {
            return;
        }
        float size = SkillTreeNodeGeometry.NODE_SIZE * GHOST_SIZE_RATIO * viewport.zoom();
        for (GhostFlight flight : flights) {
            flight.draw(sprite, GhostFlight.color(), size, viewport, alphaMult);
        }
    }
}
