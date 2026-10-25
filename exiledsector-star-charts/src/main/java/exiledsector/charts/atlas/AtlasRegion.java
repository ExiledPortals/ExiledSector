package exiledsector.charts.atlas;

import java.util.Map;

public record AtlasRegion(String id, float weight, int minSteps, String requiredMod, String unlock, Map<String, Float> layoutWeights,
                          Map<String, Float> factionWeights, Map<String, Float> markerWeights, int maxSteps, float weightPerStep, float size,
                          float density, Map<String, Float> neighbourWeights, float clouds, float walls) {

    public static final String NORMAL = "normal";
    public static final int NO_MAX_STEPS = -1;
    public static final float MIN_SIZE = 0.5f;
    public static final float MAX_SIZE = 2f;
    public static final AtlasRegion DEFAULT_NORMAL = new AtlasRegion(NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of());

    public AtlasRegion {
        weight = Math.max(0f, weight);
        minSteps = Math.max(0, minSteps);
        maxSteps = maxSteps < 0 ? NO_MAX_STEPS : Math.max(minSteps, maxSteps);
        requiredMod = requiredMod == null ? "" : requiredMod;
        unlock = unlock == null ? "" : unlock;
        size = Float.isNaN(size) ? 1f : Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
        density = Float.isNaN(density) ? 1f : Math.max(0f, Math.min(1f, density));
        clouds = Float.isNaN(clouds) ? 0f : Math.max(0f, Math.min(1f, clouds));
        walls = Float.isNaN(walls) ? 0f : Math.max(0f, Math.min(1f, walls));
        layoutWeights = Map.copyOf(layoutWeights);
        factionWeights = Map.copyOf(factionWeights);
        markerWeights = Map.copyOf(markerWeights);
        neighbourWeights = Map.copyOf(neighbourWeights);
    }

    public AtlasRegion(String id, float weight, int minSteps, String requiredMod, String unlock, Map<String, Float> layoutWeights,
                       Map<String, Float> factionWeights, Map<String, Float> markerWeights) {
        this(id, weight, minSteps, requiredMod, unlock, layoutWeights, factionWeights, markerWeights, NO_MAX_STEPS, 0f, 1f, 1f, Map.of());
    }

    public AtlasRegion(String id, float weight, int minSteps, String requiredMod, String unlock, Map<String, Float> layoutWeights,
                       Map<String, Float> factionWeights, Map<String, Float> markerWeights, int maxSteps, float weightPerStep, float size,
                       float density, Map<String, Float> neighbourWeights) {
        this(id, weight, minSteps, requiredMod, unlock, layoutWeights, factionWeights, markerWeights, maxSteps, weightPerStep, size, density,
                neighbourWeights, 0f, 0f);
    }

    public boolean isNormal() {
        return NORMAL.equals(id);
    }

    public float markerWeight(String markerId) {
        return markerWeights.getOrDefault(markerId, 1f);
    }

    public float weightAt(float distance) {
        if (distance < minSteps || maxSteps != NO_MAX_STEPS && distance > maxSteps) {
            return 0f;
        }
        return Math.max(0f, weight + weightPerStep * (distance - minSteps));
    }

    public float neighbourWeight(String regionId) {
        return neighbourWeights.getOrDefault(regionId, 1f);
    }

    boolean availableWith(AtlasUnlocks unlocks) {
        return unlocks.allows(requiredMod, unlock);
    }
}
