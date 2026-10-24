package exiledsector.charts.atlas;

import java.util.Map;

public record AtlasRegion(String id, float weight, int minSteps, String requiredMod, String unlock, Map<String, Float> layoutWeights,
                          Map<String, Float> factionWeights, Map<String, Float> markerWeights) {

    public static final String NORMAL = "normal";
    public static final AtlasRegion DEFAULT_NORMAL = new AtlasRegion(NORMAL, 1f, 0, "", "", Map.of(), Map.of(), Map.of());

    public AtlasRegion {
        weight = Math.max(0f, weight);
        minSteps = Math.max(0, minSteps);
        requiredMod = requiredMod == null ? "" : requiredMod;
        unlock = unlock == null ? "" : unlock;
        layoutWeights = Map.copyOf(layoutWeights);
        factionWeights = Map.copyOf(factionWeights);
        markerWeights = Map.copyOf(markerWeights);
    }

    public boolean isNormal() {
        return NORMAL.equals(id);
    }

    public float markerWeight(String markerId) {
        return markerWeights.getOrDefault(markerId, 1f);
    }

    boolean availableWith(AtlasUnlocks unlocks) {
        return unlocks.allows(requiredMod, unlock);
    }
}
