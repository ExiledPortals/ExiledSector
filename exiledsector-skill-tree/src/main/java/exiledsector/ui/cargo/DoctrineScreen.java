package exiledsector.ui.cargo;

public enum DoctrineScreen {
    READOUT(null),
    SEAL("hegemony"),
    DECRYPT("tritachyon"),
    MANUSCRIPT("luddic_church"),
    DRILL("lions_guard"),
    GLITCH("pirates"),
    COMMITTEE("persean"),
    REDACTED("remnant");

    private final String factionId;

    DoctrineScreen(String factionId) {
        this.factionId = factionId;
    }

    public static DoctrineScreen forFaction(String factionId) {
        for (DoctrineScreen screen : values()) {
            if (screen.factionId != null && screen.factionId.equals(factionId)) {
                return screen;
            }
        }
        return READOUT;
    }
}
