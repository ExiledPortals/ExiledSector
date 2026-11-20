package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.charts.tree.AtlasTreeBodies.Link;
import exiledsector.ui.TreeViewport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntToDoubleFunction;

final class ConvoyLongHauls {

    static final float SPEED = 110f;
    static final float MIN_WAIT_SECONDS = 120f;
    static final float EXTRA_WAIT_SECONDS = 180f;
    static final float WARM_UP_SECONDS = 300f;
    private static final float WARM_UP_STEP_SECONDS = 1f;

    record Terminal(Body body, List<List<String>> convoys, List<List<String>> haulers) {
    }

    static final class Trip {

        private final Terminal from;
        private final Terminal to;
        private final ConvoyRun run;
        private final ConvoyLooks looks;
        private float distance;

        private Trip(Terminal from, Terminal to, ConvoyRun run, ConvoyLooks looks) {
            this.from = from;
            this.to = to;
            this.run = run;
            this.looks = looks;
        }

        Terminal from() {
            return from;
        }

        Terminal to() {
            return to;
        }

        ConvoyRun run() {
            return run;
        }

        float distance() {
            return distance;
        }

        boolean arrived() {
            return distance >= run.length();
        }
    }

    private final AtlasTreeBodies bodies;
    private final List<Terminal> terminals;
    private final float[] departIn;
    private final Map<Long, ConvoyRun> runs = new HashMap<>();
    private final List<Trip> trips = new ArrayList<>();
    private final Random random;
    private long departures;

    ConvoyLongHauls(AtlasTreeBodies bodies, Random random) {
        this.bodies = bodies;
        this.random = random;
        this.terminals = terminalsOf(bodies);
        this.departIn = new float[terminals.size()];
        for (int index = 0; index < departIn.length; index++) {
            departIn[index] = random.nextFloat() * (MIN_WAIT_SECONDS + EXTRA_WAIT_SECONDS);
        }
        for (float warmed = 0f; warmed < WARM_UP_SECONDS; warmed += WARM_UP_STEP_SECONDS) {
            move(WARM_UP_STEP_SECONDS);
        }
    }

    static List<Terminal> terminalsOf(AtlasTreeBodies bodies) {
        Map<Integer, List<List<String>>> convoysByWheel = new LinkedHashMap<>();
        Map<Integer, List<List<String>>> haulersByWheel = new HashMap<>();
        for (Body port : bodies.bodies()) {
            if (port.wheel() != AtlasTreeBodies.NO_WHEEL && !port.art().convoys().isEmpty()) {
                convoysByWheel.computeIfAbsent(port.wheel(), wheel -> new ArrayList<>()).addAll(port.art().convoys());
                haulersByWheel.computeIfAbsent(port.wheel(), wheel -> new ArrayList<>()).addAll(port.art().haulers());
            }
        }
        List<Terminal> terminals = new ArrayList<>();
        for (Map.Entry<Integer, List<List<String>>> wheel : convoysByWheel.entrySet()) {
            Body gate = highwayGateOf(bodies, wheel.getKey());
            if (gate != null) {
                terminals.add(new Terminal(gate, List.copyOf(wheel.getValue()), List.copyOf(haulersByWheel.get(wheel.getKey()))));
            }
        }
        return terminals;
    }

    private static Body highwayGateOf(AtlasTreeBodies bodies, int wheel) {
        for (Link link : bodies.links()) {
            Body first = bodies.bodies().get(link.first());
            Body second = bodies.bodies().get(link.second());
            if (first.wheel() == wheel && second.highway() && second.wheel() == AtlasTreeBodies.NO_WHEEL) {
                return first;
            }
            if (second.wheel() == wheel && first.highway() && first.wheel() == AtlasTreeBodies.NO_WHEEL) {
                return second;
            }
        }
        return null;
    }

    List<Terminal> terminals() {
        return terminals;
    }

    List<Trip> trips() {
        return trips;
    }

    long departures() {
        return departures;
    }

    void advance(float amount) {
        move(amount);
        for (Trip trip : trips) {
            trip.looks.advance(amount, trip.run.along(trip.distance).headingDegrees());
        }
    }

    private void move(float amount) {
        Iterator<Trip> travelling = trips.iterator();
        while (travelling.hasNext()) {
            Trip trip = travelling.next();
            trip.distance += amount * SPEED;
            if (trip.arrived()) {
                travelling.remove();
            }
        }
        if (terminals.size() < 2) {
            return;
        }
        for (int index = 0; index < departIn.length; index++) {
            departIn[index] -= amount;
            if (departIn[index] <= 0f) {
                depart(index);
                departIn[index] = MIN_WAIT_SECONDS + random.nextFloat() * EXTRA_WAIT_SECONDS;
            }
        }
    }

    private void depart(int fromIndex) {
        int toIndex = random.nextInt(terminals.size() - 1);
        if (toIndex >= fromIndex) {
            toIndex++;
        }
        Terminal from = terminals.get(fromIndex);
        Terminal to = terminals.get(toIndex);
        int fleet = random.nextInt(from.convoys().size());
        List<String> haulers = from.haulers().isEmpty() ? List.of() : from.haulers().get(fleet % from.haulers().size());
        long key = (long) fromIndex * terminals.size() + toIndex;
        ConvoyRun run = runs.computeIfAbsent(key, ignored -> new ConvoyRun(ConvoyRoute.acrossTheAtlas(bodies, from.body(), to.body()), 0, 1));
        long seed = 31L * from.body().nodeId().hashCode() + departures;
        trips.add(new Trip(from, to, run, new ConvoyLooks(from.convoys().get(fleet), haulers, seed)));
        departures++;
    }

    void render(TreeViewport viewport, IntToDoubleFunction portAlpha, float alpha) {
        for (Trip trip : trips) {
            float progress = trip.run.length() > 0f ? trip.distance / trip.run.length() : 0f;
            float fromAlpha = (float) portAlpha.applyAsDouble(trip.from.body().index());
            float toAlpha = (float) portAlpha.applyAsDouble(trip.to.body().index());
            trip.looks.render(viewport, trip.run.along(trip.distance), alpha * (fromAlpha + (toAlpha - fromAlpha) * progress));
        }
    }
}
