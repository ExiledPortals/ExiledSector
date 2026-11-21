package exiledsector.charts.phasing;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public record PhasedPlanetTrait(String id, String conditionId, float weight, Map<String, Float> regionMultipliers,
                                Map<String, Float> anomalyMultipliers, Set<String> excludes) {

    public PhasedPlanetTrait {
        regionMultipliers = regionMultipliers == null ? Map.of() : Map.copyOf(regionMultipliers);
        anomalyMultipliers = anomalyMultipliers == null ? Map.of() : Map.copyOf(anomalyMultipliers);
        excludes = excludes == null ? Set.of() : Set.copyOf(excludes);
    }

    public float weightFor(String regionId, Collection<String> anomalyIds) {
        float rolledWeight = weight;
        if (regionId != null) {
            rolledWeight *= regionMultipliers.getOrDefault(regionId, 1f);
        }
        if (anomalyIds != null) {
            for (String anomalyId : anomalyIds) {
                rolledWeight *= anomalyMultipliers.getOrDefault(anomalyId, 1f);
            }
        }
        return Math.max(0f, rolledWeight);
    }

    public boolean conflictsWith(PhasedPlanetTrait other) {
        return excludes.contains(other.id()) || other.excludes().contains(id);
    }
}
