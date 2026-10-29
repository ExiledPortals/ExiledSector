package exiledsector.charts.content;

import exiledsector.ProjectFiles;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.content.ChartBosses.Definition;
import exiledsector.charts.content.ChartBosses.Station;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartBossesTest {

    @AfterEach
    void tearDown() {
        ChartBosses.resetForTests();
    }

    private static List<String> variants(List<Station> stations) {
        return stations.stream().map(Station::variantId).toList();
    }

    @Test
    void theShippedTablesLoadCleanlyAndGiveEveryFactionAStationAtEveryTier() throws Exception {
        assertTrue(ChartBosses.register(ChartTables.csvRows(ChartBosses.PATH)).isEmpty());
        assertTrue(ChartBosses.registerStations(ChartTables.csvRows(ChartBosses.STATIONS_PATH)).isEmpty());

        Definition station = ChartBosses.definition(ChartBossType.STATION);
        assertTrue(station.fleetShare() > 0f && station.fleetShare() < 1f);
        assertEquals(1f, ChartBosses.definition(ChartBossType.FLEET).fleetShare());
        assertEquals(List.of("station1_hightech_Standard"), variants(ChartBosses.stationsFor("tritachyon", 3)));
        assertEquals(List.of("station3_midline_Standard"), variants(ChartBosses.stationsFor("independent", 12)));
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            for (String factionId : List.of("pirates", "independent", "some_mod_faction")) {
                assertFalse(ChartBosses.stationsFor(factionId, tier).isEmpty(), factionId + " at tier " + tier);
            }
        }
    }

    @Test
    void theRemnantAndDerelictsHaveNoStationRowsAndNeverTakeTheSharedOnesSoTheirBossIsAlwaysAFleet() throws Exception {
        ChartBosses.register(ChartTables.csvRows(ChartBosses.PATH));
        ChartBosses.registerStations(ChartTables.csvRows(ChartBosses.STATIONS_PATH));

        for (String factionId : List.of("remnant", "derelict")) {
            assertTrue(ChartBosses.definition(ChartBossType.STATION).excludes(factionId));
            for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
                assertEquals(List.of(), ChartBosses.stationsFor(factionId, tier), factionId + " at tier " + tier);
                assertNull(ChartBosses.pickStation(factionId, tier, new Random(tier)));
                for (long seed = 0; seed < 10; seed++) {
                    assertEquals(ChartBossType.FLEET, ChartBosses.pickType(tier, factionId, new Random(seed)), factionId + " at tier " + tier);
                }
            }
        }
        String stations = Files.readString(ProjectFiles.projectRoot().resolve(ChartBosses.STATIONS_PATH), StandardCharsets.UTF_8);
        assertFalse(stations.contains("remnant_station2"));
        assertFalse(stations.contains("station_derelict_survey_mothership"));
    }

    @Test
    void anExcludedFactionTakesNeitherItsOwnNorTheSharedStations() {
        ChartBosses.useForTests(List.of(new Definition(ChartBossType.STATION, 1f, 1, 15, 0.5f, Set.of("remnant"))),
                List.of(new Station("own", Set.of("remnant"), 1, 15, 1f), new Station("shared", Set.of(), 1, 15, 1f)));

        assertEquals(List.of(), ChartBosses.stationsFor("remnant", 5));
        assertEquals(List.of("shared"), variants(ChartBosses.stationsFor("pirates", 5)));
        for (long seed = 0; seed < 20; seed++) {
            assertEquals(ChartBossType.FLEET, ChartBosses.pickType(5, "remnant", new Random(seed)));
        }
    }

    @Test
    void anUnknownBossTypeIsSkippedAndAnEmptyTableKeepsTheFleetBoss() throws Exception {
        List<String> problems = ChartBosses.register(new JSONArray().put(new JSONObject().put("id", "dragon").put("weight", "1")));

        assertFalse(problems.isEmpty());
        assertEquals(ChartBossType.FLEET, ChartBosses.pickType(5, "pirates", new Random(1)));
        assertNull(ChartBosses.definition(ChartBossType.STATION));
    }

    @Test
    void aStationBossNeedsAStationForTheFactionAndTier() {
        ChartBosses.useForTests(List.of(new Definition(ChartBossType.STATION, 1f, 1, 15, 0.5f)),
                List.of(new Station("remnant_station2_Standard", Set.of("remnant"), 4, 15, 1f)));

        for (long seed = 0; seed < 20; seed++) {
            assertEquals(ChartBossType.FLEET, ChartBosses.pickType(9, "pirates", new Random(seed)));
            assertEquals(ChartBossType.FLEET, ChartBosses.pickType(3, "remnant", new Random(seed)));
            assertEquals(ChartBossType.STATION, ChartBosses.pickType(9, "remnant", new Random(seed)));
        }
        assertNull(ChartBosses.pickStation("pirates", 9, new Random(1)));
        assertEquals("remnant_station2_Standard", ChartBosses.pickStation("remnant", 9, new Random(1)));
    }

    @Test
    void aFactionWithItsOwnStationsSkipsTheSharedOnesOnlyWhereItHasOne() {
        ChartBosses.useForTests(List.of(),
                List.of(new Station("own", Set.of("hegemony"), 1, 5, 1f), new Station("shared", Set.of(), 1, 15, 1f)));

        assertEquals(List.of("own"), variants(ChartBosses.stationsFor("hegemony", 3)));
        assertEquals(List.of("shared"), variants(ChartBosses.stationsFor("hegemony", 9)));
        assertEquals(List.of("shared"), variants(ChartBosses.stationsFor("pirates", 3)));
    }

    @Test
    void theThreatNeverTakesASharedStationSoItsBossIsAlwaysAFleet() {
        ChartBosses.useForTests(List.of(new Definition(ChartBossType.STATION, 1f, 1, 15, 0.5f)), List.of(new Station("shared", Set.of(), 1, 15, 1f)));

        assertTrue(ChartBosses.stationsFor("threat", 9).isEmpty());
        for (long seed = 0; seed < 20; seed++) {
            assertEquals(ChartBossType.FLEET, ChartBosses.pickType(9, "threat", new Random(seed)));
        }
    }

    @Test
    void everyBossTypeHasATableKeyAndAnObjectiveDescriptionInEveryCatalogue() throws Exception {
        for (ChartBossType type : ChartBossType.values()) {
            assertEquals(type, ChartBossType.byKey(type.key()));
            assertEquals(type, ChartBossType.parse(type.name()));
            for (String catalogue : List.of("en.json", "zh_CN.json")) {
                String text = Files.readString(ProjectFiles.projectRoot().resolve("data/strings/exiledSector/starCharts/" + catalogue),
                        StandardCharsets.UTF_8);
                assertTrue(text.contains("\"chart.objective.boss." + type.key() + ".description\""), type + " in " + catalogue);
            }
        }
        assertEquals(ChartBossType.FLEET, ChartBossType.parse(null));
    }
}
