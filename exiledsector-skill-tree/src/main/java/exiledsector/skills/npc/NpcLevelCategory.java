package exiledsector.skills.npc;

import exiledsector.ModSettings;

public enum NpcLevelCategory {
    PIRATE("pirate", "exiledSector_npcLevelCapPirate", 15),
    FACTION("faction", "exiledSector_npcLevelCapFaction", 25),
    AI("ai", "exiledSector_npcLevelCapAi", 35);

    private final String csvId;
    private final String capFieldId;
    private final int defaultCap;

    NpcLevelCategory(String csvId, String capFieldId, int defaultCap) {
        this.csvId = csvId;
        this.capFieldId = capFieldId;
        this.defaultCap = defaultCap;
    }

    public String csvId() {
        return csvId;
    }

    public String capFieldId() {
        return capFieldId;
    }

    public int defaultCap() {
        return defaultCap;
    }

    public int settingCap() {
        return Math.max(0, ModSettings.intOr(capFieldId, defaultCap));
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
