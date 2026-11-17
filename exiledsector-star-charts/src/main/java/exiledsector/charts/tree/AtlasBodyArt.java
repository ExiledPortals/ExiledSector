package exiledsector.charts.tree;

import java.util.List;

public record AtlasBodyArt(Kind kind, String planetType, List<String> spritePaths, float size, Facing facing) {

    public static final float DEFAULT_SIZE = 20f;
    public static final AtlasBodyArt PLAIN = new AtlasBodyArt(Kind.PLAIN, null, List.of(), DEFAULT_SIZE, Facing.SPIN);

    public enum Kind { STAR, PLANET, SPRITE, FLEET, BREACH, PLAIN }

    public enum Facing { SPIN, TRAVEL }

    public AtlasBodyArt {
        spritePaths = spritePaths == null ? List.of() : List.copyOf(spritePaths);
    }
}
