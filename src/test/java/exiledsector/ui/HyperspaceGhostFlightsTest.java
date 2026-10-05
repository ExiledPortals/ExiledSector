package exiledsector.ui;

import exiledsector.ui.hyperspace.HyperspaceAnchor;
import exiledsector.ui.hyperspace.HyperspaceRoute;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HyperspaceGhostFlightsTest {

    private static final HyperspaceRoute ROUTE = new HyperspaceRoute(
            new HyperspaceAnchor("sun", "core", 0f, 0f, 150f, true),
            new HyperspaceAnchor("pirate", "pirate", 8000f, 0f, 400f, true));

    @Test
    void eachRouteLaunchesAFlightWithinItsLongestWait() {
        HyperspaceGhostFlights flights = new HyperspaceGhostFlights(new Random(1));
        flights.setRoutes(List.of(ROUTE));

        flights.advance(HyperspaceGhostFlights.MAX_SECONDS_BETWEEN_FLIGHTS);

        assertEquals(1, flights.activeFlightCount());
    }

    @Test
    void aRouteWaitsAtLeastTheShortestGapBeforeItsNextFlight() {
        HyperspaceGhostFlights flights = new HyperspaceGhostFlights(new Random(1));
        flights.setRoutes(List.of(ROUTE));
        flights.advance(HyperspaceGhostFlights.MAX_SECONDS_BETWEEN_FLIGHTS);

        flights.advance(1f);

        assertEquals(1, flights.activeFlightCount());
    }

    @Test
    void flightsExpireOnceTheyReachTheOtherStar() {
        HyperspaceGhostFlights flights = new HyperspaceGhostFlights(new Random(1));
        flights.setRoutes(List.of(ROUTE));
        flights.advance(HyperspaceGhostFlights.MAX_SECONDS_BETWEEN_FLIGHTS);

        flights.advance(HyperspaceGhostFlights.MIN_SECONDS_BETWEEN_FLIGHTS / 2f);

        assertEquals(0, flights.activeFlightCount());
    }

    @Test
    void nothingFliesWithoutRoutes() {
        HyperspaceGhostFlights flights = new HyperspaceGhostFlights(new Random(1));

        flights.advance(HyperspaceGhostFlights.MAX_SECONDS_BETWEEN_FLIGHTS * 3f);

        assertEquals(0, flights.activeFlightCount());
    }
}
