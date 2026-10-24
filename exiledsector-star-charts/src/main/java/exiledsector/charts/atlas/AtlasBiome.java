package exiledsector.charts.atlas;

import java.util.List;

public record AtlasBiome(long siteKey, float centreX, float centreY, float radius, String regionId, List<AtlasCell> systems) {

    public AtlasBiome {
        systems = List.copyOf(systems);
    }

    public boolean isNormal() {
        return AtlasRegion.NORMAL.equals(regionId);
    }

    public boolean covers(float mapX, float mapY) {
        float dx = mapX - centreX;
        float dy = mapY - centreY;
        return dx * dx + dy * dy < radius * radius;
    }
}
