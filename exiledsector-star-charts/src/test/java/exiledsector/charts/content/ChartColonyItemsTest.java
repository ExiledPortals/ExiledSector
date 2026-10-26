package exiledsector.charts.content;

import exiledlib.ProjectFiles;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTiers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartColonyItemsTest {

    @AfterEach
    void tearDown() {
        ChartColonyItems.clear();
        ChartTiers.resetForTests();
    }

    private static JSONObject row(String id, String value) throws Exception {
        return new JSONObject().put("id", id).put("value", value);
    }

    @Test
    void theShippedTableParsesWithItsCeilingAndVanillaWeights() throws Exception {
        List<String> problems = ChartColonyItems.register(ChartTables.csvRows(ChartColonyItems.PATH), itemId -> true);

        assertEquals(List.of(), problems);
        assertEquals(0.25f, ChartColonyItems.ceiling());
        assertEquals(14, ChartColonyItems.items().size());
        assertTrue(ChartColonyItems.items().contains(new ChartColonyItems.Item("pristine_nanoforge", 3f)));
    }

    @Test
    void badRowsAreSkippedAndABadCeilingFallsBackToTheDefault() throws Exception {
        List<String> problems = ChartColonyItems.register(new JSONArray().put(row("ceiling", "2")).put(row("synchrotron", "0"))
                .put(row("missing_item", "10")).put(row("mantle_bore", "10")), Set.of("synchrotron", "mantle_bore")::contains);

        assertEquals(3, problems.size(), problems.toString());
        assertEquals(ChartColonyItems.DEFAULT_CEILING, ChartColonyItems.ceiling());
        assertEquals(List.of(new ChartColonyItems.Item("mantle_bore", 10f)), ChartColonyItems.items());
    }

    @Test
    void aCeilingOfOneWouldMakeAnItemCertainSoItIsRefused() throws Exception {
        List<String> problems = ChartColonyItems.register(new JSONArray().put(row("ceiling", "1")), itemId -> true);

        assertEquals(1, problems.size(), problems.toString());
        assertEquals(ChartColonyItems.DEFAULT_CEILING, ChartColonyItems.ceiling());
    }

    @Test
    void theSoftCeilingKeepsSmallChancesAndNeverReachesTheCeiling() throws Exception {
        ChartColonyItems.register(new JSONArray().put(row("ceiling", "0.25")), itemId -> true);

        assertEquals(0f, ChartColonyItems.chance(0f, 100f));
        assertEquals(0f, ChartColonyItems.chance(0.02f, 0f));
        assertEquals(0.01f, ChartColonyItems.chance(0.01f, 1f), 0.0003f);
        assertTrue(ChartColonyItems.chance(0.04f, 2f) > ChartColonyItems.chance(0.04f, 1f));
        assertTrue(ChartColonyItems.chance(0.04f, 1000f) <= 0.25f);
        assertTrue(ChartColonyItems.chance(0.04f, 4f) < 0.16f);
    }

    @Test
    void theColonyItemSuffixIsOnlyRollableFromTheFirstTierWithAChance() throws Exception {
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));
        int firstTier = ChartTiers.firstColonyItemTier();

        assertTrue(firstTier > 1);
        assertEquals(0f, ChartTiers.colonyItemChance(firstTier - 1));
        assertTrue(ChartTiers.colonyItemChance(ChartTiers.HIGHEST_TIER) > ChartTiers.colonyItemChance(firstTier));
        String chartPools = Files.readString(exiledlib.ProjectFiles.projectRoot().resolve("data/config/exiledSector/star_charts.csv"),
                StandardCharsets.UTF_8);
        int pooled = 0;
        for (String line : chartPools.split("\n")) {
            if (line.contains("@CHART_COLONY_ITEM_PERCENT")) {
                assertTrue(line.contains("@CHART_COLONY_ITEM_PERCENT:3:t" + firstTier + "+"), line);
                pooled++;
            }
        }
        assertTrue(pooled > 0);
    }
}
