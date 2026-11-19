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
                    "convoys": [["buffalo", "wolf"], [], ["tarsus", "hermes", "lasher"]] } }
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
        assertTrue(port.cityLights().isEmpty());
        assertEquals("world", bodies.get("port").wheel());
    }

    @Test
    void thePlainArtHasNoStormNoColoursAndNoPopulation() {
        assertEquals(AtlasBodyArt.Storm.NONE, AtlasBodyArt.PLAIN.storm());
        assertNull(AtlasBodyArt.PLAIN.lightColour());
        assertNull(AtlasBodyArt.PLAIN.tintColour());
        assertTrue(AtlasBodyArt.PLAIN.cityLights().isEmpty());
        assertTrue(AtlasBodyArt.PLAIN.convoys().isEmpty());
    }
}
