package exiledsector.charts.atlas;

import java.util.List;

public record AtlasNode(AtlasCell cell, float mapX, float mapY, boolean isVoid, String regionId, String factionId, String layoutId,
                        boolean powerfulBoss, String phenomenonId, String expansionId, String specialId, String infestationId,
                        List<AtlasCell> lanes) {

    public AtlasNode {
        lanes = List.copyOf(lanes);
    }

    public float distance() {
        return (float) Math.hypot(mapX, mapY);
    }

    public boolean isOrigin() {
        return cell.equals(AtlasCell.ORIGIN);
    }
}
