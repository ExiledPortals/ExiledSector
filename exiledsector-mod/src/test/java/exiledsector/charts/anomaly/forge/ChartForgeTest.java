package exiledsector.charts.anomaly.forge;

import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.TestItems;
import exiledsector.items.crafting.ForgeChoice;
import exiledsector.items.crafting.ForgeOperation;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.items.crafting.material.InfusedFeedstock;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.modifier.ModifierSources;
import exiledsector.items.modifier.RolledLine;
import exiledsector.items.modifier.RolledModifier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartForgeTest {

    private static final String RUN_ID = "run1";
    private static final String PLATE = "forge_test_plate";
    private static final String BIOME = "forge_test_biome";
    private static final String BIOME_MODIFIER = "TIMEFLOW_PERCENT_FORGE_TEST_BIOME";

    private MockSector mockSector;
    private final Map<String, Integer> storedMaterials = new HashMap<>();
    private final CraftingMaterials materials = CraftingMaterials.storedForTests(storedMaterials);
    private ForgePrograms.Program excise;
    private ForgePrograms.Program biomeInfuse;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        ItemModifiers.register(new JSONArray().put(new JSONObject().put("id", BIOME_MODIFIER).put("affix", "prefix").put("scopes", "hull_item")
                .put("lines", "TIMEFLOW_PERCENT:3:5").put("name", "Chronal")));
        BiomeModifiers.register(new JSONArray().put(new JSONObject().put("modifier", BIOME_MODIFIER).put("biome", BIOME).put("item", "armor_plating")));
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", PLATE).put("name", "Forge Plate")
                .put("icon", "graphics/icons/cargo/chip1.png").put("rarity", "20").put("prefixes", "FLUX_CAPACITY_MULT:4:6; HULL_MULT:4:6")
                .put("suffixes", "ARMOR_PERCENT:6:9").put("section", "armor_plating")));
        ForgePrograms.register(new JSONArray()
                .put(program("excise", "hull_item", 20, 0))
                .put(program("biome_infuse", "hull_item", 0, 30))
                .put(program("graft", "subroutine", 20, 0)));
        excise = ForgePrograms.byId("excise");
        biomeInfuse = ForgePrograms.byId("biome_infuse");
    }

    @AfterEach
    void tearDown() {
        ForgePrograms.resetForTests();
        BiomeModifiers.unregisterAll();
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
        mockSector.close();
    }

    private static JSONObject program(String id, String itemTypes, int feedstock, int infusedFeedstockEach) throws Exception {
        return new JSONObject().put("id", id).put("item_types", itemTypes).put("feedstock", Integer.toString(feedstock))
                .put("infused_feedstock_each", Integer.toString(infusedFeedstockEach));
    }

    private static ChartRun liveForgeRun(List<ForgePrograms.Program> programs, int runs, String infusionBiome) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(1, "pirates", "boss", 3, 2, List.of(DerelictForge.ANOMALY_ID)));
        run.recordArrival();
        ForgeStates.record(ForgeStates.of(run, ForgeSite.DERELICT_FORGE), programs, runs, infusionBiome);
        ChartRuns.store(run);
        return run;
    }

    private static ChartRun liveForgeRun(List<ForgePrograms.Program> programs, int runs) {
        return liveForgeRun(programs, runs, null);
    }

    private void owns(int feedstock, int infusedFeedstockEach) {
        storedMaterials.put(ChartForge.FEEDSTOCK_ITEM_ID, feedstock);
        for (InfusedFeedstock infused : InfusedFeedstock.values()) {
            storedMaterials.put(infused.itemId(), infusedFeedstockEach);
        }
    }

    private int infusedCount(InfusedFeedstock infused) {
        return materials.count(infused.itemId());
    }

    private static Item plate() {
        return TestItems.withLines("plate", PLATE, 1L, List.of(new RolledLine("FLUX_CAPACITY_MULT", 5f), new RolledLine("ARMOR_PERCENT", 7f)));
    }

    @Test
    void runningAProgramChangesTheItemTakesTheFeedstockAndSpendsARun() {
        ChartRun run = liveForgeRun(List.of(excise), 1);
        owns(50, 50);
        Item item = plate();

        assertTrue(ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, excise, item, materials).allowed());
        assertTrue(ChartForge.run(run, ForgeSite.DERELICT_FORGE, excise, item, ForgeChoice.modifier(item.explicits().get(1)), materials, new Random(1L)));

        assertEquals(1, item.explicits().size());
        assertEquals(30, materials.count(ChartForge.FEEDSTOCK_ITEM_ID));
        for (InfusedFeedstock infused : InfusedFeedstock.values()) {
            assertEquals(50, infusedCount(infused), infused.itemId());
        }
        assertEquals(0, ForgeStates.runsLeft(run, ForgeSite.DERELICT_FORGE));
        assertSame(ChartForge.DARK, ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, excise, item, materials));
    }

    @Test
    void biomeInfusionAddsTheSystemsModifierAndTakesEachInfusedFeedstock() {
        ChartRun run = liveForgeRun(List.of(biomeInfuse), 1, BIOME);
        owns(50, 40);
        Item item = plate();

        assertTrue(ChartForge.run(run, ForgeSite.DERELICT_FORGE, biomeInfuse, item, ForgeChoice.WHOLE_ITEM, materials, new Random(1L)));

        assertTrue(item.explicits().stream().anyMatch(rolled -> rolled.modifierId().equals(BIOME_MODIFIER)));
        assertEquals(50, materials.count(ChartForge.FEEDSTOCK_ITEM_ID));
        for (InfusedFeedstock infused : InfusedFeedstock.values()) {
            assertEquals(10, infusedCount(infused), infused.itemId());
        }
    }

    @Test
    void biomeInfusionNeedsEveryInfusedFeedstockAndRefusesWhatAbyssalInfusionRefuses() {
        ChartRun run = liveForgeRun(List.of(biomeInfuse), 2, BIOME);
        owns(0, 30);
        storedMaterials.put(InfusedFeedstock.PHASE.itemId(), 29);

        assertSame(ChartForge.COST, ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, biomeInfuse, plate(), materials));
        storedMaterials.put(InfusedFeedstock.PHASE.itemId(), 30);
        Item item = plate();
        assertTrue(ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, biomeInfuse, item, materials).allowed());
        assertTrue(ChartForge.run(run, ForgeSite.DERELICT_FORGE, biomeInfuse, item, ForgeChoice.WHOLE_ITEM, materials, new Random(1L)));
        assertEquals("ui.forge.refused.biome_infuse.present", ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, biomeInfuse, item, materials).refusalKey());
    }

    @Test
    void aProgramThatCannotRunCostsNothing() {
        ChartRun run = liveForgeRun(List.of(excise), 1);
        owns(50, 50);
        Item item = plate();
        RolledModifier notOnTheItem = plate().explicits().get(0).asImmutable();

        assertFalse(ChartForge.run(run, ForgeSite.DERELICT_FORGE, excise, item, ForgeChoice.modifier(notOnTheItem), materials, new Random(1L)));

        assertEquals(1, ForgeStates.runsLeft(run, ForgeSite.DERELICT_FORGE));
        assertEquals(2, item.explicits().size());
        assertEquals(50, materials.count(ChartForge.FEEDSTOCK_ITEM_ID));
    }

    @Test
    void theForgeRefusesWhatItCannotPayForOrDoesNotOffer() {
        ForgePrograms.Program subroutineGraft = ForgePrograms.byId("graft");
        ChartRun run = liveForgeRun(List.of(excise), 1);
        owns(5, 0);

        assertSame(ChartForge.COST, ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, excise, plate(), materials));
        assertSame(ChartForge.DARK, ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, subroutineGraft, plate(), materials));
        assertEquals("ui.forge.refused.loadFirst", ChartForge.verdict(run, ForgeSite.DERELICT_FORGE, excise, null, materials).refusalKey());
        assertSame(ChartForge.DARK, ChartForge.verdict(null, ForgeSite.DERELICT_FORGE, excise, plate(), materials));
        ChartRun bothPrograms = liveForgeRun(List.of(excise, subroutineGraft), 1);
        assertSame(ChartForge.ITEM_TYPE, ChartForge.verdict(bothPrograms, ForgeSite.DERELICT_FORGE, subroutineGraft, plate(), materials));
    }

    @Test
    void theCostIsFixedAtEveryTierAndTheCostLineScalesBothFeedstocks() {
        ChartRun run = liveForgeRun(List.of(excise, biomeInfuse), 1);

        assertEquals(new ForgeRules.Cost(20, 0), ChartForge.costFor(run, ForgeSite.DERELICT_FORGE, excise));
        assertEquals(new ForgeRules.Cost(0, 30), ChartForge.costFor(run, ForgeSite.DERELICT_FORGE, biomeInfuse));

        run.recordAtlasTreeLines(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_FORGE_COST_PERCENT, 50f)));
        assertEquals(new ForgeRules.Cost(30, 0), ChartForge.costFor(run, ForgeSite.DERELICT_FORGE, excise));
        assertEquals(new ForgeRules.Cost(0, 45), ChartForge.costFor(run, ForgeSite.DERELICT_FORGE, biomeInfuse));
        assertNull(ChartForge.liveRun("another", ForgeSite.DERELICT_FORGE));
        assertEquals(ForgeOperation.EXCISE, excise.operation());
    }
}
