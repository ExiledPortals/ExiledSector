package exiledsector.charts.atlas;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.Map;

public record AtlasLayout(String id, float weight, Map<StarSystemType, Float> systemTypeWeights, int minOrbits, int maxOrbits,
                          float maxOrbitRadius, Map<String, Float> terrainWeights, String featureId) {

    public AtlasLayout {
        weight = Math.max(0f, weight);
        systemTypeWeights = Map.copyOf(systemTypeWeights);
        terrainWeights = Map.copyOf(terrainWeights);
        featureId = featureId == null ? "" : featureId;
    }
}
