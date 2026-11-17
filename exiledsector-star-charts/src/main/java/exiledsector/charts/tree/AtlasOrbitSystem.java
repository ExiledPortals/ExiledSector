package exiledsector.charts.tree;

import exiledsector.skills.layout.TreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AtlasOrbitSystem {

    private static final float SAME_TRACK_TOLERANCE = 1f;
    private static final double FULL_TURN = 360.0;

    public enum LinkKind { ARC, STRAIGHT, TETHER }

    public record Body(String nodeId, int index, int centreIndex, float degreesPerSecond, float startX, float startY, float offsetX, float offsetY,
                       float orbitRadius, int trackIndex, AtlasBodyArt art, String colour) {

        public boolean orbits() {
            return centreIndex >= 0;
        }
    }

    public record Track(int index, int centreIndex, float radius, float degreesPerSecond, List<Integer> memberIndexes) {
    }

    public record Link(LinkKind kind, int first, int second, int trackIndex) {
    }

    public static final class Positions {

        private final float[] xs;
        private final float[] ys;
        private final double[] turns;

        private Positions(int count) {
            this.xs = new float[count];
            this.ys = new float[count];
            this.turns = new double[count];
        }

        public float x(int bodyIndex) {
            return xs[bodyIndex];
        }

        public float y(int bodyIndex) {
            return ys[bodyIndex];
        }

        public float turnDegrees(int bodyIndex) {
            return (float) turns[bodyIndex];
        }
    }

    private final List<Body> bodies;
    private final Map<String, Integer> indexById;
    private final List<Track> tracks;
    private final List<Link> links;
    private final Map<String, Set<String>> trackNeighbours;

    private AtlasOrbitSystem(List<Body> bodies, List<Track> tracks, List<Link> links) {
        this.bodies = List.copyOf(bodies);
        this.tracks = List.copyOf(tracks);
        this.links = List.copyOf(links);
        Map<String, Integer> indexes = new HashMap<>();
        for (Body body : bodies) {
            indexes.put(body.nodeId(), body.index());
        }
        this.indexById = Map.copyOf(indexes);
        this.trackNeighbours = neighboursAcrossTethers(bodies, tracks, links);
    }

    public static AtlasOrbitSystem empty() {
        return new AtlasOrbitSystem(List.of(), List.of(), List.of());
    }

    public static AtlasOrbitSystem of(Collection<? extends TreeNode> nodes, Map<String, AtlasOrbit> orbits) {
        Map<String, TreeNode> nodesById = new LinkedHashMap<>();
        for (TreeNode node : nodes) {
            nodesById.put(node.getId(), node);
        }
        List<TreeNode> ordered = new ArrayList<>(nodesById.values());
        Map<String, Integer> depths = new HashMap<>();
        for (TreeNode node : ordered) {
            depthOf(node.getId(), nodesById, orbits, depths, new HashSet<>());
        }
        ordered.sort(Comparator.comparingInt(node -> depths.get(node.getId())));

        Map<String, Integer> indexes = new HashMap<>();
        List<Body> placed = new ArrayList<>();
        for (TreeNode node : ordered) {
            AtlasOrbit orbit = orbits.get(node.getId());
            String centreId = depths.get(node.getId()) > 0 ? orbit.aroundId() : null;
            Integer centreIndex = centreId == null ? null : indexes.get(centreId);
            AtlasBodyArt art = orbit == null || orbit.art() == null ? AtlasBodyArt.PLAIN : orbit.art();
            String colour = orbit == null ? null : orbit.colour();
            int index = placed.size();
            indexes.put(node.getId(), index);
            if (centreIndex == null) {
                placed.add(new Body(node.getId(), index, -1, 0f, node.getX(), node.getY(), 0f, 0f, 0f, -1, art, colour));
            } else {
                Body centre = placed.get(centreIndex);
                float offsetX = node.getX() - centre.startX();
                float offsetY = node.getY() - centre.startY();
                placed.add(new Body(node.getId(), index, centreIndex, orbit.degreesPerSecond(), node.getX(), node.getY(), offsetX, offsetY,
                        (float) Math.hypot(offsetX, offsetY), -1, art, colour));
            }
        }

        List<Track> tracks = new ArrayList<>();
        List<Body> bodies = withTracks(placed, tracks);
        List<Link> links = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();
        for (Body body : bodies) {
            for (String otherId : nodesById.get(body.nodeId()).getConnectedNodeIds()) {
                Integer otherIndex = indexes.get(otherId);
                if (otherIndex == null || otherIndex == body.index()) {
                    continue;
                }
                int low = Math.min(body.index(), otherIndex);
                int high = Math.max(body.index(), otherIndex);
                if (seenPairs.add(low + ":" + high)) {
                    links.add(classify(bodies, bodies.get(low), bodies.get(high)));
                }
            }
        }
        return new AtlasOrbitSystem(bodies, tracks, links);
    }

    private static int depthOf(String nodeId, Map<String, TreeNode> nodesById, Map<String, AtlasOrbit> orbits, Map<String, Integer> depths,
                               Set<String> visiting) {
        Integer known = depths.get(nodeId);
        if (known != null) {
            return known;
        }
        AtlasOrbit orbit = orbits.get(nodeId);
        String centreId = orbit == null ? null : orbit.aroundId();
        int depth = 0;
        if (centreId != null && nodesById.containsKey(centreId) && !centreId.equals(nodeId) && visiting.add(nodeId)) {
            depth = depthOf(centreId, nodesById, orbits, depths, visiting) + 1;
        }
        depths.put(nodeId, depth);
        return depth;
    }

    private static List<Body> withTracks(List<Body> placed, List<Track> tracks) {
        Map<String, List<Integer>> membersByKey = new LinkedHashMap<>();
        Map<String, Body> firstByKey = new HashMap<>();
        for (Body body : placed) {
            if (!body.orbits()) {
                continue;
            }
            String key = body.centreIndex() + "|" + body.degreesPerSecond() + "|" + Math.round(body.orbitRadius() / SAME_TRACK_TOLERANCE);
            membersByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(body.index());
            firstByKey.putIfAbsent(key, body);
        }
        Integer[] trackOfBody = new Integer[placed.size()];
        for (Map.Entry<String, List<Integer>> entry : membersByKey.entrySet()) {
            Body first = firstByKey.get(entry.getKey());
            int trackIndex = tracks.size();
            tracks.add(new Track(trackIndex, first.centreIndex(), first.orbitRadius(), first.degreesPerSecond(), List.copyOf(entry.getValue())));
            for (int member : entry.getValue()) {
                trackOfBody[member] = trackIndex;
            }
        }
        List<Body> bodies = new ArrayList<>(placed.size());
        for (Body body : placed) {
            Integer trackIndex = trackOfBody[body.index()];
            bodies.add(trackIndex == null ? body : new Body(body.nodeId(), body.index(), body.centreIndex(), body.degreesPerSecond(), body.startX(),
                    body.startY(), body.offsetX(), body.offsetY(), body.orbitRadius(), trackIndex, body.art(), body.colour()));
        }
        return bodies;
    }

    private static boolean sameFrame(Body first, Body second) {
        return first.centreIndex() == second.centreIndex() && first.degreesPerSecond() == second.degreesPerSecond();
    }

    private static Link classify(List<Body> bodies, Body first, Body second) {
        if (sameFrame(first, second)) {
            boolean sameTrack = first.orbits() && Math.abs(first.orbitRadius() - second.orbitRadius()) < SAME_TRACK_TOLERANCE;
            return new Link(sameTrack ? LinkKind.ARC : LinkKind.STRAIGHT, first.index(), second.index(), sameTrack ? first.trackIndex() : -1);
        }
        if (first.centreIndex() == second.index() || second.centreIndex() == first.index()) {
            return straight(first, second);
        }
        if (first.orbits() && first.centreIndex() == second.centreIndex()) {
            if (Math.abs(first.orbitRadius() - second.orbitRadius()) < SAME_TRACK_TOLERANCE) {
                return straight(first, second);
            }
            Body anchor = first.orbitRadius() > second.orbitRadius() ? first : second;
            Body landing = anchor == first ? second : first;
            return new Link(LinkKind.TETHER, anchor.index(), landing.index(), landing.trackIndex());
        }
        if (second.orbits() && sameFrame(bodies.get(second.centreIndex()), first)) {
            return new Link(LinkKind.TETHER, first.index(), second.index(), second.trackIndex());
        }
        if (first.orbits() && sameFrame(bodies.get(first.centreIndex()), second)) {
            return new Link(LinkKind.TETHER, second.index(), first.index(), first.trackIndex());
        }
        return straight(first, second);
    }

    private static Link straight(Body first, Body second) {
        return new Link(LinkKind.STRAIGHT, first.index(), second.index(), -1);
    }

    private static Map<String, Set<String>> neighboursAcrossTethers(List<Body> bodies, List<Track> tracks, List<Link> links) {
        Map<String, Set<String>> neighbours = new HashMap<>();
        for (Link link : links) {
            if (link.kind() != LinkKind.TETHER) {
                continue;
            }
            String anchorId = bodies.get(link.first()).nodeId();
            for (int member : tracks.get(link.trackIndex()).memberIndexes()) {
                String memberId = bodies.get(member).nodeId();
                neighbours.computeIfAbsent(anchorId, ignored -> new LinkedHashSet<>()).add(memberId);
                neighbours.computeIfAbsent(memberId, ignored -> new LinkedHashSet<>()).add(anchorId);
            }
        }
        Map<String, Set<String>> frozen = new HashMap<>();
        neighbours.forEach((nodeId, ids) -> frozen.put(nodeId, Set.copyOf(ids)));
        return frozen;
    }

    public List<Body> bodies() {
        return bodies;
    }

    public Body body(String nodeId) {
        Integer index = indexById.get(nodeId);
        return index == null ? null : bodies.get(index);
    }

    public List<Track> tracks() {
        return tracks;
    }

    public List<Link> links() {
        return links;
    }

    public Set<String> neighboursAcrossTethers(String nodeId) {
        return trackNeighbours.getOrDefault(nodeId, Set.of());
    }

    public Positions newPositions() {
        return new Positions(bodies.size());
    }

    public void positionsAt(double seconds, Positions out) {
        for (Body body : bodies) {
            int index = body.index();
            if (!body.orbits()) {
                out.xs[index] = body.startX();
                out.ys[index] = body.startY();
                out.turns[index] = 0.0;
                continue;
            }
            int centre = body.centreIndex();
            double turn = (out.turns[centre] + body.degreesPerSecond() * seconds) % FULL_TURN;
            double radians = Math.toRadians(turn);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            out.turns[index] = turn;
            out.xs[index] = (float) (out.xs[centre] + body.offsetX() * cos - body.offsetY() * sin);
            out.ys[index] = (float) (out.ys[centre] + body.offsetX() * sin + body.offsetY() * cos);
        }
    }
}
