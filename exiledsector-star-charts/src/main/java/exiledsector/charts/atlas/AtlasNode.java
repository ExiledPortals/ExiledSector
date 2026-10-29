package exiledsector.charts.atlas;

import java.util.List;

public record AtlasNode(AtlasCell cell, float mapX, float mapY, boolean isVoid, String regionId, String factionId, String layoutId,
                        List<String> anomalyIds, String specialId, String infestationId, List<AtlasCell> lanes) {

    public AtlasNode {
        anomalyIds = anomalyIds == null ? List.of() : List.copyOf(anomalyIds);
        lanes = List.copyOf(lanes);
    }

    public float distance() {
        return (float) Math.hypot(mapX, mapY);
    }

    public boolean isOrigin() {
        return cell.equals(AtlasCell.ORIGIN);
    }
}
