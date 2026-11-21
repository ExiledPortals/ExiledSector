package exiledsector.charts.layout.sketch;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.awt.Color;
import java.util.List;

public record ChartSystemSketch(StarSystemType systemType, List<Body> stars, List<Body> planets, List<Belt> belts, List<Moon> moons,
                                List<PlanetRing> planetRings, Color photosphere) {

    public record Body(String typeId, float radius, float orbitRadius, float orbitDays, float startAngle) {
    }

    public record Belt(String typeId, float orbitRadius, float width, float orbitDays, int variant) {
    }

    public record Moon(int planet, Body body) {
    }

    public record PlanetRing(int planet, Belt belt) {
    }

    public ChartSystemSketch {
        stars = List.copyOf(stars);
        planets = List.copyOf(planets);
        belts = List.copyOf(belts);
        moons = List.copyOf(moons);
        planetRings = List.copyOf(planetRings);
    }

    public ChartSystemSketch(StarSystemType systemType, List<Body> stars, List<Body> planets, List<Belt> belts, Color photosphere) {
        this(systemType, stars, planets, belts, List.of(), List.of(), photosphere);
    }

    public ChartSystemSketch(StarSystemType systemType, List<Body> stars, List<Body> planets, List<Belt> belts) {
        this(systemType, stars, planets, belts, List.of(), List.of(), null);
    }

    public float outerRadius() {
        float outer = 0f;
        for (Body star : stars) {
            outer = Math.max(outer, star.orbitRadius() + star.radius());
        }
        for (int index = 0; index < planets.size(); index++) {
            outer = Math.max(outer, planets.get(index).orbitRadius() + reachAround(index));
        }
        for (Belt belt : belts) {
            outer = Math.max(outer, belt.orbitRadius() + belt.width() / 2f);
        }
        return outer;
    }

    public float reachAround(int planetIndex) {
        float reach = planets.get(planetIndex).radius();
        for (Moon moon : moons) {
            if (moon.planet() == planetIndex) {
                reach = Math.max(reach, moon.body().orbitRadius() + moon.body().radius());
            }
        }
        for (PlanetRing ring : planetRings) {
            if (ring.planet() == planetIndex) {
                reach = Math.max(reach, ring.belt().orbitRadius() + ring.belt().width() / 2f);
            }
        }
        return reach;
    }
}
