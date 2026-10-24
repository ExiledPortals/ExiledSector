package exiledsector.ui;

import exiledsector.ui.hyperspace.HyperspaceRoute;
import exiledsector.ui.node.GhostFlight;
import exiledsector.ui.node.GhostFlightPool;

import java.util.List;
import java.util.Random;

final class HyperspaceGhostFlights {

    static final float MIN_SECONDS_BETWEEN_FLIGHTS = 20f;
    static final float MAX_SECONDS_BETWEEN_FLIGHTS = 60f;
    private static final float FLIGHT_SPEED = 2500f;
    private static final float GHOST_SIZE = 14f;

    private final Random random;
    private final GhostFlightPool flights = new GhostFlightPool();
    private List<HyperspaceRoute> routes = List.of();
    private float[] secondsUntilNextFlight = new float[0];

    HyperspaceGhostFlights(Random random) {
        this.random = random;
    }

    void setRoutes(List<HyperspaceRoute> newRoutes) {
        if (newRoutes.equals(routes)) {
            return;
        }
        routes = newRoutes;
        flights.clear();
        secondsUntilNextFlight = new float[routes.size()];
        for (int i = 0; i < secondsUntilNextFlight.length; i++) {
            secondsUntilNextFlight[i] = random.nextFloat() * MAX_SECONDS_BETWEEN_FLIGHTS;
        }
    }

    void advance(float amount) {
        flights.advance(amount);
        for (int i = 0; i < secondsUntilNextFlight.length; i++) {
            secondsUntilNextFlight[i] -= amount;
            if (secondsUntilNextFlight[i] <= 0f) {
                flights.add(launch(routes.get(i)));
                secondsUntilNextFlight[i] = MIN_SECONDS_BETWEEN_FLIGHTS
                        + random.nextFloat() * (MAX_SECONDS_BETWEEN_FLIGHTS - MIN_SECONDS_BETWEEN_FLIGHTS);
            }
        }
    }

    int activeFlightCount() {
        return flights.size();
    }

    private GhostFlight launch(HyperspaceRoute route) {
        boolean forwards = random.nextBoolean();
        return forwards
                ? GhostFlight.between(route.from().x(), route.from().y(), route.to().x(), route.to().y(), FLIGHT_SPEED, random)
                : GhostFlight.between(route.to().x(), route.to().y(), route.from().x(), route.from().y(), FLIGHT_SPEED, random);
    }

    void draw(TreeViewport viewport, float alphaMult) {
        flights.draw(GHOST_SIZE, viewport, alphaMult);
    }
}
