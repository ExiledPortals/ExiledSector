package exiledsector.ui.node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class AllocationAnimation {

    static final float FILL_SECONDS = 0.5f;
    static final float MAX_TOTAL_SECONDS = 3f;
    static final float BASE_RING_SECONDS = 0.2f;

    interface Tree {
        List<String> neighbours(String nodeId);

        boolean isSatisfied(String nodeId);
    }

    interface Listener {
        void started(String nodeId, boolean allocating);

        void lit(String nodeId);
    }

    private final Tree tree;
    private final Listener listener;
    private final Map<String, NodeTimer> timersByNodeId = new HashMap<>();
    private final Map<String, Map<String, Edge>> edgesByFirstId = new HashMap<>();

    AllocationAnimation(Tree tree, Listener listener) {
        this.tree = tree;
        this.listener = listener;
    }

    static float ringSeconds(int ringCount) {
        return ringCount <= 1 ? 0f : Math.min(BASE_RING_SECONDS, (MAX_TOTAL_SECONDS - FILL_SECONDS) / (ringCount - 1));
    }

    void allocated(Collection<String> nodeIds, String originId) {
        Map<String, Integer> ringById = rings(withoutReversed(nodeIds, false), originId);
        float ringSeconds = ringSeconds(ringCount(ringById));
        for (Map.Entry<String, Integer> ring : ringById.entrySet()) {
            String nodeId = ring.getKey();
            List<String> sources = new ArrayList<>();
            for (String neighbourId : tree.neighbours(nodeId)) {
                Integer neighbourRing = ringById.get(neighbourId);
                if (neighbourRing == null ? tree.isSatisfied(neighbourId) : neighbourRing < ring.getValue()) {
                    sources.add(neighbourId);
                }
            }
            timersByNodeId.put(nodeId, NodeTimer.forAllocation(ring.getValue() * ringSeconds, sources));
        }
    }

    void deallocated(Collection<String> nodeIds, String originId) {
        Map<String, Integer> ringById = rings(withoutReversed(nodeIds, true), originId);
        int lastRing = ringCount(ringById) - 1;
        float ringSeconds = ringSeconds(lastRing + 1);
        for (Map.Entry<String, Integer> ring : ringById.entrySet()) {
            timersByNodeId.put(ring.getKey(), NodeTimer.forDeallocation((lastRing - ring.getValue()) * ringSeconds));
        }
    }

    private Set<String> withoutReversed(Collection<String> nodeIds, boolean wasAllocating) {
        Set<String> changed = new LinkedHashSet<>(nodeIds);
        Iterator<String> changedIds = changed.iterator();
        while (changedIds.hasNext()) {
            String nodeId = changedIds.next();
            NodeTimer pending = timersByNodeId.get(nodeId);
            if (pending != null && pending.allocating == wasAllocating) {
                timersByNodeId.remove(nodeId);
                cancelEdges(nodeId);
                changedIds.remove();
            }
        }
        return changed;
    }

    private Map<String, Integer> rings(Set<String> changed, String originId) {
        Map<String, Integer> ringById = new HashMap<>();
        Deque<String> frontier = new ArrayDeque<>();
        for (String nodeId : changed) {
            for (String neighbourId : tree.neighbours(nodeId)) {
                if (!changed.contains(neighbourId) && tree.isSatisfied(neighbourId)) {
                    ringById.put(nodeId, 0);
                    frontier.add(nodeId);
                    break;
                }
            }
        }
        if (frontier.isEmpty() && changed.contains(originId)) {
            ringById.put(originId, 0);
            frontier.add(originId);
        }
        while (!frontier.isEmpty()) {
            String nodeId = frontier.poll();
            int nextRing = ringById.get(nodeId) + 1;
            for (String neighbourId : tree.neighbours(nodeId)) {
                if (changed.contains(neighbourId) && !ringById.containsKey(neighbourId)) {
                    ringById.put(neighbourId, nextRing);
                    frontier.add(neighbourId);
                }
            }
        }
        int unreachedRing = ringById.isEmpty() ? 0 : ringCount(ringById);
        for (String nodeId : changed) {
            ringById.putIfAbsent(nodeId, unreachedRing);
        }
        return ringById;
    }

    private static int ringCount(Map<String, Integer> ringById) {
        int lastRing = 0;
        for (int ring : ringById.values()) {
            lastRing = Math.max(lastRing, ring);
        }
        return lastRing + 1;
    }

    boolean isEmpty() {
        return timersByNodeId.isEmpty() && edgesByFirstId.isEmpty();
    }

    boolean shown(String nodeId, boolean live) {
        if (timersByNodeId.isEmpty()) {
            return live;
        }
        NodeTimer timer = timersByNodeId.get(nodeId);
        return timer == null ? live : !timer.allocating;
    }

    void advance(float amount) {
        if (isEmpty()) {
            return;
        }
        removeEdgesIf(edge -> {
            edge.elapsed += amount;
            return edge.elapsed >= FILL_SECONDS;
        });
        List<String> starting = new ArrayList<>();
        List<String> lit = new ArrayList<>();
        for (Map.Entry<String, NodeTimer> entry : timersByNodeId.entrySet()) {
            NodeTimer timer = entry.getValue();
            timer.remainingSeconds -= amount;
            if (timer.remainingSeconds > 0f) {
                continue;
            }
            if (!timer.started) {
                starting.add(entry.getKey());
            } else {
                lit.add(entry.getKey());
            }
        }
        starting.forEach(this::start);
        for (String nodeId : lit) {
            timersByNodeId.remove(nodeId);
            listener.lit(nodeId);
        }
    }

    private void start(String nodeId) {
        NodeTimer timer = timersByNodeId.get(nodeId);
        float lateSeconds = -timer.remainingSeconds;
        if (timer.allocating) {
            cancelEdges(nodeId);
        } else {
            removeEdgesIf(edge -> !edge.draining && edge.touches(nodeId));
        }
        listener.started(nodeId, timer.allocating);
        if (timer.allocating) {
            timer.started = true;
            timer.remainingSeconds = FILL_SECONDS - lateSeconds;
            boolean filling = false;
            for (String sourceId : timer.sources) {
                if (tree.isSatisfied(sourceId)) {
                    putEdge(new Edge(sourceId, nodeId, false, lateSeconds));
                    filling = true;
                }
            }
            if (!filling || timer.remainingSeconds <= 0f) {
                timersByNodeId.remove(nodeId);
                listener.lit(nodeId);
            }
            return;
        }
        timersByNodeId.remove(nodeId);
        for (String neighbourId : tree.neighbours(nodeId)) {
            if (shown(neighbourId, tree.isSatisfied(neighbourId))) {
                putEdge(new Edge(neighbourId, nodeId, true, lateSeconds));
            }
        }
    }

    private void cancelEdges(String nodeId) {
        removeEdgesIf(edge -> edge.touches(nodeId));
    }

    private void putEdge(Edge edge) {
        boolean ordered = edge.litId.compareTo(edge.otherId) <= 0;
        edgesByFirstId.computeIfAbsent(ordered ? edge.litId : edge.otherId, ignored -> new HashMap<>())
                .put(ordered ? edge.otherId : edge.litId, edge);
    }

    private void removeEdgesIf(Predicate<Edge> removed) {
        Iterator<Map<String, Edge>> groups = edgesByFirstId.values().iterator();
        while (groups.hasNext()) {
            Map<String, Edge> group = groups.next();
            group.values().removeIf(removed);
            if (group.isEmpty()) {
                groups.remove();
            }
        }
    }

    FillRange animatedRange(String startId, String endId) {
        if (edgesByFirstId.isEmpty()) {
            return null;
        }
        boolean ordered = startId.compareTo(endId) <= 0;
        Map<String, Edge> group = edgesByFirstId.get(ordered ? startId : endId);
        Edge edge = group == null ? null : group.get(ordered ? endId : startId);
        if (edge == null) {
            return null;
        }
        float progress = Math.min(1f, edge.elapsed / FILL_SECONDS);
        float fraction = edge.draining ? 1f - progress : progress;
        return edge.litId.equals(startId) ? new FillRange(0f, fraction) : new FillRange(1f - fraction, 1f);
    }

    record FillRange(float startFraction, float endFraction) {
        static final FillRange FULL = new FillRange(0f, 1f);

        boolean isFull() {
            return startFraction <= 0f && endFraction >= 1f;
        }

        boolean contains(float from, float to) {
            return from >= startFraction && to <= endFraction;
        }
    }

    private static final class NodeTimer {
        final boolean allocating;
        final List<String> sources;
        float remainingSeconds;
        boolean started;

        private NodeTimer(boolean allocating, float delaySeconds, List<String> sources) {
            this.allocating = allocating;
            this.remainingSeconds = delaySeconds;
            this.sources = sources;
        }

        static NodeTimer forAllocation(float delaySeconds, List<String> sources) {
            return new NodeTimer(true, delaySeconds, sources);
        }

        static NodeTimer forDeallocation(float delaySeconds) {
            return new NodeTimer(false, delaySeconds, List.of());
        }
    }

    private static final class Edge {
        final String litId;
        final String otherId;
        final boolean draining;
        float elapsed;

        Edge(String litId, String otherId, boolean draining, float elapsed) {
            this.litId = litId;
            this.otherId = otherId;
            this.draining = draining;
            this.elapsed = elapsed;
        }

        boolean touches(String nodeId) {
            return litId.equals(nodeId) || otherId.equals(nodeId);
        }
    }
}
