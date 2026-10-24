package exiledsector.charts.atlas;

@FunctionalInterface
public interface AtlasRegionMemory {

    AtlasRegionMemory NONE = siteKey -> null;

    String recordedRegion(long siteKey);
}
