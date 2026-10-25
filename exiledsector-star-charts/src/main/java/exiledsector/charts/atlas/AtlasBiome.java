package exiledsector.charts.atlas;

public record AtlasBiome(long siteKey, float centreX, float centreY, String regionId) {

    public boolean isNormal() {
        return AtlasRegion.NORMAL.equals(regionId);
    }
}
