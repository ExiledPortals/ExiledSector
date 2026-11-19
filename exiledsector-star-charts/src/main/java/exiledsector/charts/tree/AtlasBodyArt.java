package exiledsector.charts.tree;

import java.util.List;

public record AtlasBodyArt(Kind kind, String planetType, List<String> spritePaths, List<String> hullIds, Shape shape, Colours colours,
                           Weather weather) {

    public static final float DEFAULT_SIZE = 20f;
    public static final float DEFAULT_HEADING = 90f;
    public static final Shape DEFAULT_SHAPE = new Shape(DEFAULT_SIZE, Facing.SPIN, 1f, DEFAULT_HEADING);
    public static final Colours NO_COLOURS = new Colours(null, null);
    public static final Weather NO_WEATHER = new Weather(Storm.NONE, Flare.NONE);
    public static final AtlasBodyArt PLAIN = new AtlasBodyArt(Kind.PLAIN, null, List.of(), List.of(), DEFAULT_SHAPE, NO_COLOURS, NO_WEATHER);

    public enum Kind {
        STAR, PLANET, SWARMED_PLANET, CROSSED_PLANET, SPRITE, BINARY, FLEET, DERELICTS, SHROUDED, STATION, BREACH, STORM_CELL, PLAIN
    }

    public enum Facing { SPIN, TRAVEL }

    public enum Storm { NONE, GENERIC, LIGHTNING_ROD, SUPERCELL, EYE, CAPTURED, SOLAR, DUEL }

    public enum Flare { NONE, PLAIN, OFTEN, WARM, GENTLE, PERFECT, LICKING }

    public record Shape(float size, Facing facing, float spinRate, float heading) {
    }

    public record Colours(String light, String tint) {
    }

    public record Weather(Storm storm, Flare flare) {

        public Weather {
            storm = storm == null ? Storm.NONE : storm;
            flare = flare == null ? Flare.NONE : flare;
        }
    }

    public AtlasBodyArt {
        spritePaths = spritePaths == null ? List.of() : List.copyOf(spritePaths);
        hullIds = hullIds == null ? List.of() : List.copyOf(hullIds);
        shape = shape == null ? DEFAULT_SHAPE : shape;
        colours = colours == null ? NO_COLOURS : colours;
        weather = weather == null ? NO_WEATHER : weather;
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

    public float heading() {
        return shape.heading();
    }

    public String lightColour() {
        return colours.light();
    }

    public String tintColour() {
        return colours.tint();
    }

    public Storm storm() {
        return weather.storm();
    }

    public Flare flare() {
        return weather.flare();
    }

    public boolean isPlanet() {
        return kind == Kind.PLANET || kind == Kind.SWARMED_PLANET || kind == Kind.CROSSED_PLANET;
    }

    public boolean isShipGroup() {
        return kind == Kind.FLEET || kind == Kind.DERELICTS || kind == Kind.SHROUDED || kind == Kind.STATION;
    }
}
