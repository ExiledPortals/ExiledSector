package exiledsector.charts.phasing;

import exiledsector.ProjectFiles;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhasedPlanetTraitsTest {

    private static final PhasedPlanetTrait MANTLE = new PhasedPlanetTrait("mantle", "c_mantle", 1f, Map.of(), Map.of("neutron_star", 10f),
            Set.of("furnace"));
    private static final PhasedPlanetTrait FURNACE = new PhasedPlanetTrait("furnace", "c_furnace", 1f, Map.of("furnace", 8f, "frozen_fringe", 0f),
            Map.of(), Set.of());
    private static final PhasedPlanetTrait THROAT = new PhasedPlanetTrait("throat", "c_throat", 1f, Map.of(), Map.of(), Set.of());

    @AfterEach
    void tearDown() {
        PhasedPlanetTraits.use(List.of());
    }

    private static JSONArray shippedRows(String dataPath) throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    @Test
    void regionAndAnomalyMultipliersScaleTheWeight() {
        assertEquals(1f, MANTLE.weightFor("normal", List.of()));
        assertEquals(10f, MANTLE.weightFor("normal", List.of("neutron_star", "black_hole")));
        assertEquals(8f, FURNACE.weightFor("furnace", null));
        assertEquals(0f, FURNACE.weightFor("frozen_fringe", List.of()));
    }

    @Test
    void higherTiersRollMoreTraits() {
        assertEquals(0, PhasedPlanetTraits.fewestFor(1));
        assertEquals(0, PhasedPlanetTraits.fewestFor(5));
        assertEquals(1, PhasedPlanetTraits.fewestFor(6));
        assertEquals(1, PhasedPlanetTraits.fewestFor(10));
        assertEquals(2, PhasedPlanetTraits.fewestFor(11));
        Random random = new Random(3);
        for (int roll = 0; roll < 50; roll++) {
            int count = PhasedPlanetTraits.countFor(12, random);
            assertTrue(count == 2 || count == 3);
        }
    }

    @Test
    void aRollNeverRepeatsOrPairsExcludedTraits() {
        PhasedPlanetTraits.use(List.of(MANTLE, FURNACE, THROAT));
        for (int seed = 0; seed < 100; seed++) {
            List<String> rolled = PhasedPlanetTraits.roll(15, "normal", List.of(), new Random(seed));
            assertEquals(new HashSet<>(rolled).size(), rolled.size());
            assertFalse(rolled.contains("c_mantle") && rolled.contains("c_furnace"));
        }
    }

    @Test
    void aRegionWithZeroWeightNeverRollsTheTrait() {
        PhasedPlanetTraits.use(List.of(FURNACE, THROAT));
        for (int seed = 0; seed < 50; seed++) {
            assertFalse(PhasedPlanetTraits.roll(15, "frozen_fringe", List.of(), new Random(seed)).contains("c_furnace"));
        }
    }

    @Test
    void rowsAreReadWithTheirMultipliersAndExclusions() throws Exception {
        JSONArray rows = new JSONArray()
                .put(new JSONObject().put("id", "mantle").put("condition", "c_mantle").put("weight", "2")
                        .put("regions", "furnace:0").put("anomalies", "neutron_star:10; black_hole:3").put("excludes", "furnace; throat"))
                .put(new JSONObject().put("id", "ghost").put("condition", "c_missing").put("weight", "1"));

        List<String> problems = PhasedPlanetTraits.register(rows, "c_mantle"::equals);

        assertEquals(1, PhasedPlanetTraits.all().size());
        PhasedPlanetTrait mantle = PhasedPlanetTraits.all().get(0);
        assertEquals(2f, mantle.weight());
        assertEquals(Map.of("furnace", 0f), mantle.regionMultipliers());
        assertEquals(Map.of("neutron_star", 10f, "black_hole", 3f), mantle.anomalyMultipliers());
        assertEquals(Set.of("furnace", "throat"), mantle.excludes());
        assertEquals(1, problems.size());
    }

    @Test
    void everyShippedTraitIsAShippedMarketCondition() throws Exception {
        Set<String> conditionIds = new HashSet<>();
        JSONArray conditions = shippedRows("data/campaign/market_conditions.csv");
        for (int index = 0; index < conditions.length(); index++) {
            conditionIds.add(conditions.getJSONObject(index).getString("id"));
        }
        assertTrue(conditionIds.contains(PhasedPlanetTraits.PHASE_DISPLACED_CONDITION_ID));

        List<String> problems = PhasedPlanetTraits.register(shippedRows(PhasedPlanetTraits.PATH), conditionIds::contains);

        assertEquals(List.of(), problems);
        assertEquals(shippedRows(PhasedPlanetTraits.PATH).length(), PhasedPlanetTraits.all().size());
    }
}
