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
    public static final int NO_WHEEL = -1;

    public record Body(String nodeId, int index, float x, float y, boolean highway, AtlasBodyArt art, String colour, int wheel, int spiral) {
    }

    public enum Shape {
        STRAIGHT,
        BOWED,
        ALONG_RING,
        SPIRAL
    }

    public record Link(int first, int second, Shape shape, AtlasLinkPath path) {

        public boolean alongRing() {
            return shape == Shape.ALONG_RING;
        }
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
        Map<String, Integer> indexes = new HashMap<>();
        List<TreeNode> unique = new ArrayList<>();
        for (TreeNode node : nodes) {
            if (indexes.putIfAbsent(node.getId(), unique.size()) == null) {
                unique.add(node);
            }
        }
        List<Body> bodies = new ArrayList<>();
        Body star = null;
        for (TreeNode node : unique) {
            AtlasBody entry = bodiesById.get(node.getId());
            AtlasBodyArt art = entry == null || entry.art() == null ? AtlasBodyArt.PLAIN : entry.art();
            Integer wheel = entry == null || entry.wheel() == null ? null : indexes.get(entry.wheel());
            Body body = new Body(node.getId(), bodies.size(), node.getX(), node.getY(), entry != null && entry.highway(), art,
                    entry == null ? null : entry.colour(), wheel == null ? NO_WHEEL : wheel, entry == null ? 0 : entry.spiral());
            bodies.add(body);
            if (star == null && art.kind() == AtlasBodyArt.Kind.STAR) {
                star = body;
            }
        }
        List<Link> links = new ArrayList<>();
        Set<Long> seenPairs = new HashSet<>();
        for (TreeNode node : unique) {
            int index = indexes.get(node.getId());
            for (String otherId : node.getConnectedNodeIds()) {
                Integer otherIndex = indexes.get(otherId);
                if (otherIndex == null || otherIndex == index) {
                    continue;
                }
                int low = Math.min(index, otherIndex);
                int high = Math.max(index, otherIndex);
                if (seenPairs.add(((long) low << 32) | high)) {
                    links.add(link(bodies, bodies.get(low), bodies.get(high), star));
                }
            }
        }
        return new AtlasTreeBodies(bodies, links, star);
    }

    private static Link link(List<Body> bodies, Body first, Body second, Body star) {
        if (alongRing(first, second, star)) {
            return new Link(first.index(), second.index(), Shape.ALONG_RING, AtlasLinkPath.arc(star.x(), star.y(), distanceFrom(star, first),
                    first.x(), first.y(), second.x(), second.y()));
        }
        int wheel = first.wheel() != NO_WHEEL ? first.wheel() : second.wheel();
        if (wheel != NO_WHEEL) {
            Body centre = bodies.get(wheel);
            int turn = centre.spiral() == 0 ? 1 : centre.spiral();
            return new Link(first.index(), second.index(), Shape.SPIRAL, AtlasLinkPath.spiral(centre.x(), centre.y(), turn, first.x(), first.y(),
                    second.x(), second.y()));
        }
        if (star != null) {
            return new Link(first.index(), second.index(), Shape.BOWED, AtlasLinkPath.bowed(first.x(), first.y(), second.x(), second.y(), star.x(), star.y()));
        }
        return new Link(first.index(), second.index(), Shape.STRAIGHT, AtlasLinkPath.straight(first.x(), first.y(), second.x(), second.y()));
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
