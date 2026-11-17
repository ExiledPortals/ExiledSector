package exiledsector.charts.officer.reward;

public enum RewardKind {
    HULL_ITEM("hull_item", false),
    BIOME_HULL_ITEM("biome_hull_item", false),
    CRAFTING_ITEM("crafting_item", true),
    INHIBITOR("inhibitor", true),
    ENGRAM("engram", true),
    FEEDSTOCK("feedstock", true),
    STAR_CHART("star_chart", false),
    UNIQUE("unique", false);

    private final String id;
    private final boolean material;

    RewardKind(String id, boolean material) {
        this.id = id;
        this.material = material;
    }

    public String id() {
        return id;
    }

    public boolean material() {
        return material;
    }

    public static RewardKind byId(String id) {
        for (RewardKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return null;
    }
}
