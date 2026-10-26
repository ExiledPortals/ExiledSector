package exiledsector.charts.layout.sketch;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.List;

public record ChartSystemSketch(StarSystemType systemType, List<Body> stars, List<Body> planets, List<Belt> belts) {

    public record Body(String typeId, float radius, float orbitRadius, float orbitDays, float startAngle) {
    }

    public record Belt(String typeId, float orbitRadius, float width, float orbitDays, int variant) {
    }

    public ChartSystemSketch {
        stars = List.copyOf(stars);
        planets = List.copyOf(planets);
        belts = List.copyOf(belts);
    }

    public float outerRadius() {
        float outer = 0f;
        for (Body star : stars) {
            outer = Math.max(outer, star.orbitRadius() + star.radius());
        }
        for (Body planet : planets) {
            outer = Math.max(outer, planet.orbitRadius() + planet.radius());
        }
        for (Belt belt : belts) {
            outer = Math.max(outer, belt.orbitRadius() + belt.width() / 2f);
        }
        return outer;
    }
}
