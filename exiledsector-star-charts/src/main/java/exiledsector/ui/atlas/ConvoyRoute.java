package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasLinkPath;
import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.charts.tree.AtlasTreeBodies.Link;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

final class ConvoyRoute {

    private static final float SAME_POINT = 0.01f;

    private record Reach(double distance, int bodyIndex) {
    }

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
        return alongLinks(port, centre, linksBetween(bodies, port, centre));
    }

    static ConvoyRoute acrossTheAtlas(AtlasTreeBodies bodies, Body from, Body to) {
        return alongLinks(from, to, shortestLinks(bodies, from, to));
    }

    private static ConvoyRoute alongLinks(Body from, Body to, List<Link> links) {
        if (links.isEmpty()) {
            return through(new float[]{from.x(), to.x()}, new float[]{from.y(), to.y()});
        }
        List<Float> xs = new ArrayList<>();
        List<Float> ys = new ArrayList<>();
        int at = from.index();
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
        return walkBack(arrivedBy, centre.index());
    }

    private static List<Link> shortestLinks(AtlasTreeBodies bodies, Body from, Body to) {
        int count = bodies.bodies().size();
        int starIndex = bodies.star() == null ? -1 : bodies.star().index();
        List<List<Link>> linksAt = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            linksAt.add(new ArrayList<>());
        }
        for (Link link : bodies.links()) {
            linksAt.get(link.first()).add(link);
            linksAt.get(link.second()).add(link);
        }
        double[] distances = new double[count];
        Arrays.fill(distances, Double.MAX_VALUE);
        distances[from.index()] = 0.0;
        Map<Integer, Link> arrivedBy = new HashMap<>();
        arrivedBy.put(from.index(), null);
        PriorityQueue<Reach> frontier = new PriorityQueue<>((first, second) -> Double.compare(first.distance(), second.distance()));
        frontier.add(new Reach(0.0, from.index()));
        while (!frontier.isEmpty()) {
            Reach reach = frontier.poll();
            int at = reach.bodyIndex();
            if (at == to.index()) {
                break;
            }
            if (reach.distance() > distances[at]) {
                continue;
            }
            for (Link link : linksAt.get(at)) {
                int other = link.first() == at ? link.second() : link.first();
                double reached = distances[at] + lengthOf(link.path());
                boolean passable = other == to.index() || bodies.bodies().get(other).highway();
                if (other != starIndex && passable && reached < distances[other]) {
                    distances[other] = reached;
                    arrivedBy.put(other, link);
                    frontier.add(new Reach(reached, other));
                }
            }
        }
        return arrivedBy.containsKey(to.index()) ? walkBack(arrivedBy, to.index()) : List.of();
    }

    private static double lengthOf(AtlasLinkPath path) {
        double length = 0.0;
        for (int point = 1; point < path.size(); point++) {
            length += Math.hypot(path.x(point) - path.x(point - 1), path.y(point) - path.y(point - 1));
        }
        return length;
    }

    private static List<Link> walkBack(Map<Integer, Link> arrivedBy, int end) {
        List<Link> route = new ArrayList<>();
        Integer at = end;
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
