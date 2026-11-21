package exiledsector.charts.layout.feature;

import java.util.List;

public record FeaturePlan(List<World> worlds, List<RingBand> ringBands, List<AsteroidBelt> asteroidBelts) {

    public static final int CENTRE = -1;

    public record World(String part, int focus, String typeId, float radius, float orbitRadius, float orbitDays, float startAngle) {
    }

    public record RingBand(int focus, String texture, int variant, float width, float middleRadius, float orbitDays, boolean terrain) {

        public float outerEdge() {
            return middleRadius + width / 2f;
        }
    }

    public record AsteroidBelt(int asteroids, float orbitRadius, float width, float orbitDays) {
    }

    public FeaturePlan {
        worlds = List.copyOf(worlds);
        ringBands = List.copyOf(ringBands);
        asteroidBelts = List.copyOf(asteroidBelts);
    }

    public float reach() {
        float reach = 0f;
        for (int index = 0; index < worlds.size(); index++) {
            if (worlds.get(index).focus() == CENTRE) {
                reach = Math.max(reach, worlds.get(index).orbitRadius() + extent(index));
            }
        }
        for (RingBand band : ringBands) {
            if (band.focus() == CENTRE) {
                reach = Math.max(reach, band.outerEdge());
            }
        }
        for (AsteroidBelt belt : asteroidBelts) {
            reach = Math.max(reach, belt.orbitRadius() + belt.width() / 2f);
        }
        return reach;
    }

    public float extent(int worldIndex) {
        float extent = worlds.get(worldIndex).radius();
        for (int index = 0; index < worlds.size(); index++) {
            if (worlds.get(index).focus() == worldIndex) {
                extent = Math.max(extent, worlds.get(index).orbitRadius() + extent(index));
            }
        }
        for (RingBand band : ringBands) {
            if (band.focus() == worldIndex) {
                extent = Math.max(extent, band.outerEdge());
            }
        }
        return extent;
    }
}
