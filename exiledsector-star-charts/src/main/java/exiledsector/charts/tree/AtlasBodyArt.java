package exiledsector.charts.tree;

import java.util.List;

public record AtlasBodyArt(Kind kind, String planetType, List<String> spritePaths, List<String> hullIds, Shape shape, String lightColour) {

    public static final float DEFAULT_SIZE = 20f;
    public static final Shape DEFAULT_SHAPE = new Shape(DEFAULT_SIZE, Facing.SPIN, 1f);
    public static final AtlasBodyArt PLAIN = new AtlasBodyArt(Kind.PLAIN, null, List.of(), List.of(), DEFAULT_SHAPE, null);

    public enum Kind { STAR, PLANET, SPRITE, BINARY, FLEET, DERELICTS, STATION, BREACH, PLAIN }

    public enum Facing { SPIN, TRAVEL }

    public record Shape(float size, Facing facing, float spinRate) {
    }

    public AtlasBodyArt {
        spritePaths = spritePaths == null ? List.of() : List.copyOf(spritePaths);
        hullIds = hullIds == null ? List.of() : List.copyOf(hullIds);
        shape = shape == null ? DEFAULT_SHAPE : shape;
    }

    public float size() {
        return shape.size();
    }

    public Facing facing() {
        return shape.facing();
    }

    public float spinRate() {
        return shape.spinRate();
    }

    public boolean isShipGroup() {
        return kind == Kind.FLEET || kind == Kind.DERELICTS || kind == Kind.STATION;
    }
}
