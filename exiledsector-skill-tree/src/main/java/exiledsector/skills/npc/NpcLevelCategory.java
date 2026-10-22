package exiledsector.skills.npc;

public enum NpcLevelCategory {
    PIRATE("pirate", 15),
    FACTION("faction", 25),
    AI("ai", 35);

    private final String csvId;
    private final int defaultCap;

    NpcLevelCategory(String csvId, int defaultCap) {
        this.csvId = csvId;
        this.defaultCap = defaultCap;
    }

    public String csvId() {
        return csvId;
    }

    public int defaultCap() {
        return defaultCap;
    }

    public static NpcLevelCategory fromCsvId(String csvId) {
        for (NpcLevelCategory category : values()) {
            if (category.csvId.equals(csvId)) {
                return category;
            }
        }
        return null;
    }
}
