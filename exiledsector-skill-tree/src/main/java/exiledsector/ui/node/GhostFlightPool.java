package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.TexturedQuadBatch;

import java.util.ArrayList;
import java.util.List;

public final class GhostFlightPool {

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
