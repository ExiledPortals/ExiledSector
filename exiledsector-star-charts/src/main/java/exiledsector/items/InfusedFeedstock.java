package exiledsector.items;

import java.awt.Color;

public enum InfusedFeedstock {

    THREAT("exiledSector_threat_feedstock", new Color(130, 155, 145)),
    SHROUD("exiledSector_shroud_feedstock", new Color(175, 0, 28)),
    PHASE("exiledSector_phase_feedstock", new Color(200, 120, 255));

    private final String itemId;
    private final Color color;

    InfusedFeedstock(String itemId, Color color) {
        this.itemId = itemId;
        this.color = color;
    }

    public String itemId() {
        return itemId;
    }

    public Color color() {
        return color;
    }

    public static InfusedFeedstock byItemId(String itemId) {
        for (InfusedFeedstock feedstock : values()) {
            if (feedstock.itemId.equals(itemId)) {
                return feedstock;
            }
        }
        return null;
    }
}
