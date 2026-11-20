package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasLinkPath;
import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.charts.tree.AtlasTreeBodies.Link;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ConvoyRoute {

    private static final float SAME_POINT = 0.01f;

    private final float[] xs;
    private final float[] ys;

    private ConvoyRoute(float[] xs, float[] ys) {
        this.xs = xs;
        this.ys = ys;
    }

    static ConvoyRoute through(float[] xs, float[] ys) {
        return new ConvoyRoute(xs.clone(), ys.clone());
    }

    static ConvoyRoute fromPortToWheelCentre(AtlasTreeBodies bodies, Body port) {
        Body centre = bodies.bodies().get(port.wheel());
        List<Link> links = linksBetween(bodies, port, centre);
        if (links.isEmpty()) {
            return through(new float[]{port.x(), centre.x()}, new float[]{port.y(), centre.y()});
        }
        List<Float> xs = new ArrayList<>();
        List<Float> ys = new ArrayList<>();
        int at = port.index();
        for (Link link : links) {
            boolean forward = link.first() == at;
            AtlasLinkPath path = link.path();
            for (int step = 0; step < path.size(); step++) {
                int point = forward ? step : path.size() - 1 - step;
                addPoint(xs, ys, path.x(point), path.y(point));
            }
            at = forward ? link.second() : link.first();
        }
        return new ConvoyRoute(toArray(xs), toArray(ys));
    }

    private static List<Link> linksBetween(AtlasTreeBodies bodies, Body port, Body centre) {
        Map<Integer, Link> arrivedBy = new HashMap<>();
        Deque<Integer> frontier = new ArrayDeque<>();
        frontier.add(port.index());
        arrivedBy.put(port.index(), null);
        while (!frontier.isEmpty() && !arrivedBy.containsKey(centre.index())) {
            int at = frontier.poll();
            for (Link link : bodies.links()) {
                int other = link.first() == at ? link.second() : link.second() == at ? link.first() : -1;
                if (other >= 0 && !arrivedBy.containsKey(other) && inWheel(bodies.bodies().get(other), centre)) {
                    arrivedBy.put(other, link);
                    frontier.add(other);
                }
            }
        }
        List<Link> route = new ArrayList<>();
        Integer at = centre.index();
        while (arrivedBy.get(at) != null) {
            Link link = arrivedBy.get(at);
            route.add(0, link);
            at = link.first() == at ? link.second() : link.first();
        }
        return route;
    }

    private static boolean inWheel(Body body, Body centre) {
        return body.index() == centre.index() || body.wheel() == centre.index();
    }

    private static void addPoint(List<Float> xs, List<Float> ys, float x, float y) {
        int last = xs.size() - 1;
        if (last >= 0 && Math.abs(xs.get(last) - x) < SAME_POINT && Math.abs(ys.get(last) - y) < SAME_POINT) {
            return;
        }
        xs.add(x);
        ys.add(y);
    }

    private static float[] toArray(List<Float> values) {
        float[] array = new float[values.size()];
        for (int index = 0; index < array.length; index++) {
            array[index] = values.get(index);
        }
        return array;
    }

    int size() {
        return xs.length;
    }

    float x(int point) {
        return xs[point];
    }

    float y(int point) {
        return ys[point];
    }
}
