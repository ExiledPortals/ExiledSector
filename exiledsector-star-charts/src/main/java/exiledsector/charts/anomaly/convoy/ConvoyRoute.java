package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class ConvoyRoute {

    static final float ENTRY_SPREAD_DEGREES = 90f;
    static final float EXIT_SPREAD_DEGREES = 120f;

    record Plan(Vector2f entry, Vector2f exit, List<SectorEntityToken> waypoints, SectorEntityToken destination) {

        Plan {
            waypoints = List.copyOf(waypoints);
        }
    }

    private ConvoyRoute() {
    }

    static Plan plan(List<SectorEntityToken> planets, List<SectorEntityToken> stations, Set<SectorEntityToken> takenDestinations, Random random) {
        List<SectorEntityToken> freePlanets = planets.stream().filter(planet -> planet.getLocation() != null && !takenDestinations.contains(planet)).toList();
        List<SectorEntityToken> destinations = freePlanets.isEmpty() ? planets.stream().filter(planet -> planet.getLocation() != null).toList() : freePlanets;
        if (destinations.isEmpty()) {
            return null;
        }
        SectorEntityToken destination = destinations.get(random.nextInt(destinations.size()));
        List<SectorEntityToken> pool = new ArrayList<>();
        for (SectorEntityToken candidate : concat(planets, stations)) {
            if (candidate != destination && candidate.getLocation() != null && !pool.contains(candidate)) {
                pool.add(candidate);
            }
        }
        Collections.shuffle(pool, random);
        int wanted = ConvoyRules.MIN_WAYPOINTS + random.nextInt(ConvoyRules.MAX_WAYPOINTS - ConvoyRules.MIN_WAYPOINTS + 1);
        List<SectorEntityToken> picked = new ArrayList<>(pool.subList(0, Math.min(wanted, pool.size())));
        float edge = edgeRadius(concat(planets, stations));
        double destinationAngle = Math.atan2(destination.getLocation().y, destination.getLocation().x);
        Vector2f entry = atAngle(destinationAngle + Math.PI + spread(ENTRY_SPREAD_DEGREES, random), edge);
        Vector2f exit = atAngle(destinationAngle + spread(EXIT_SPREAD_DEGREES, random), edge);
        return new Plan(entry, exit, nearestFirst(entry, picked), destination);
    }

    private static List<SectorEntityToken> concat(List<SectorEntityToken> first, List<SectorEntityToken> second) {
        List<SectorEntityToken> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private static double spread(float degrees, Random random) {
        return Math.toRadians((random.nextFloat() - 0.5f) * degrees);
    }

    static float edgeRadius(List<SectorEntityToken> entities) {
        float farthest = 0f;
        for (SectorEntityToken entity : entities) {
            if (entity.getLocation() != null) {
                farthest = Math.max(farthest, entity.getLocation().length() + entity.getRadius());
            }
        }
        return farthest + ConvoyRules.EDGE_MARGIN;
    }

    static Vector2f atAngle(double radians, float radius) {
        return new Vector2f((float) Math.cos(radians) * radius, (float) Math.sin(radians) * radius);
    }

    static List<SectorEntityToken> nearestFirst(Vector2f start, List<SectorEntityToken> stops) {
        List<SectorEntityToken> left = new ArrayList<>(stops);
        List<SectorEntityToken> ordered = new ArrayList<>();
        Vector2f at = new Vector2f(start);
        while (!left.isEmpty()) {
            SectorEntityToken nearest = left.get(0);
            for (SectorEntityToken stop : left) {
                if (distance(at, stop.getLocation()) < distance(at, nearest.getLocation())) {
                    nearest = stop;
                }
            }
            left.remove(nearest);
            ordered.add(nearest);
            at = new Vector2f(nearest.getLocation());
        }
        return ordered;
    }

    static float distance(Vector2f from, Vector2f to) {
        return Vector2f.sub(to, from, new Vector2f()).length();
    }

    static float pathLength(Vector2f from, List<Vector2f> stops) {
        float length = 0f;
        Vector2f at = from;
        for (Vector2f stop : stops) {
            length += distance(at, stop);
            at = stop;
        }
        return length;
    }
}
