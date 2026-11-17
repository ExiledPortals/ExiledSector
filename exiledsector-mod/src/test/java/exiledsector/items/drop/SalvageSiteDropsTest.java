package exiledsector.items.drop;

import exiledsector.ProjectFiles;
import exiledsector.compat.SalvageSiteCompat;
import exiledsector.items.HullItemRows;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemFixtures;
import exiledsector.items.ItemKind;
import exiledsector.items.crafting.ItemDisassembly;
import exiledsector.items.crafting.material.CraftingInhibitor;
import exiledsector.items.crafting.material.Nanoswarm;
import exiledsector.items.subroutine.SubroutineKind;
import exiledsector.loot.DropType;
import exiledsector.loot.LootMultipliers;
import lunalib.lunaSettings.LunaSettings;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static exiledsector.items.HullItemRows.MILITARY;
import static exiledsector.items.HullItemRows.MILITARY_PREFIXES;
import static exiledsector.items.HullItemRows.MILITARY_SUFFIXES;
import static exiledsector.items.HullItemRows.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalvageSiteDropsTest {

    private static final int TRIALS = 20000;

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(row(MILITARY, HullItemRows.MILITARY_SECTION, MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(ItemFixtures.subroutineRow("relic", SubroutineKind.UNIQUE).put("unique", "true")));
        SalvageSiteDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "probe").put("chances", "0.5").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", "station").put("chances", "1;0.6;0.3").put("uniqueChance", "0.19"))
                .put(new JSONObject().put("site", "research").put("chances", "1;0.6;0.3").put("uniqueChance", "0").put("feedstockMin", "20").put("feedstockMax", "30"))
                .put(new JSONObject().put("site", "broken").put("chances", "lots").put("uniqueChance", "0")));
        CraftingDrops.register(ProjectFiles.csvRows(CraftingDrops.DATA_PATH));
    }

    @AfterEach
    void tearDown() {
        SalvageSiteDrops.clear();
        ItemDefinitions.clear();
        CraftingDrops.clear();
    }

    private static double share(String site, float mult, java.util.function.Predicate<List<ItemData>> test) {
        return share(site, mult, definition -> true, test);
    }

    private static double share(String site, float mult, java.util.function.Predicate<ItemDefinition> uniqueAllowed,
                                java.util.function.Predicate<List<ItemData>> test) {
        Random random = new Random(11L);
        int hits = 0;
        for (int i = 0; i < TRIALS; i++) {
            if (test.test(SalvageSiteDrops.roll(site, random, ItemChances.atShare(mult), uniqueAllowed))) {
                hits++;
            }
        }
        return hits / (double) TRIALS;
    }

    private static double basicShare(String site, float mult, java.util.function.Predicate<List<ItemData>> test) {
        Random random = new Random(11L);
        int hits = 0;
        for (int i = 0; i < TRIALS; i++) {
            if (test.test(HullItemDrops.rollHullItemLoot(site, random, ItemChances.atShare(mult)).items())) {
                hits++;
            }
        }
        return hits / (double) TRIALS;
    }

    private static long basics(List<ItemData> items) {
        return items.stream().filter(item -> MILITARY.equals(item.definitionId())).count();
    }

    @Test
    void eachExtraNeedsThePreviousToLandAndHullItemsKeepTheirSalvageShareOfEachItem() {
        assertEquals(0.25, basicShare("probe", 1f, items -> basics(items) >= 1), 0.02);
        assertEquals(0.6725, basicShare("station", 1f, items -> basics(items) >= 1), 0.02);
        assertEquals(0.195, basicShare("station", 1f, items -> basics(items) >= 2), 0.02);
        assertEquals(0.0225, basicShare("station", 1f, items -> basics(items) >= 3), 0.01);
        assertEquals(0.0, basicShare("station", 1f, items -> basics(items) >= 4), 0.0);
    }

    @Test
    void uniquesRollSeparatelyAndOnlyWhereTheSiteAllowsThem() {
        assertEquals(0.19, share("station", 1f, items -> items.stream().anyMatch(item -> "relic".equals(item.definitionId()))), 0.02);
        assertEquals(0.0, share("probe", 1f, items -> items.stream().anyMatch(item -> "relic".equals(item.definitionId()))), 0.0);
    }

    @Test
    void aUniqueSubroutineReadsTheIncreasedQuantityOfSubroutines() {
        ItemChances moreSubroutines = ItemChances.increasedBy(DropType.SUBROUTINE, 100f);
        Random random = new Random(11L);
        int relics = 0;
        for (int i = 0; i < TRIALS; i++) {
            if (SalvageSiteDrops.roll("station", random, moreSubroutines, definition -> true).stream().anyMatch(item -> "relic".equals(item.definitionId()))) {
                relics++;
            }
        }

        assertEquals(0.38, relics / (double) TRIALS, 0.02);
    }

    @Test
    void aUniqueHullItemKeepsItsSalvageShareOfTheSiteUniqueRoll() throws Exception {
        ItemDefinitions.register(new JSONArray().put(row("bridge_relic", "bridge", "HULL_PERCENT:4:6").put("unique", "true")));

        assertEquals(0.19 * HullItemDrops.HULL_ITEM_SALVAGE_SHARE,
                share("station", 1f, items -> items.stream().anyMatch(item -> "bridge_relic".equals(item.definitionId()))), 0.02);
    }

    @Test
    void aUniqueThatCannotDropYetIsNeverPickedAndTheRollWithoutASectorDropsNoUniques() {
        assertEquals(0.0, share("station", 1f, definition -> false, items -> items.stream().anyMatch(item -> "relic".equals(item.definitionId()))), 0.0);
        Random random = new Random(2L);
        for (int i = 0; i < 200; i++) {
            assertTrue(SalvageSiteDrops.roll("station", random, ItemChances.BASE).stream().noneMatch(item -> "relic".equals(item.definitionId())));
        }
    }

    @Test
    void aChanceMultiplierScalesEveryRoll() {
        assertEquals(0.125, basicShare("probe", 0.5f, items -> !items.isEmpty()), 0.02);
    }

    @Test
    void unknownOrMalformedSitesDropNothing() {
        assertFalse(SalvageSiteDrops.hasRule("broken"));
        assertTrue(SalvageSiteDrops.roll("nowhere", new Random(1L), ItemChances.BASE).isEmpty());
        assertTrue(SalvageSiteDrops.roll(null, new Random(1L), ItemChances.BASE).isEmpty());
    }

    @Test
    void basicDrawsNeverPickAUniqueAndUniqueDrawsOnlyPickUniques() {
        Random random = new Random(5L);
        for (int i = 0; i < 200; i++) {
            assertEquals(MILITARY, DroppableDefinitions.pickBasicOfKind(ItemKind.BRIDGE, random).id());
            assertEquals("relic", DroppableDefinitions.pickUnique(random, definition -> true).id());
        }
    }

    @Test
    void theShippedTableGivesTheAgreedChances() throws Exception {
        String csv = Files.readString(ProjectFiles.projectRoot().resolve(SalvageSiteDrops.DATA_PATH), StandardCharsets.UTF_8);
        JSONArray rows = CDL.toJSONArray(csv.replace("\r\n", "\n"));
        SalvageSiteDrops.register(rows);

        assertEquals(rows.length(), countRules(rows));
        Map<String, Float> firstChances = Map.of("derelict_probe", 0.5f, "derelict_survey_ship", 0.8f, "derelict_mothership", 1f,
                "station_research", 1f, "station_mining", 1f, "orbital_habitat", 1f, SalvageSiteDrops.TECH_MINING_FIRST_FIND, 0.8f,
                "weapons_cache", 0.4f, "wreck", 0.1f);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String site = row.getString("site");
            float first = Float.parseFloat(row.getString("chances").split(";")[0]);
            float unique = Float.parseFloat(row.getString("uniqueChance"));
            assertTrue(first >= 0f && first <= 1f && unique >= 0f && unique <= 1f, site);
            if (firstChances.containsKey(site)) {
                assertEquals(firstChances.get(site), first, 1e-6f, site);
            }
            if (site.contains("derelict") || site.contains("probe") || site.contains("cache") || site.equals("wreck")) {
                assertEquals(0f, unique, site + " should never drop uniques");
            }
        }
    }

    @Test
    void otherModSitesOnlyDropWhileTheSettingIsOnAndNeverOverrideOurOwnRows() throws Exception {
        SalvageSiteDrops.registerOtherMod(new JSONArray()
                .put(new JSONObject().put("site", "rat_abyss_research").put("chances", "1").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", "probe").put("chances", "1").put("uniqueChance", "0")));

        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            luna.when(() -> LunaSettings.getBoolean("exiledSector", SalvageSiteCompat.DROPS_FIELD_ID)).thenReturn(true);
            assertTrue(SalvageSiteDrops.hasRule("rat_abyss_research"));
            assertEquals(1, HullItemDrops.rollHullItemLoot("rat_abyss_research", new Random(1L), ItemChances.BASE).items().size());
            assertEquals(0.25, basicShare("probe", 1f, items -> !items.isEmpty()), 0.02);

            luna.when(() -> LunaSettings.getBoolean("exiledSector", SalvageSiteCompat.DROPS_FIELD_ID)).thenReturn(false);
            assertFalse(SalvageSiteDrops.hasRule("rat_abyss_research"));
            assertTrue(HullItemDrops.rollHullItemLoot("rat_abyss_research", new Random(1L), ItemChances.BASE).items().isEmpty());
            assertTrue(SalvageSiteDrops.hasRule("probe"));
        }
    }

    @Test
    void everyCompatFileBelongsToAListedModAndOnlyNamesSitesOutsideTheVanillaTable() throws Exception {
        Path folder = ProjectFiles.projectRoot().resolve("data/config/exiledSector/compat/salvage");
        Set<String> files = new HashSet<>();
        try (var listing = Files.list(folder)) {
            listing.forEach(path -> files.add(path.getFileName().toString()));
        }
        Set<String> expected = new HashSet<>();
        SalvageSiteCompat.SOURCES.forEach(source -> expected.add(source.modId() + ".csv"));
        assertEquals(expected, files);

        String base = Files.readString(ProjectFiles.projectRoot().resolve(SalvageSiteDrops.DATA_PATH), StandardCharsets.UTF_8);
        Set<String> vanillaSites = new HashSet<>();
        JSONArray baseRows = CDL.toJSONArray(base.replace("\r\n", "\n"));
        for (int i = 0; i < baseRows.length(); i++) {
            vanillaSites.add(baseRows.getJSONObject(i).getString("site"));
        }
        for (String file : files) {
            JSONArray rows = CDL.toJSONArray(Files.readString(folder.resolve(file), StandardCharsets.UTF_8).replace("\r\n", "\n"));
            assertTrue(rows.length() > 0, file);
            for (int i = 0; i < rows.length(); i++) {
                String site = rows.getJSONObject(i).getString("site");
                assertFalse(vanillaSites.contains(site), site + " in " + file + " is a vanilla site");
                Float.parseFloat(rows.getJSONObject(i).getString("chances").split(";")[0]);
            }
        }
    }

    private static int countRules(JSONArray rows) throws Exception {
        int count = 0;
        for (int i = 0; i < rows.length(); i++) {
            if (SalvageSiteDrops.hasRule(rows.getJSONObject(i).getString("site"))) {
                count++;
            }
        }
        return count;
    }

    private static List<Map<String, Integer>> materials(String site, float mult) {
        Random random = new Random(13L);
        List<Map<String, Integer>> rolls = new java.util.ArrayList<>();
        for (int i = 0; i < TRIALS; i++) {
            rolls.add(SalvageSiteDrops.rollMaterials(site, random, new LootMultipliers(ItemChances.atShare(mult), CraftingDrops.NO_BAND_SHIFT)));
        }
        return rolls;
    }

    private static double materialShare(List<Map<String, Integer>> rolls, java.util.function.Predicate<Map<String, Integer>> test) {
        return rolls.stream().filter(test).count() / (double) rolls.size();
    }

    @Test
    void partsDropAlongsideHullItemsWithEachSlotAddingOneToThree() {
        List<Map<String, Integer>> station = materials("station", 1f);
        double average = station.stream().mapToInt(roll -> roll.getOrDefault(ItemDisassembly.FEEDSTOCK_ITEM_ID, 0)).average().orElse(0);

        assertEquals(1.0, materialShare(station, roll -> roll.containsKey(ItemDisassembly.FEEDSTOCK_ITEM_ID)), 0.0);
        assertEquals((1 + 0.6 + 0.3) * 2, average, 0.1);
        assertTrue(station.stream().allMatch(roll -> roll.getOrDefault(ItemDisassembly.FEEDSTOCK_ITEM_ID, 0) <= 9));
        assertEquals(0.5, materialShare(materials("probe", 1f), roll -> roll.containsKey(ItemDisassembly.FEEDSTOCK_ITEM_ID)), 0.02);
        assertEquals(0.25, materialShare(materials("probe", 0.5f), roll -> roll.containsKey(ItemDisassembly.FEEDSTOCK_ITEM_ID)), 0.02);
    }

    @Test
    void aSiteWithAFeedstockRangeRollsWithinItScaledByTheFeedstockBonus() {
        List<Map<String, Integer>> research = materials("research", 1f);
        assertTrue(research.stream().allMatch(roll -> roll.get(ItemDisassembly.FEEDSTOCK_ITEM_ID) >= 20 && roll.get(ItemDisassembly.FEEDSTOCK_ITEM_ID) <= 30));
        assertEquals(java.util.Set.of(20, 30), java.util.Set.of(
                research.stream().mapToInt(roll -> roll.get(ItemDisassembly.FEEDSTOCK_ITEM_ID)).min().orElse(0),
                research.stream().mapToInt(roll -> roll.get(ItemDisassembly.FEEDSTOCK_ITEM_ID)).max().orElse(0)));
        assertEquals(25.0, research.stream().mapToInt(roll -> roll.get(ItemDisassembly.FEEDSTOCK_ITEM_ID)).average().orElse(0), 0.1);
        double halved = materials("research", 0.5f).stream().mapToInt(roll -> roll.get(ItemDisassembly.FEEDSTOCK_ITEM_ID)).average().orElse(0);
        assertEquals(12.5, halved, 0.1);
    }

    @Test
    void aSitesCraftingItemsComeFromTheDropTableWithoutTheChartOnlyOnes() {
        List<Map<String, Integer>> station = materials("station", 1f);
        double share = SalvageSiteDrops.CRAFTING_ITEM_SALVAGE_SHARE;

        assertEquals(share, averageCraftingItems(station), 0.02);
        assertEquals(0.5 * share, averageCraftingItems(materials("probe", 1f)), 0.02);
        assertEquals(share * dropShare(Nanoswarm.AUGMENTATION.itemId()), averageCount(station, Nanoswarm.AUGMENTATION.itemId()), 0.02);
        assertEquals(share * dropShare(Nanoswarm.CORRUPTION.itemId()), averageCount(station, Nanoswarm.CORRUPTION.itemId()), 0.01);
        assertEquals(share * dropShare(CraftingInhibitor.PREFIX.itemId()), averageCount(station, CraftingInhibitor.PREFIX.itemId()), 0.01);
        assertTrue(station.stream().noneMatch(roll -> roll.keySet().stream().anyMatch(CraftingDrops::chartOnly)));
    }

    @Test
    void aSitesBandShiftMovesItsCraftingItemsOutOfTheCommonBand() {
        Random random = new Random(19L);
        List<Map<String, Integer>> shifted = new java.util.ArrayList<>();
        for (int i = 0; i < TRIALS; i++) {
            shifted.add(SalvageSiteDrops.rollMaterials("station", random, new LootMultipliers(ItemChances.BASE, 3f)));
        }
        List<Map<String, Integer>> unshifted = materials("station", 1f);

        assertEquals(averageCraftingItems(unshifted), averageCraftingItems(shifted), 0.02);
        assertTrue(averageCount(shifted, Nanoswarm.CORRUPTION.itemId()) > 1.5 * averageCount(unshifted, Nanoswarm.CORRUPTION.itemId()));
        assertTrue(averageCount(shifted, Nanoswarm.AUGMENTATION.itemId()) < averageCount(unshifted, Nanoswarm.AUGMENTATION.itemId()));
    }

    @Test
    void aSitesInhibitorsScaleByTheInhibitorNodeAndItsOtherCraftingItemsDoNot() {
        Random random = new Random(23L);
        LootMultipliers moreInhibitors = new LootMultipliers(ItemChances.increasedBy(DropType.INHIBITOR, 100f), CraftingDrops.NO_BAND_SHIFT);
        List<Map<String, Integer>> boosted = new java.util.ArrayList<>();
        for (int i = 0; i < TRIALS; i++) {
            boosted.add(SalvageSiteDrops.rollMaterials("station", random, moreInhibitors));
        }
        List<Map<String, Integer>> plain = materials("station", 1f);

        assertEquals(2 * averageCount(plain, CraftingInhibitor.PREFIX.itemId()), averageCount(boosted, CraftingInhibitor.PREFIX.itemId()), 0.004);
        assertEquals(averageCount(plain, Nanoswarm.AUGMENTATION.itemId()), averageCount(boosted, Nanoswarm.AUGMENTATION.itemId()), 0.02);
    }

    private static double averageCraftingItems(List<Map<String, Integer>> rolls) {
        return rolls.stream().mapToInt(SalvageSiteDropsTest::craftingItemCount).average().orElse(0);
    }

    private static double averageCount(List<Map<String, Integer>> rolls, String itemId) {
        return rolls.stream().mapToInt(roll -> roll.getOrDefault(itemId, 0)).average().orElse(0);
    }

    private static int craftingItemCount(Map<String, Integer> roll) {
        return roll.entrySet().stream().filter(entry -> CraftingDrops.covers(entry.getKey())).mapToInt(Map.Entry::getValue).sum();
    }

    private static double dropShare(String itemId) {
        double outsideChartWeight = CraftingDrops.all().stream().filter(drop -> !drop.chartOnly()).mapToDouble(CraftingDrops.Drop::weight).sum();
        return CraftingDrops.weight(itemId) / outsideChartWeight;
    }

    @Test
    void feedstockAndCraftingItemsScaleByTheirOwnNodes() {
        Random random = new Random(17L);
        ItemChances craftingItemsOnly = new ItemChances(Map.of(DropType.FEEDSTOCK, -100f, DropType.CRAFTING_ITEM, 100f), Map.of(), 1f);
        List<Map<String, Integer>> craftingItemOnly = new java.util.ArrayList<>();
        for (int i = 0; i < TRIALS; i++) {
            craftingItemOnly.add(SalvageSiteDrops.rollMaterials("probe", random, new LootMultipliers(craftingItemsOnly, CraftingDrops.NO_BAND_SHIFT)));
        }

        assertEquals(0.0, materialShare(craftingItemOnly, roll -> roll.containsKey(ItemDisassembly.FEEDSTOCK_ITEM_ID)), 0.0);
        assertEquals(2 * 0.5 * SalvageSiteDrops.CRAFTING_ITEM_SALVAGE_SHARE, averageCraftingItems(craftingItemOnly), 0.02);
    }

    @Test
    void sitesWithoutARuleDropNoMaterials() {
        assertTrue(SalvageSiteDrops.rollMaterials("nowhere", new Random(1L), LootMultipliers.NONE).isEmpty());
        assertTrue(SalvageSiteDrops.rollMaterials(null, new Random(1L), LootMultipliers.NONE).isEmpty());
    }
}
