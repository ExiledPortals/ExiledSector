package exiledsector.charts.tree;

import exiledsector.skills.layout.TreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AtlasTreeBodies {

    private static final float SAME_RING_TOLERANCE = 1f;

    public record Body(String nodeId, int index, float x, float y, boolean highway, AtlasBodyArt art, String colour) {
    }

    public record Link(int first, int second, boolean alongRing) {
    }

    private final List<Body> bodies;
    private final Map<String, Integer> indexById;
    private final List<Link> links;
    private final Body star;

    private AtlasTreeBodies(List<Body> bodies, List<Link> links, Body star) {
        this.bodies = List.copyOf(bodies);
        this.links = List.copyOf(links);
        this.star = star;
        Map<String, Integer> indexes = new HashMap<>();
        for (Body body : bodies) {
            indexes.put(body.nodeId(), body.index());
        }
        this.indexById = Map.copyOf(indexes);
    }

    public static AtlasTreeBodies of(Collection<? extends TreeNode> nodes, Map<String, AtlasBody> bodiesById) {
        List<Body> bodies = new ArrayList<>();
        Map<String, Integer> indexes = new HashMap<>();
        Body star = null;
        for (TreeNode node : nodes) {
            if (indexes.containsKey(node.getId())) {
                continue;
            }
            AtlasBody entry = bodiesById.get(node.getId());
            AtlasBodyArt art = entry == null || entry.art() == null ? AtlasBodyArt.PLAIN : entry.art();
            Body body = new Body(node.getId(), bodies.size(), node.getX(), node.getY(), entry != null && entry.highway(), art,
                    entry == null ? null : entry.colour());
            indexes.put(node.getId(), body.index());
            bodies.add(body);
            if (star == null && art.kind() == AtlasBodyArt.Kind.STAR) {
                star = body;
            }
        }
        List<Link> links = new ArrayList<>();
        Set<Long> seenPairs = new HashSet<>();
        for (TreeNode node : nodes) {
            int index = indexes.get(node.getId());
            for (String otherId : node.getConnectedNodeIds()) {
                Integer otherIndex = indexes.get(otherId);
                if (otherIndex == null || otherIndex == index) {
                    continue;
                }
                int low = Math.min(index, otherIndex);
                int high = Math.max(index, otherIndex);
                if (seenPairs.add(((long) low << 32) | high)) {
                    links.add(new Link(low, high, alongRing(bodies.get(low), bodies.get(high), star)));
                }
            }
        }
        return new AtlasTreeBodies(bodies, links, star);
    }

    private static boolean alongRing(Body first, Body second, Body star) {
        return star != null && first.highway() && second.highway()
                && Math.abs(distanceFrom(star, first) - distanceFrom(star, second)) < SAME_RING_TOLERANCE;
    }

    private static float distanceFrom(Body centre, Body body) {
        return (float) Math.hypot(body.x() - centre.x(), body.y() - centre.y());
    }

    public List<Body> bodies() {
        return bodies;
    }

    public Body body(String nodeId) {
        Integer index = indexById.get(nodeId);
        return index == null ? null : bodies.get(index);
    }

    public List<Link> links() {
        return links;
    }

    public Body star() {
        return star;
    }

    public float ringRadius(Link link) {
        return distanceFrom(star, bodies.get(link.first()));
    }
}
