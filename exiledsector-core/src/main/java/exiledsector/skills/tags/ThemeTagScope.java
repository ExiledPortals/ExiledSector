package exiledsector.skills.tags;

public enum ThemeTagScope {
    SHIP("ship"),
    STAR_CHART("star_chart");

    private final String id;

    ThemeTagScope(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ThemeTagScope byId(String id) {
        for (ThemeTagScope scope : values()) {
            if (scope.id.equals(id)) {
                return scope;
            }
        }
        return null;
    }
}
