package exiledsector.ui.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class SocketArcs {

    static final float BORDER_INSET = 0.1f;
    static final int MAX_ARCS = 2;
    static final float MIN_SPAWN_GAP = 0.25f;
    static final float MAX_SPAWN_GAP = 1.1f;
    static final float MIN_LIFE = 0.12f;
    static final float MAX_LIFE = 0.3f;
    static final float RESHAPE_INTERVAL = 0.05f;
    static final float MIN_SPAN = 0.3f;
    static final float MAX_SPAN = 0.9f;
    static final float JITTER = 0.1f;
    private static final int POINTS_PER_EDGE = 10;
    private static final float MIN_FLICKER = 0.6f;
    private static final int EDGES = 4;

    private final Random random;
    private final Map<String, NodeArcs> arcsByNodeId = new HashMap<>();
    private final Set<String> drawnNodeIds = new HashSet<>();

    SocketArcs(Random random) {
        this.random = random;
    }

    void advance(float amount) {
        arcsByNodeId.keySet().retainAll(drawnNodeIds);
        drawnNodeIds.clear();
        for (NodeArcs nodeArcs : arcsByNodeId.values()) {
            nodeArcs.advance(amount);
        }
    }

    List<Arc> arcs(String nodeId) {
        drawnNodeIds.add(nodeId);
        return arcsByNodeId.computeIfAbsent(nodeId, id -> new NodeArcs()).arcs;
    }

    static float perimeterX(float position) {
        float wrapped = wrap(position);
        int edge = (int) wrapped;
        float t = wrapped - edge;
        return switch (edge) {
            case 0 -> -1f + 2f * t;
            case 1 -> 1f;
            case 2 -> 1f - 2f * t;
            default -> -1f;
        };
    }

    static float perimeterY(float position) {
        float wrapped = wrap(position);
        int edge = (int) wrapped;
        float t = wrapped - edge;
        return switch (edge) {
            case 0 -> -1f;
            case 1 -> -1f + 2f * t;
            case 2 -> 1f;
            default -> 1f - 2f * t;
        };
    }

    private static float wrap(float position) {
        float wrapped = position % EDGES;
        return wrapped < 0f ? wrapped + EDGES : Math.min(wrapped, Math.nextDown((float) EDGES));
    }

    private float between(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private final class NodeArcs {

        private final List<Arc> arcs = new ArrayList<>(MAX_ARCS);
        private float secondsUntilSpawn = between(0f, MAX_SPAWN_GAP);

        void advance(float amount) {
            Iterator<Arc> arcIterator = arcs.iterator();
            while (arcIterator.hasNext()) {
                Arc arc = arcIterator.next();
                arc.age += amount;
                if (arc.age >= arc.life) {
                    arcIterator.remove();
                    continue;
                }
                arc.secondsUntilReshape -= amount;
                if (arc.secondsUntilReshape <= 0f) {
                    arc.reshape();
                }
            }
            secondsUntilSpawn -= amount;
            if (secondsUntilSpawn <= 0f) {
                secondsUntilSpawn = between(MIN_SPAWN_GAP, MAX_SPAWN_GAP);
                if (arcs.size() < MAX_ARCS) {
                    arcs.add(new Arc(between(0f, EDGES), between(MIN_SPAN, MAX_SPAN), between(MIN_LIFE, MAX_LIFE)));
                }
            }
        }
    }

    final class Arc {

        private final float startPosition;
        private final float span;
        private final float life;
        private final float[] pointXs;
        private final float[] pointYs;
        private float age;
        private float secondsUntilReshape;
        private float flicker;

        private Arc(float startPosition, float span, float life) {
            this.startPosition = startPosition;
            this.span = span;
            this.life = life;
            int points = Math.max(3, Math.round(span * POINTS_PER_EDGE)) + 1;
            this.pointXs = new float[points];
            this.pointYs = new float[points];
            reshape();
        }

        private void reshape() {
            secondsUntilReshape = RESHAPE_INTERVAL;
            flicker = between(MIN_FLICKER, 1f);
            int last = pointXs.length - 1;
            float scale = 1f - BORDER_INSET;
            for (int i = 0; i <= last; i++) {
                float position = startPosition + span * i / last;
                float x = perimeterX(position) * scale;
                float y = perimeterY(position) * scale;
                float offset = i == 0 || i == last ? 0f : between(-JITTER, JITTER);
                int edge = (int) wrap(position);
                pointXs[i] = x + offset * (edge == 1 ? -1f : edge == 3 ? 1f : 0f);
                pointYs[i] = y + offset * (edge == 0 ? 1f : edge == 2 ? -1f : 0f);
            }
        }

        int pointCount() {
            return pointXs.length;
        }

        float x(int index) {
            return pointXs[index];
        }

        float y(int index) {
            return pointYs[index];
        }

        float alpha() {
            float remaining = 1f - age / life;
            return flicker * remaining;
        }
    }
}
