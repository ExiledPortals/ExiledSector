package exiledsector.charts.layout;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.Map;

public record ChartShape(Map<StarSystemType, Float> systemTypeWeights, int minOrbits, int maxOrbits, float maxOrbitRadius,
                         Map<String, Float> terrainWeights, String featureId) {

    public static final float VANILLA_ORBIT_RADIUS = 20000f;
    public static final ChartShape ANY = new ChartShape(Map.of(), -1, -1, Float.NaN, Map.of(), "");

    public ChartShape {
        systemTypeWeights = Map.copyOf(systemTypeWeights);
        if (minOrbits >= 0 || maxOrbits >= 0) {
            minOrbits = Math.max(0, minOrbits);
            maxOrbits = Math.max(minOrbits, maxOrbits);
        }
        terrainWeights = Map.copyOf(terrainWeights);
        featureId = featureId == null ? "" : featureId;
    }

    public boolean setsOrbits() {
        return minOrbits >= 0;
    }

    public float orbitRadiusLimit() {
        return Float.isNaN(maxOrbitRadius) || maxOrbitRadius <= 0f ? VANILLA_ORBIT_RADIUS : maxOrbitRadius;
    }
}
