package exiledsector.charts.atlas;

@FunctionalInterface
public interface AtlasMemory {

    AtlasMemory NONE = siteKey -> null;

    String recordedRegion(long siteKey);

    default Boolean unlockedAtReveal(long chunkKey, String unlock) {
        return null;
    }

    default String recordedFaction(String territoryKey) {
        return null;
    }
}
