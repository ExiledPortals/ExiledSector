package exiledsector.items;

public enum InfusedFeedstock {

    THREAT("exiledSector_threat_feedstock"),
    SHROUD("exiledSector_shroud_feedstock"),
    PHASE("exiledSector_phase_feedstock");

    private final String itemId;

    InfusedFeedstock(String itemId) {
        this.itemId = itemId;
    }

    public String itemId() {
        return itemId;
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
