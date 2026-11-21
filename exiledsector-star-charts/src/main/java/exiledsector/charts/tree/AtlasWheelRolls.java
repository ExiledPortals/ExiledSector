package exiledsector.charts.tree;

import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.charts.tree.AtlasTreeBodies.Link;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class AtlasWheelRolls {

    private static final int ATTEMPTS = 10;
    private static final double CALM_TOTAL_DEGREES = 200;
    private static final double CALM_TOTAL_SPREAD_DEGREES = 60;
    private static final double WILD_TOTAL_DEGREES = 500;
    private static final double WILD_TOTAL_SPREAD_DEGREES = 200;
    private static final double FIRST_SHARE_LOW = 0.2;
    private static final double FIRST_SHARE_HIGH = 0.45;
    private static final double LATER_SHARE_LOW = 0.22;
    private static final double LATER_SHARE_HIGH = 0.4;
    static final double FIRST_GAP = Math.toRadians(45);
    static final double MIDDLE_GAP = Math.toRadians(80);
    static final double LAST_GAP = Math.toRadians(100);
    private static final double FULL_TURN = Math.PI * 2;
    private static final double MIN_TURN_RATE = 0.15;
    private static final double ENTRY_SHARE_OF_FIRST_GAP = 0.8;
    private static final double ENTRY_HUG = 60;
    private static final double ENTRY_HUG_SHARE = 0.3;
    private static final double DEPARTURE_SPREAD = Math.toRadians(20);
    private static final double CALMEST_DEPARTURE = Math.toRadians(30);
    private static final double SAMPLE_STEP = Math.toRadians(3);
    private static final double SAMPLE_LENGTH = 20;
    static final float BODY_CLEARANCE = 30f;
    static final float LINK_CLEARANCE = 40f;
    private static final double FORK_SPREAD = Math.sin(Math.toRadians(8));

    private AtlasWheelRolls() {
    }

    static final class Placement {

        private final AtlasWheelSpiral spiral;
        private final List<Integer> chain;
        private final Map<Integer, Double> sweepByBody;
        private Placement(AtlasWheelSpiral spiral, List<Integer> chain, Map<Integer, Double> sweepByBody) {
            this.spiral = spiral;
            this.chain = chain;
            this.sweepByBody = sweepByBody;
        }

        List<Integer> members() {
            return chain.subList(1, chain.size() - 1);
        }

        boolean holds(int body) {
            return sweepByBody.containsKey(body);
        }

        double sweepOf(int body) {
            return sweepByBody.get(body);
        }

        double totalSweep() {
            return spiral.totalSweep();
        }

        float x(int body) {
            return spiral.xAt(sweepByBody.get(body));
        }

        float y(int body) {
            return spiral.yAt(sweepByBody.get(body));
        }

        AtlasLinkPath path(int from, int to) {
            return AtlasLinkPath.alongSpiral(spiral, sweepByBody.get(from), sweepByBody.get(to));
        }
    }

    private record Wheel(int centre, float centreX, float centreY, List<Integer> chain, double[] radii, float[] sizes, double entryAngle, int turn) {

        double reach() {
            float largest = 0f;
            for (float size : sizes) {
                largest = Math.max(largest, size);
            }
            return radii[0] + largest;
        }
    }

    private record Disc(float x, float y, float radius) {
    }

    private record Segment(float fromX, float fromY, float toX, float toY, boolean touchesEntry) {
    }

    static Map<Integer, Placement> roll(AtlasTreeBodies bodies, Random random) {
        List<Wheel> wheels = wheelsOf(bodies);
        Collections.shuffle(wheels, random);
        Scene scene = new Scene(bodies);
        Map<Integer, Placement> placements = new HashMap<>();
        for (Wheel wheel : wheels) {
            Placement placement = place(wheel, scene.around(wheel), random);
            if (placement != null) {
                placements.put(wheel.centre(), placement);
                scene.move(placement);
            }
        }
        return placements;
    }

    private static List<Wheel> wheelsOf(AtlasTreeBodies bodies) {
        List<List<Integer>> neighbours = new ArrayList<>();
        for (int body = 0; body < bodies.bodies().size(); body++) {
            neighbours.add(new ArrayList<>());
        }
        for (Link link : bodies.links()) {
            neighbours.get(link.first()).add(link.second());
            neighbours.get(link.second()).add(link.first());
        }
        Map<Integer, Set<Integer>> membersByCentre = new HashMap<>();
        for (Body body : bodies.bodies()) {
            if (body.wheel() != AtlasTreeBodies.NO_WHEEL) {
                membersByCentre.computeIfAbsent(body.wheel(), centre -> new HashSet<>()).add(body.index());
            }
        }
        List<Wheel> wheels = new ArrayList<>();
        membersByCentre.forEach((centre, members) -> {
            List<Integer> chain = chainOf(centre, members, neighbours);
            Wheel wheel = chain == null ? null : wheelOf(bodies, centre, chain);
            if (wheel != null) {
                wheels.add(wheel);
            }
        });
        wheels.sort((first, second) -> Integer.compare(first.centre(), second.centre()));
        return wheels;
    }

    private static List<Integer> chainOf(int centre, Set<Integer> members, List<List<Integer>> neighbours) {
        if (neighbours.get(centre).size() != 1) {
            return null;
        }
        Deque<Integer> chain = new ArrayDeque<>();
        chain.addFirst(centre);
        int previous = centre;
        int at = neighbours.get(centre).get(0);
        while (members.contains(at)) {
            List<Integer> next = neighbours.get(at);
            if (next.size() != 2 || chain.contains(at)) {
                return null;
            }
            chain.addFirst(at);
            int following = next.get(0) == previous ? next.get(1) : next.get(0);
            previous = at;
            at = following;
        }
        if (chain.size() != members.size() + 1 || chain.size() < 2 || at == centre) {
            return null;
        }
        chain.addFirst(at);
        return List.copyOf(chain);
    }

    private static Wheel wheelOf(AtlasTreeBodies bodies, int centre, List<Integer> chain) {
        Body hub = bodies.bodies().get(centre);
        double[] radii = new double[chain.size()];
        float[] sizes = new float[chain.size()];
        for (int step = 0; step < chain.size(); step++) {
            Body body = bodies.bodies().get(chain.get(step));
            radii[step] = step == chain.size() - 1 ? 0 : Math.hypot(body.x() - hub.x(), body.y() - hub.y());
            sizes[step] = body.art().size();
            if (step > 0 && radii[step] >= radii[step - 1]) {
                return null;
            }
        }
        Body entry = bodies.bodies().get(chain.get(0));
        return new Wheel(centre, hub.x(), hub.y(), chain, radii, sizes, Math.atan2(entry.y() - hub.y(), entry.x() - hub.x()),
                hub.spiral() == 0 ? 1 : hub.spiral());
    }

    private static Placement place(Wheel wheel, Obstacles obstacles, Random random) {
        double drama = random.nextDouble();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double tamed = drama * (ATTEMPTS - 1 - attempt) / (ATTEMPTS - 1);
            Placement candidate = candidate(wheel, tamed, random);
            if (obstacles.clearOf(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Placement candidate(Wheel wheel, double drama, Random random) {
        int gaps = wheel.chain().size() - 1;
        double total = Math.toRadians(lerp(CALM_TOTAL_DEGREES, WILD_TOTAL_DEGREES, drama)
                + random.nextDouble() * lerp(CALM_TOTAL_SPREAD_DEGREES, WILD_TOTAL_SPREAD_DEGREES, drama));
        double[] shares = new double[gaps];
        double shareSum = 0;
        for (int gap = 0; gap < gaps; gap++) {
            shares[gap] = gap == 0 ? between(random, FIRST_SHARE_LOW, FIRST_SHARE_HIGH) : between(random, LATER_SHARE_LOW, LATER_SHARE_HIGH);
            shareSum += shares[gap];
        }
        double[] radii = wheel.radii();
        double hugRadius = radii[1] + Math.min(ENTRY_HUG, (radii[0] - radii[1]) * ENTRY_HUG_SHARE);
        double steepest = AtlasWheelSpiral.steepestDeparture(radii[0], hugRadius);
        double departurePitch = Math.max(Math.min(steepest, CALMEST_DEPARTURE), steepest - random.nextDouble() * DEPARTURE_SPREAD);
        double firstGap = Math.max(FIRST_GAP, AtlasWheelSpiral.longestEntrySweep(radii[0], hugRadius, departurePitch) / ENTRY_SHARE_OF_FIRST_GAP);
        double[] gapSweeps = new double[gaps];
        double used = 0;
        for (int gap = 0; gap + 1 < gaps; gap++) {
            gapSweeps[gap] = Math.max(gap == 0 ? firstGap : MIDDLE_GAP, total * shares[gap] / shareSum);
            used += gapSweeps[gap];
        }
        gapSweeps[gaps - 1] = Math.max(LAST_GAP, total - used);
        gapSweeps[0] = Math.max(gapSweeps[0], firstGap);
        double[] orbitSweeps = new double[gaps + 1];
        Map<Integer, Double> sweepByBody = new HashMap<>();
        sweepByBody.put(wheel.chain().get(0), 0.0);
        for (int gap = 0; gap < gaps; gap++) {
            orbitSweeps[gap + 1] = orbitSweeps[gap] + gapSweeps[gap];
            sweepByBody.put(wheel.chain().get(gap + 1), orbitSweeps[gap + 1]);
        }
        AtlasWheelSpiral spiral = AtlasWheelSpiral.wound(wheel.centreX(), wheel.centreY(), wheel.entryAngle(), wheel.turn(), departurePitch, hugRadius, orbitSweeps, radii);
        return new Placement(spiral, wheel.chain(), sweepByBody);
    }

    private static double lerp(double from, double to, double share) {
        return from + (to - from) * share;
    }

    private static double between(Random random, double low, double high) {
        return low + random.nextDouble() * (high - low);
    }

    private static final class Scene {

        private final AtlasTreeBodies bodies;
        private final float[] xs;
        private final float[] ys;
        private final List<AtlasLinkPath> paths = new ArrayList<>();

        Scene(AtlasTreeBodies bodies) {
            this.bodies = bodies;
            int count = bodies.bodies().size();
            this.xs = new float[count];
            this.ys = new float[count];
            for (Body body : bodies.bodies()) {
                xs[body.index()] = body.x();
                ys[body.index()] = body.y();
            }
            for (Link link : bodies.links()) {
                paths.add(link.path());
            }
        }

        Obstacles around(Wheel wheel) {
            Set<Integer> own = Set.copyOf(wheel.chain());
            int entry = wheel.chain().get(0);
            double reach = wheel.reach();
            List<Disc> discs = new ArrayList<>();
            for (Body body : bodies.bodies()) {
                float size = body.art().size();
                if (!own.contains(body.index())
                        && Math.hypot(xs[body.index()] - wheel.centreX(), ys[body.index()] - wheel.centreY()) - size < reach + BODY_CLEARANCE) {
                    discs.add(new Disc(xs[body.index()], ys[body.index()], size));
                }
            }
            List<Segment> segments = new ArrayList<>();
            for (int index = 0; index < paths.size(); index++) {
                Link link = bodies.links().get(index);
                if (own.contains(link.first()) && own.contains(link.second())) {
                    continue;
                }
                boolean touchesEntry = link.first() == entry || link.second() == entry;
                AtlasLinkPath path = paths.get(index);
                for (int point = 0; point + 1 < path.size(); point++) {
                    Segment segment = new Segment(path.x(point), path.y(point), path.x(point + 1), path.y(point + 1), touchesEntry);
                    if (toSegment(wheel.centreX(), wheel.centreY(), segment.fromX(), segment.fromY(), segment.toX(), segment.toY()) < reach + LINK_CLEARANCE) {
                        segments.add(segment);
                    }
                }
            }
            return new Obstacles(wheel, discs, segments);
        }

        void move(Placement placement) {
            for (int member : placement.members()) {
                xs[member] = placement.x(member);
                ys[member] = placement.y(member);
            }
            for (int index = 0; index < paths.size(); index++) {
                Link link = bodies.links().get(index);
                if (placement.holds(link.first()) && placement.holds(link.second())) {
                    paths.set(index, placement.path(link.first(), link.second()));
                }
            }
        }
    }

    private record Trace(double[] sweeps, float[] xs, float[] ys) {

        static Trace of(AtlasWheelSpiral spiral) {
            double[] sweeps = spiral.sampleSweeps(0, spiral.totalSweep(), SAMPLE_STEP, SAMPLE_LENGTH);
            float[] xs = new float[sweeps.length];
            float[] ys = new float[sweeps.length];
            for (int point = 0; point < sweeps.length; point++) {
                xs[point] = spiral.xAt(sweeps[point]);
                ys[point] = spiral.yAt(sweeps[point]);
            }
            return new Trace(sweeps, xs, ys);
        }

        int nearest(double sweep) {
            int found = Arrays.binarySearch(sweeps, sweep);
            if (found >= 0) {
                return found;
            }
            int after = Math.min(sweeps.length - 1, -found - 1);
            int before = Math.max(0, after - 1);
            return sweep - sweeps[before] <= sweeps[after] - sweep ? before : after;
        }
    }

    private record Obstacles(Wheel wheel, List<Disc> discs, List<Segment> segments) {

        boolean clearOf(Placement placement) {
            Trace trace = Trace.of(placement.spiral);
            float[] xs = trace.xs();
            float[] ys = trace.ys();
            if (!turnsOneWay(trace)) {
                return false;
            }
            List<Integer> chain = wheel.chain();
            for (int step = 0; step + 1 < chain.size(); step++) {
                int body = chain.get(step);
                int nearest = trace.nearest(placement.sweepOf(body));
                if (!leavesAndStaysClear(placement.x(body), placement.y(body), wheel.sizes()[step] + BODY_CLEARANCE, nearest, xs, ys)) {
                    return false;
                }
            }
            for (int first = 1; first + 1 < chain.size(); first++) {
                for (int second = first + 1; second + 1 < chain.size(); second++) {
                    double apart = Math.hypot(placement.x(chain.get(first)) - placement.x(chain.get(second)),
                            placement.y(chain.get(first)) - placement.y(chain.get(second)));
                    if (apart < wheel.sizes()[first] + wheel.sizes()[second] + BODY_CLEARANCE) {
                        return false;
                    }
                }
            }
            return clearOfDiscs(placement, xs, ys) && clearOfSegments(placement, xs, ys);
        }

        private boolean clearOfDiscs(Placement placement, float[] xs, float[] ys) {
            List<Integer> chain = wheel.chain();
            for (Disc disc : discs) {
                for (int point = 0; point + 1 < xs.length; point++) {
                    if (toSegment(disc.x(), disc.y(), xs[point], ys[point], xs[point + 1], ys[point + 1]) < disc.radius() + BODY_CLEARANCE) {
                        return false;
                    }
                }
                for (int step = 1; step + 1 < chain.size(); step++) {
                    int member = chain.get(step);
                    if (Math.hypot(placement.x(member) - disc.x(), placement.y(member) - disc.y()) < wheel.sizes()[step] + disc.radius() + BODY_CLEARANCE) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean clearOfSegments(Placement placement, float[] xs, float[] ys) {
            List<Integer> chain = wheel.chain();
            for (Segment segment : segments) {
                for (int point = 0; point + 1 < xs.length; point++) {
                    if (betweenSegments(xs[point], ys[point], xs[point + 1], ys[point + 1], segment) < clearanceFrom(segment, xs, ys, point)) {
                        return false;
                    }
                }
                for (int step = 1; step + 1 < chain.size(); step++) {
                    int member = chain.get(step);
                    if (toSegment(placement.x(member), placement.y(member), segment.fromX(), segment.fromY(), segment.toX(), segment.toY())
                            < wheel.sizes()[step] + BODY_CLEARANCE) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static double clearanceFrom(Segment segment, float[] xs, float[] ys, int point) {
            if (!segment.touchesEntry()) {
                return LINK_CLEARANCE;
            }
            double fromEntry = toSegment(xs[0], ys[0], xs[point], ys[point], xs[point + 1], ys[point + 1]);
            return Math.min(LINK_CLEARANCE, fromEntry * FORK_SPREAD);
        }

        private boolean turnsOneWay(Trace trace) {
            float[] xs = trace.xs();
            float[] ys = trace.ys();
            double[] sweeps = trace.sweeps();
            for (int point = 1; point + 1 < xs.length; point++) {
                if (xs[point] == xs[point + 1] && ys[point] == ys[point + 1] || xs[point] == xs[point - 1] && ys[point] == ys[point - 1]) {
                    continue;
                }
                double before = Math.atan2(ys[point] - ys[point - 1], xs[point] - xs[point - 1]);
                double after = Math.atan2(ys[point + 1] - ys[point], xs[point + 1] - xs[point]);
                double bend = Math.IEEEremainder(after - before, FULL_TURN) * wheel.turn();
                if (bend < MIN_TURN_RATE * (sweeps[point + 1] - sweeps[point - 1]) / 2) {
                    return false;
                }
            }
            return true;
        }

        private static boolean leavesAndStaysClear(float bodyX, float bodyY, double clearance, int nearest, float[] xs, float[] ys) {
            for (int direction = -1; direction <= 1; direction += 2) {
                boolean left = false;
                for (int point = nearest; point + direction >= 0 && point + direction < xs.length; point += direction) {
                    int next = point + direction;
                    double distance = toSegment(bodyX, bodyY, xs[point], ys[point], xs[next], ys[next]);
                    if (!left) {
                        left = distance > clearance;
                    } else if (distance < clearance) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    static double toSegment(double pointX, double pointY, double fromX, double fromY, double toX, double toY) {
        double deltaX = toX - fromX;
        double deltaY = toY - fromY;
        double lengthSquared = deltaX * deltaX + deltaY * deltaY;
        double share = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1, ((pointX - fromX) * deltaX + (pointY - fromY) * deltaY) / lengthSquared));
        return Math.hypot(pointX - (fromX + share * deltaX), pointY - (fromY + share * deltaY));
    }

    private static double betweenSegments(float fromX, float fromY, float toX, float toY, Segment other) {
        if (crosses(fromX, fromY, toX, toY, other)) {
            return 0;
        }
        return Math.min(Math.min(toSegment(fromX, fromY, other.fromX(), other.fromY(), other.toX(), other.toY()),
                        toSegment(toX, toY, other.fromX(), other.fromY(), other.toX(), other.toY())),
                Math.min(toSegment(other.fromX(), other.fromY(), fromX, fromY, toX, toY),
                        toSegment(other.toX(), other.toY(), fromX, fromY, toX, toY)));
    }

    private static boolean crosses(float fromX, float fromY, float toX, float toY, Segment other) {
        double first = side(fromX, fromY, toX, toY, other.fromX(), other.fromY());
        double second = side(fromX, fromY, toX, toY, other.toX(), other.toY());
        double third = side(other.fromX(), other.fromY(), other.toX(), other.toY(), fromX, fromY);
        double fourth = side(other.fromX(), other.fromY(), other.toX(), other.toY(), toX, toY);
        return first * second < 0 && third * fourth < 0;
    }

    private static double side(double fromX, double fromY, double toX, double toY, double pointX, double pointY) {
        return (toX - fromX) * (pointY - fromY) - (toY - fromY) * (pointX - fromX);
    }
}
