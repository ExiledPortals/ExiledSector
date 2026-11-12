package exiledsector.charts.atlas;

@FunctionalInterface
public interface AtlasMemory {

    AtlasMemory NONE = siteKey -> null;

    String recordedRegion(long siteKey);

    default Boolean unlockedAtReveal(long chunkKey, String unlock) {
        return null;
    }

    default String claimedFaction(AtlasCell cell) {
        return null;
    }

    default String territoryAt(AtlasCell cell) {
        return null;
    }
}
