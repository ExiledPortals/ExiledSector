package exiledsector.charts.layout.sketch;

import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RingBandAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.layout.ChartGeometry;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Belt;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Body;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Moon;
import exiledsector.charts.layout.sketch.ChartSystemSketch.PlanetRing;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public final class SectorSystemSketcher {

    private SectorSystemSketcher() {
    }

    public static ChartSystemSketch sketch(StarSystemAPI system) {
        if (system == null) {
            return null;
        }
        SectorEntityToken centre = system.getCenter();
        Vector2f centreLocation = centre == null ? new Vector2f() : centre.getLocation();
        List<Body> stars = new ArrayList<>();
        List<Body> planets = new ArrayList<>();
        List<PlanetAPI> centralPlanets = new ArrayList<>();
        for (PlanetAPI planet : system.getPlanets()) {
            if (planet.isStar()) {
                stars.add(body(planet, centreLocation));
            } else if (isCentral(focusOf(planet), centre, centreLocation)) {
                planets.add(body(planet, centreLocation));
                centralPlanets.add(planet);
            }
        }
        List<Moon> moons = new ArrayList<>();
        for (PlanetAPI planet : system.getPlanets()) {
            SectorEntityToken focus = focusOf(planet);
            int parentIndex = indexOf(centralPlanets, focus);
            if (!planet.isStar() && parentIndex >= 0) {
                moons.add(new Moon(parentIndex, body(planet, focus.getLocation())));
            }
        }
        List<Belt> belts = new ArrayList<>();
        List<PlanetRing> planetRings = new ArrayList<>();
        for (Object entity : system.getEntities(RingBandAPI.class)) {
            RingBandAPI band = (RingBandAPI) entity;
            Belt belt = new Belt(band.getSpriteKey(), band.getMiddleRadius(), band.getBandWidthInEngine(), band.getOrbitDays(), band.getBandIndex());
            int parentIndex = indexOf(centralPlanets, band.getFocus());
            if (parentIndex >= 0) {
                planetRings.add(new PlanetRing(parentIndex, belt));
            } else if (isCentral(band.getFocus(), centre, centreLocation)) {
                belts.add(belt);
            }
        }
        return new ChartSystemSketch(system.getType(), stars, planets, belts, moons, planetRings, null);
    }

    static boolean isCentral(SectorEntityToken focus, SectorEntityToken centre, Vector2f centreLocation) {
        if (focus == null || focus == centre) {
            return true;
        }
        return focus instanceof PlanetAPI star && star.isStar() && distance(centreLocation, star.getLocation()) <= star.getRadius();
    }

    private static int indexOf(List<PlanetAPI> centralPlanets, SectorEntityToken focus) {
        for (int index = 0; index < centralPlanets.size(); index++) {
            if (centralPlanets.get(index) == focus) {
                return index;
            }
        }
        return -1;
    }

    private static SectorEntityToken focusOf(PlanetAPI planet) {
        OrbitAPI orbit = planet.getOrbit();
        return orbit == null ? null : orbit.getFocus();
    }

    private static float distance(Vector2f from, Vector2f to) {
        return (float) Math.hypot(to.x - from.x, to.y - from.y);
    }

    private static Body body(PlanetAPI planet, Vector2f focusLocation) {
        Vector2f location = planet.getLocation();
        float orbitRadius = distance(focusLocation, location);
        float angle = orbitRadius > 0f ? ChartGeometry.degreesFrom(focusLocation, location) : 0f;
        OrbitAPI orbit = planet.getOrbit();
        float orbitDays = orbit == null || orbitRadius <= 0f ? 0f : orbit.getOrbitalPeriod();
        return new Body(planet.getTypeId(), planet.getRadius(), orbitRadius, orbitDays, angle);
    }
}
