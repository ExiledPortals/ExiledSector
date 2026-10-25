package exiledsector.charts;

import exiledlib.ProjectFiles;
import exiledsector.charts.ChartBosses.Definition;
import exiledsector.charts.ChartBosses.Station;
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
        assertEquals(List.of("remnant_station2_Standard"), variants(ChartBosses.stationsFor("remnant", 8)));
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            for (String factionId : List.of("pirates", "derelict", "some_mod_faction")) {
                assertFalse(ChartBosses.stationsFor(factionId, tier).isEmpty(), factionId + " at tier " + tier);
            }
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
