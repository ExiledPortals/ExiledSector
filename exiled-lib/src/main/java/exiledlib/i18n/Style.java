package exiledlib.i18n;

public enum Style {
    GOOD("good"),
    BAD("bad"),
    HIGHLIGHT("hl"),
    HULLMOD("hullmod"),
    NODE("node"),
    IMMUTABLE("immutable"),
    MUTED("muted");

    private final String tag;

    Style(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }

    public Style inverted() {
        return switch (this) {
            case GOOD -> BAD;
            case BAD -> GOOD;
            default -> this;
        };
    }

    public static Style byTag(String tag) {
        for (Style style : values()) {
            if (style.tag.equals(tag)) {
                return style;
            }
        }
        return null;
    }
}
