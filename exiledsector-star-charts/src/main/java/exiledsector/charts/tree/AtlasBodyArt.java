package exiledsector.charts.tree;

import java.util.List;

public record AtlasBodyArt(Kind kind, String planetType, List<String> spritePaths, List<String> hullIds, Shape shape, Colours colours, Storm storm,
                           Population population) {

    public static final float DEFAULT_SIZE = 20f;
    public static final float DEFAULT_HEADING = 90f;
    public static final Shape DEFAULT_SHAPE = new Shape(DEFAULT_SIZE, Facing.SPIN, 1f, DEFAULT_HEADING);
    public static final Colours NO_COLOURS = new Colours(null, null);
    public static final Population UNPOPULATED = new Population(List.of(), List.of());
    public static final AtlasBodyArt PLAIN = new AtlasBodyArt(Kind.PLAIN, null, List.of(), List.of(), DEFAULT_SHAPE, NO_COLOURS, Storm.NONE,
            UNPOPULATED);

    public enum Kind { STAR, PLANET, SWARMED_PLANET, SPRITE, BINARY, FLEET, DERELICTS, SHROUDED, STATION, BREACH, STORM_CELL, PLAIN }

    public enum Facing { SPIN, TRAVEL }

    public enum Storm { NONE, GENERIC, LIGHTNING_ROD, SUPERCELL, EYE, CAPTURED, SOLAR, DUEL }

    public record Shape(float size, Facing facing, float spinRate, float heading) {
    }

    public record Colours(String light, String tint) {
    }

    public record Population(List<String> cityLights, List<List<String>> convoys) {

        public Population {
            cityLights = cityLights == null ? List.of() : List.copyOf(cityLights);
            convoys = convoys == null ? List.of() : convoys.stream().map(List::copyOf).toList();
        }
    }

    public AtlasBodyArt {
        spritePaths = spritePaths == null ? List.of() : List.copyOf(spritePaths);
        hullIds = hullIds == null ? List.of() : List.copyOf(hullIds);
        shape = shape == null ? DEFAULT_SHAPE : shape;
        colours = colours == null ? NO_COLOURS : colours;
        storm = storm == null ? Storm.NONE : storm;
        population = population == null ? UNPOPULATED : population;
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

    public List<String> cityLights() {
        return population.cityLights();
    }

    public List<List<String>> convoys() {
        return population.convoys();
    }

    public boolean isPlanet() {
        return kind == Kind.PLANET || kind == Kind.SWARMED_PLANET;
    }

    public boolean isShipGroup() {
        return kind == Kind.FLEET || kind == Kind.DERELICTS || kind == Kind.SHROUDED || kind == Kind.STATION;
    }
}
