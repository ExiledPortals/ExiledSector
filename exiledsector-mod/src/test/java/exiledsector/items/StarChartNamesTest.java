package exiledsector.items;

import exiledlib.ProjectFiles;
import exiledlib.items.AffixLayout;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemModifiers;
import exiledlib.items.ItemNames;
import exiledlib.items.ItemRarity;
import exiledlib.items.ItemRoller;
import exiledlib.items.ItemType;
import exiledlib.items.RolledModifier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarChartNamesTest {

    private JSONObject shippedWords;

    @BeforeEach
    void setUp() throws Exception {
        ItemFixtures.registerShippedItems();
        StarChartFixtures.registerShipped();
        shippedWords = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(StarChartNames.DATA_PATH));
        StarChartNames.register(shippedWords);
    }

    @AfterEach
    void tearDown() {
        ItemNames.clear();
        StarChartFixtures.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static List<String> words(JSONObject json, String list) throws Exception {
        JSONArray entries = json.getJSONArray(list);
        List<String> words = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            words.add(entries.getString(i));
        }
        return words;
    }

    @Test
    void rareChartsTakeANameFromTheChartWordsAndKeepItThroughTheCodec() throws Exception {
        List<String> firstWords = words(shippedWords, "first");
        List<String> secondWords = words(shippedWords, "second");
        int rareCharts = 0;
        for (long seed = 0; seed < 200; seed++) {
            StarChart chart = StarChartFixtures.chart("star_chart_binary_close", seed, 8);
            if (chart.rarity() != ItemRarity.RARE) {
                continue;
            }
            rareCharts++;
            String[] nameParts = chart.name().split(" ");
            assertEquals(2, nameParts.length, chart.name());
            assertTrue(firstWords.contains(nameParts[0]), chart.name());
            assertTrue(secondWords.contains(nameParts[1]), chart.name());
            assertEquals(chart.name(), ItemData.of(chart).preview().name());
        }
        assertTrue(rareCharts > 20);
    }

    @Test
    void theChartWordsNeverNameAShipItem() {
        ItemDefinition shipItem = ItemDefinitions.all().stream().filter(definition -> definition.type() == ItemType.HULL_ITEM && !definition.unique())
                .findFirst().orElseThrow();
        List<RolledModifier> threeModifiers = new ArrayList<>();
        for (long seed = 0; threeModifiers.size() < 3 && seed < 500; seed++) {
            List<RolledModifier> rolled = ItemRoller.roll(shipItem, seed);
            if (rolled.size() >= 3) {
                threeModifiers = rolled;
            }
        }
        assertEquals(ItemRarity.RARE, AffixLayout.rarityFor(shipItem, threeModifiers.size()));

        assertNull(ItemNames.freeze(shipItem, 4L, threeModifiers));
    }

    @Test
    void withoutChartWordsARareChartKeepsItsBaseName() throws Exception {
        StarChartNames.register(null);
        for (long seed = 0; seed < 100; seed++) {
            StarChart chart = StarChartFixtures.chart("star_chart_single", seed, 9);
            if (chart.rarity() == ItemRarity.RARE) {
                assertNotNull(chart.definition());
                assertEquals(chart.definition().displayName(), chart.name());
                return;
            }
        }
        throw new AssertionError("no rare chart rolled");
    }
}
