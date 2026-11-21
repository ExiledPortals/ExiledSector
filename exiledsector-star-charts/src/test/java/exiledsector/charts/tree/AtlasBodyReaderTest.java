package exiledsector.charts.tree;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasBodyReaderTest {

    private static final String BODIES = """
            { "bodies": [
                { "id": "cell", "colour": "#ff28d2", "art": { "kind": "storm_cell", "size": 40 } },
                { "id": "rod", "art": { "kind": "planet", "planet": "gas_giant", "tint": "#ffc4ee", "storm": "lightning_rod", "size": 150 } },
                { "id": "eye", "art": { "kind": "planet", "planet": "ice_giant", "storm": "eye" } },
                { "id": "solar", "art": { "kind": "planet", "planet": "star_yellow", "storm": "solar", "size": 110 } },
                { "id": "swarm", "art": { "kind": "swarmed_planet", "planet": "gas_giant", "hulls": ["glimmer", "lumen"], "size": 150 } },
                { "id": "odd", "art": { "kind": "planet", "planet": "gas_giant", "storm": "hurricane" } },
                { "id": "site", "art": { "kind": "planet", "planet": "barren", "light": "#ffbe5a" } },
                { "id": "world", "art": { "kind": "planet", "planet": "jungle", "cityLights": ["volturn", "sindria"], "size": 150 } },
                { "id": "port", "wheel": "world", "art": { "kind": "sprite", "path": "graphics/stations/station_lowtech1.png", "size": 30,
                    "convoys": [["buffalo", "wolf"], [], ["tarsus", "hermes", "lasher"]], "haulers": [["colossus", "eagle"], []] } },
                { "id": "maximum", "art": { "kind": "planet", "planet": "star_blue_giant", "flare": "often", "size": 110 } },
                { "id": "sunstruck", "art": { "kind": "orbited_planet", "planet": "star_yellow", "flare": "licking",
                    "hulls": ["colossus", "buffalo"], "size": 90, "facing": "travel", "heading": -135 } },
                { "id": "flaring_storm", "art": { "kind": "planet", "planet": "star_white", "storm": "solar", "flare": "perfect" } },
                { "id": "odd_flare", "art": { "kind": "planet", "planet": "star_yellow", "flare": "supernova" } },
                { "id": "charged", "art": { "kind": "planet", "planet": "rocky_metallic", "field": "charged", "size": 84 } },
                { "id": "ion", "art": { "kind": "planet", "planet": "water", "field": "ion", "storm": "generic", "size": 90 } },
                { "id": "surge", "art": { "kind": "planet", "planet": "star_red_giant", "field": "surge", "flare": "surge" } },
                { "id": "odd_field", "art": { "kind": "planet", "planet": "barren", "field": "monopole" } },
                { "id": "puff", "colour": "#c8a082", "art": { "kind": "nebula", "nebula": "puff", "size": 42 } },
                { "id": "ionized", "art": { "kind": "nebula", "nebula": "ionized", "size": 116 } },
                { "id": "odd_nebula", "art": { "kind": "nebula", "nebula": "thick_soup" } },
                { "id": "command", "colour": "#ffc850", "art": { "kind": "flagship", "scene": "command",
                    "hulls": ["onslaught", "enforcer", "lasher", "wolf"], "size": 130, "facing": "travel", "heading": 45 } },
                { "id": "trophy", "art": { "kind": "flagship", "scene": "trophy", "hulls": ["paragon", "omen"], "wrecks": ["dominator"] } },
                { "id": "warfare", "art": { "kind": "flagship", "scene": "standoff", "hulls": ["odyssey", "tempest"],
                    "rivals": ["retribution", "lasher"], "rivalColour": "#ff5a50" } },
                { "id": "bare_flagship", "art": { "kind": "flagship", "hulls": ["conquest"] } },
                { "id": "odd_scene", "art": { "kind": "flagship", "scene": "parade", "hulls": ["conquest"] } }
            ] }
            """;

    private static Map<String, AtlasBody> bodies() {
        try {
            return AtlasBodyReader.parse(new JSONObject(BODIES));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Test
    void aStormCellLoadsAsItsOwnKindWithNoPlanetOrStormOfItsOwn() {
        AtlasBodyArt cell = bodies().get("cell").art();

        assertEquals(AtlasBodyArt.Kind.STORM_CELL, cell.kind());
        assertEquals(40f, cell.size());
        assertEquals(AtlasBodyArt.Storm.NONE, cell.storm());
        assertNull(cell.planetType());
        assertFalse(cell.isPlanet());
        assertFalse(cell.isShipGroup());
    }

    @Test
    void aStormyPlanetKeepsItsStormAndTint() {
        Map<String, AtlasBody> bodies = bodies();
        AtlasBodyArt rod = bodies.get("rod").art();

        assertEquals(AtlasBodyArt.Kind.PLANET, rod.kind());
        assertEquals("gas_giant", rod.planetType());
        assertEquals(AtlasBodyArt.Storm.LIGHTNING_ROD, rod.storm());
        assertEquals("#ffc4ee", rod.tintColour());
        assertNull(rod.lightColour());
        assertEquals(AtlasBodyArt.Storm.EYE, bodies.get("eye").art().storm());
        assertNull(bodies.get("eye").art().tintColour());
        assertEquals(AtlasBodyArt.Storm.SOLAR, bodies.get("solar").art().storm());
        assertEquals("star_yellow", bodies.get("solar").art().planetType());
    }

    @Test
    void aSwarmedPlanetIsAPlanetWithItsCirclingHulls() {
        AtlasBodyArt swarm = bodies().get("swarm").art();

        assertEquals(AtlasBodyArt.Kind.SWARMED_PLANET, swarm.kind());
        assertTrue(swarm.isPlanet());
        assertFalse(swarm.isShipGroup());
        assertEquals(List.of("glimmer", "lumen"), swarm.hullIds());
        assertEquals(AtlasBodyArt.Storm.NONE, swarm.storm());
    }

    @Test
    void anUnknownStormFallsBackToNoStormAndAPlanetWithoutOneHasNone() {
        Map<String, AtlasBody> bodies = bodies();

        assertEquals(AtlasBodyArt.Storm.NONE, bodies.get("odd").art().storm());
        assertEquals(AtlasBodyArt.Kind.PLANET, bodies.get("odd").art().kind());
        assertEquals(AtlasBodyArt.Storm.NONE, bodies.get("site").art().storm());
        assertEquals("#ffbe5a", bodies.get("site").art().lightColour());
        assertNull(bodies.get("site").art().tintColour());
    }

    @Test
    void aPopulatedWorldKeepsItsCityLightsAndItsPortKeepsEachNonEmptyConvoy() {
        Map<String, AtlasBody> bodies = bodies();
        AtlasBodyArt world = bodies.get("world").art();
        AtlasBodyArt port = bodies.get("port").art();

        assertEquals(List.of("volturn", "sindria"), world.cityLights());
        assertTrue(world.convoys().isEmpty());
        assertEquals(List.of(List.of("buffalo", "wolf"), List.of("tarsus", "hermes", "lasher")), port.convoys());
        assertEquals(List.of(List.of("colossus", "eagle")), port.haulers());
        assertTrue(world.haulers().isEmpty());
        assertTrue(port.cityLights().isEmpty());
        assertEquals("world", bodies.get("port").wheel());
    }

    @Test
    void aFlaringStarKeepsItsFlareStyleAndAnythingWithoutOneHasNone() {
        Map<String, AtlasBody> bodies = bodies();
        AtlasBodyArt maximum = bodies.get("maximum").art();

        assertEquals(AtlasBodyArt.Kind.PLANET, maximum.kind());
        assertEquals("star_blue_giant", maximum.planetType());
        assertEquals(AtlasBodyArt.Flare.OFTEN, maximum.flare());
        assertEquals(AtlasBodyArt.Storm.NONE, maximum.storm());
        assertEquals(AtlasBodyArt.Flare.NONE, bodies.get("rod").art().flare());
        assertEquals(AtlasBodyArt.Flare.NONE, bodies.get("cell").art().flare());
        assertEquals(AtlasBodyArt.Flare.NONE, bodies.get("swarm").art().flare());
    }

    @Test
    void aStormAndAFlareLoadSideBySideAndAnUnknownFlareFallsBackToNone() {
        Map<String, AtlasBody> bodies = bodies();
        AtlasBodyArt both = bodies.get("flaring_storm").art();

        assertEquals(AtlasBodyArt.Storm.SOLAR, both.storm());
        assertEquals(AtlasBodyArt.Flare.PERFECT, both.flare());
        assertEquals(AtlasBodyArt.Flare.NONE, bodies.get("odd_flare").art().flare());
        assertEquals(AtlasBodyArt.Kind.PLANET, bodies.get("odd_flare").art().kind());
    }

    @Test
    void anOrbitedPlanetIsAPlanetWithItsConvoyHullsAndHeading() {
        AtlasBodyArt sunstruck = bodies().get("sunstruck").art();

        assertEquals(AtlasBodyArt.Kind.ORBITED_PLANET, sunstruck.kind());
        assertTrue(sunstruck.isPlanet());
        assertFalse(sunstruck.isShipGroup());
        assertEquals(List.of("colossus", "buffalo"), sunstruck.hullIds());
        assertEquals(AtlasBodyArt.Flare.LICKING, sunstruck.flare());
        assertEquals(AtlasBodyArt.Facing.TRAVEL, sunstruck.facing());
        assertEquals(-135f, sunstruck.heading());
        assertEquals(90f, sunstruck.size());
    }

    @Test
    void aMagneticPlanetKeepsItsFieldBesideAnyStormOrFlareAndAnythingWithoutOneHasNone() {
        Map<String, AtlasBody> bodies = bodies();
        AtlasBodyArt charged = bodies.get("charged").art();
        AtlasBodyArt ion = bodies.get("ion").art();
        AtlasBodyArt surge = bodies.get("surge").art();

        assertEquals(AtlasBodyArt.Kind.PLANET, charged.kind());
        assertEquals(AtlasBodyArt.Field.CHARGED, charged.field());
        assertEquals(84f, charged.size());
        assertEquals(AtlasBodyArt.Storm.NONE, charged.storm());
        assertEquals(AtlasBodyArt.Field.ION, ion.field());
        assertEquals(AtlasBodyArt.Storm.GENERIC, ion.storm());
        assertEquals(AtlasBodyArt.Field.SURGE, surge.field());
        assertEquals(AtlasBodyArt.Flare.SURGE, surge.flare());
        assertEquals(AtlasBodyArt.Field.NONE, bodies.get("rod").art().field());
        assertEquals(AtlasBodyArt.Field.NONE, bodies.get("maximum").art().field());
        assertEquals(AtlasBodyArt.Nebula.NONE, charged.nebula());
    }

    @Test
    void anUnknownFieldFallsBackToNoFieldOnAPlanetThatStillLoads() {
        AtlasBodyArt odd = bodies().get("odd_field").art();

        assertEquals(AtlasBodyArt.Field.NONE, odd.field());
        assertEquals(AtlasBodyArt.Kind.PLANET, odd.kind());
        assertEquals("barren", odd.planetType());
    }

    @Test
    void aNebulaLoadsAsItsOwnKindWithItsStyleAndNoPlanet() {
        Map<String, AtlasBody> bodies = bodies();
        AtlasBodyArt puff = bodies.get("puff").art();

        assertEquals(AtlasBodyArt.Kind.NEBULA, puff.kind());
        assertEquals(AtlasBodyArt.Nebula.PUFF, puff.nebula());
        assertEquals(42f, puff.size());
        assertEquals("#c8a082", bodies.get("puff").colour());
        assertNull(puff.planetType());
        assertFalse(puff.isPlanet());
        assertFalse(puff.isShipGroup());
        assertEquals(AtlasBodyArt.Field.NONE, puff.field());
        assertEquals(AtlasBodyArt.Nebula.IONIZED, bodies.get("ionized").art().nebula());
        assertEquals(AtlasBodyArt.Nebula.NONE, bodies.get("cell").art().nebula());
    }

    @Test
    void anUnknownNebulaStyleFallsBackToNoneButStaysANebula() {
        AtlasBodyArt odd = bodies().get("odd_nebula").art();

        assertEquals(AtlasBodyArt.Kind.NEBULA, odd.kind());
        assertEquals(AtlasBodyArt.Nebula.NONE, odd.nebula());
    }

    @Test
    void anArtBuiltWithoutWeatherOrPopulationHasNeither() {
        AtlasBodyArt art = new AtlasBodyArt(AtlasBodyArt.Kind.PLANET, "star_yellow", null, null, null, null, null, null, null);
        AtlasBodyArt.Weather halfWeather = new AtlasBodyArt.Weather(null, AtlasBodyArt.Flare.WARM, null, null);

        assertEquals(AtlasBodyArt.NO_WEATHER, art.weather());
        assertEquals(AtlasBodyArt.UNPOPULATED, art.population());
        assertEquals(AtlasBodyArt.NO_FLAGSHIP, art.flagship());
        assertEquals(AtlasBodyArt.Storm.NONE, halfWeather.storm());
        assertEquals(AtlasBodyArt.Flare.WARM, halfWeather.flare());
        assertEquals(AtlasBodyArt.Field.NONE, halfWeather.field());
        assertEquals(AtlasBodyArt.Nebula.NONE, halfWeather.nebula());
    }

    @Test
    void thePlainArtHasNoWeatherNoColoursAndNoPopulation() {
        assertEquals(AtlasBodyArt.Flare.NONE, AtlasBodyArt.PLAIN.flare());
        assertEquals(AtlasBodyArt.Storm.NONE, AtlasBodyArt.PLAIN.storm());
        assertEquals(AtlasBodyArt.Field.NONE, AtlasBodyArt.PLAIN.field());
        assertEquals(AtlasBodyArt.Nebula.NONE, AtlasBodyArt.PLAIN.nebula());
        assertNull(AtlasBodyArt.PLAIN.lightColour());
        assertNull(AtlasBodyArt.PLAIN.tintColour());
        assertTrue(AtlasBodyArt.PLAIN.cityLights().isEmpty());
        assertTrue(AtlasBodyArt.PLAIN.convoys().isEmpty());
        assertTrue(AtlasBodyArt.PLAIN.haulers().isEmpty());
        assertEquals(AtlasBodyArt.Scene.NONE, AtlasBodyArt.PLAIN.scene());
        assertTrue(AtlasBodyArt.PLAIN.wreckHulls().isEmpty());
        assertTrue(AtlasBodyArt.PLAIN.rivalHulls().isEmpty());
        assertNull(AtlasBodyArt.PLAIN.rivalColour());
    }

    @Test
    void aFlagshipLoadsAsAShipGroupWithItsSceneHullsAndTravelHeading() {
        Map<String, AtlasBody> bodies = bodies();
        AtlasBodyArt command = bodies.get("command").art();

        assertEquals(AtlasBodyArt.Kind.FLAGSHIP, command.kind());
        assertEquals(AtlasBodyArt.Scene.COMMAND, command.scene());
        assertEquals(List.of("onslaught", "enforcer", "lasher", "wolf"), command.hullIds());
        assertEquals(130f, command.size());
        assertEquals(AtlasBodyArt.Facing.TRAVEL, command.facing());
        assertEquals(45f, command.heading());
        assertEquals("#ffc850", bodies.get("command").colour());
        assertTrue(command.isShipGroup());
        assertFalse(command.isPlanet());
        assertNull(command.planetType());
        assertTrue(command.wreckHulls().isEmpty());
        assertTrue(command.rivalHulls().isEmpty());
        assertNull(command.rivalColour());
    }

    @Test
    void aTrophyFlagshipKeepsItsWreckAndAStandoffKeepsItsRivalsAndTheirColour() {
        Map<String, AtlasBody> bodies = bodies();
        AtlasBodyArt trophy = bodies.get("trophy").art();
        AtlasBodyArt warfare = bodies.get("warfare").art();

        assertEquals(AtlasBodyArt.Scene.TROPHY, trophy.scene());
        assertEquals(List.of("dominator"), trophy.wreckHulls());
        assertEquals(List.of("paragon", "omen"), trophy.hullIds());
        assertEquals(AtlasBodyArt.Scene.STANDOFF, warfare.scene());
        assertEquals(List.of("odyssey", "tempest"), warfare.hullIds());
        assertEquals(List.of("retribution", "lasher"), warfare.rivalHulls());
        assertEquals("#ff5a50", warfare.rivalColour());
        assertTrue(warfare.wreckHulls().isEmpty());
    }

    @Test
    void aFlagshipWithoutAKnownSceneHasNoneAndAnythingElseHasNoScene() {
        Map<String, AtlasBody> bodies = bodies();

        assertEquals(AtlasBodyArt.Scene.NONE, bodies.get("bare_flagship").art().scene());
        assertEquals(AtlasBodyArt.Kind.FLAGSHIP, bodies.get("bare_flagship").art().kind());
        assertEquals(AtlasBodyArt.Scene.NONE, bodies.get("odd_scene").art().scene());
        assertEquals(List.of("conquest"), bodies.get("odd_scene").art().hullIds());
        assertEquals(AtlasBodyArt.Scene.NONE, bodies.get("rod").art().scene());
        assertEquals(AtlasBodyArt.NO_FLAGSHIP, bodies.get("rod").art().flagship());
    }
}
