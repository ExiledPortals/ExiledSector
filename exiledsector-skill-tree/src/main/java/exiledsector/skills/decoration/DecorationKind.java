package exiledsector.skills.decoration;

public enum DecorationKind {
    STAR("star"),
    IMAGE("image"),
    RING_BELT("ringBelt"),
    THREAT_UNIT("threatUnit"),
    SERPENT("serpent");

    private final String dataKey;

    DecorationKind(String dataKey) {
        this.dataKey = dataKey;
    }

    public String dataKey() {
        return dataKey;
    }

    public static DecorationKind byDataKey(String dataKey) {
        for (DecorationKind kind : values()) {
            if (kind.dataKey.equals(dataKey)) {
                return kind;
            }
        }
        return null;
    }
}
