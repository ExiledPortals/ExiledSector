package exiledsector.charts.tree;

import java.util.List;

public record AtlasBodyArt(Kind kind, String planetType, List<String> spritePaths, List<String> hullIds, Shape shape, Colours colours,
                           Weather weather, Population population, Flagship flagship) {

    public static final float DEFAULT_SIZE = 20f;
    public static final float DEFAULT_HEADING = 90f;
    public static final Shape DEFAULT_SHAPE = new Shape(DEFAULT_SIZE, Facing.SPIN, 1f, DEFAULT_HEADING);
    public static final Colours NO_COLOURS = new Colours(null, null);
    public static final Weather NO_WEATHER = new Weather(Storm.NONE, Flare.NONE, Field.NONE, Nebula.NONE);
    public static final Population UNPOPULATED = new Population(List.of(), List.of(), List.of());
    public static final Flagship NO_FLAGSHIP = new Flagship(Scene.NONE, List.of(), List.of(), null);
    public static final AtlasBodyArt PLAIN = new AtlasBodyArt(Kind.PLAIN, null, List.of(), List.of(), DEFAULT_SHAPE, NO_COLOURS, NO_WEATHER,
            UNPOPULATED, NO_FLAGSHIP);

    public enum Kind {
        STAR, PLANET, SWARMED_PLANET, ORBITED_PLANET, SPRITE, BINARY, FLEET, DERELICTS, SHROUDED, STATION, OUTPOST, PATROL, BREACH, STORM_CELL, NEBULA,
        FLAGSHIP, PLAIN
    }

    public enum Scene { NONE, COMMAND, ESCORT, GUARD_RING, STANDOFF, TROPHY, SCRAPPERS, SPOILS, TOW, SALVAGE }

    public enum Facing { SPIN, TRAVEL }

    public enum Storm { NONE, GENERIC, LIGHTNING_ROD, SUPERCELL, EYE, CAPTURED, SOLAR, DUEL }

    public enum Flare { NONE, PLAIN, OFTEN, WARM, GENTLE, PERFECT, LICKING, SURGE }

    public enum Field { NONE, PLAIN, CHARGED, HARVEST, ION, SURGE }

    public enum Nebula { NONE, PUFF, TRAILS, SETTLING, IONIZED }

    public record Shape(float size, Facing facing, float spinRate, float heading) {
    }

    public record Colours(String light, String tint) {
    }

    public record Weather(Storm storm, Flare flare, Field field, Nebula nebula) {

        public Weather {
            storm = storm == null ? Storm.NONE : storm;
            flare = flare == null ? Flare.NONE : flare;
            field = field == null ? Field.NONE : field;
            nebula = nebula == null ? Nebula.NONE : nebula;
        }
    }

    public record Population(List<String> cityLights, List<List<String>> convoys, List<List<String>> haulers) {

        public Population {
            cityLights = cityLights == null ? List.of() : List.copyOf(cityLights);
            convoys = convoys == null ? List.of() : convoys.stream().map(List::copyOf).toList();
            haulers = haulers == null ? List.of() : haulers.stream().map(List::copyOf).toList();
        }
    }

    public record Flagship(Scene scene, List<String> wreckHulls, List<String> rivalHulls, String rivalColour) {

        public Flagship {
            scene = scene == null ? Scene.NONE : scene;
            wreckHulls = wreckHulls == null ? List.of() : List.copyOf(wreckHulls);
            rivalHulls = rivalHulls == null ? List.of() : List.copyOf(rivalHulls);
        }
    }

    public AtlasBodyArt {
        spritePaths = spritePaths == null ? List.of() : List.copyOf(spritePaths);
        hullIds = hullIds == null ? List.of() : List.copyOf(hullIds);
        shape = shape == null ? DEFAULT_SHAPE : shape;
        colours = colours == null ? NO_COLOURS : colours;
        weather = weather == null ? NO_WEATHER : weather;
        population = population == null ? UNPOPULATED : population;
        flagship = flagship == null ? NO_FLAGSHIP : flagship;
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

    public Field field() {
        return weather.field();
    }

    public Nebula nebula() {
        return weather.nebula();
    }

    public List<String> cityLights() {
        return population.cityLights();
    }

    public List<List<String>> convoys() {
        return population.convoys();
    }

    public List<List<String>> haulers() {
        return population.haulers();
    }

    public Scene scene() {
        return flagship.scene();
    }

    public List<String> wreckHulls() {
        return flagship.wreckHulls();
    }

    public List<String> rivalHulls() {
        return flagship.rivalHulls();
    }

    public String rivalColour() {
        return flagship.rivalColour();
    }

    public boolean isPlanet() {
        return kind == Kind.PLANET || kind == Kind.SWARMED_PLANET || kind == Kind.ORBITED_PLANET;
    }

    public boolean isShipGroup() {
        return kind == Kind.FLEET || kind == Kind.DERELICTS || kind == Kind.SHROUDED || kind == Kind.STATION || kind == Kind.OUTPOST
                || kind == Kind.PATROL || kind == Kind.FLAGSHIP;
    }
}
