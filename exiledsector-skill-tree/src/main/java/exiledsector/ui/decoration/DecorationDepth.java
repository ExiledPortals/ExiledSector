package exiledsector.ui.decoration;

public enum DecorationDepth {
    BEHIND_STARS(false),
    STAR_DISC(false),
    STAR_ATMOSPHERE(false),
    STAR_AURORA(false),
    BELTS(false),
    CREATURES(false),
    FOREGROUND_IMAGES(false),
    UNITS(false),
    STAR_GLOW(true);

    private final boolean overTree;

    DecorationDepth(boolean overTree) {
        this.overTree = overTree;
    }

    public boolean isOverTree() {
        return overTree;
    }
}
