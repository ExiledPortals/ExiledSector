package exiledsector.hullitems;

import exiledsector.compat.SalvageSiteCompat;
import exiledsector.skills.npc.RealSkillData;
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

import static exiledsector.hullitems.HullItemFixtures.MILITARY;
import static exiledsector.hullitems.HullItemFixtures.MILITARY_PREFIXES;
import static exiledsector.hullitems.HullItemFixtures.MILITARY_SUFFIXES;
import static exiledsector.hullitems.HullItemFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemDropsTest {

    private static final int TRIALS = 20000;

    @BeforeEach
    void setUp() throws Exception {
        HullItemDefinitions.register(new JSONArray()
                .put(row(MILITARY, "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("relic", "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES).put("unique", "true")));
        HullItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "probe").put("chances", "0.5").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", "station").put("chances", "1;0.6;0.3").put("uniqueChance", "0.19"))
                .put(new JSONObject().put("site", "broken").put("chances", "lots").put("uniqueChance", "0")));
    }

    @AfterEach
    void tearDown() {
        HullItemDrops.clear();
        HullItemDefinitions.clear();
    }

    private static double share(String site, float mult, java.util.function.Predicate<List<HullItemData>> test) {
        return share(site, mult, definition -> true, test);
    }

    private static double share(String site, float mult, java.util.function.Predicate<HullItemDefinition> uniqueAllowed,
                                java.util.function.Predicate<List<HullItemData>> test) {
        Random random = new Random(11L);
        int hits = 0;
        for (int i = 0; i < TRIALS; i++) {
            if (test.test(HullItemDrops.roll(site, random, mult, uniqueAllowed))) {
                hits++;
            }
        }
        return hits / (double) TRIALS;
    }

    private static long basics(List<HullItemData> items) {
        return items.stream().filter(item -> MILITARY.equals(item.definitionId())).count();
    }

    @Test
    void theFirstChanceIsTheChanceOfAtLeastOneAndEachExtraNeedsThePreviousToLand() {
        assertEquals(0.5, share("probe", 1f, items -> basics(items) >= 1), 0.02);
        assertEquals(1.0, share("station", 1f, items -> basics(items) >= 1), 0.0);
        assertEquals(0.6, share("station", 1f, items -> basics(items) >= 2), 0.02);
        assertEquals(0.18, share("station", 1f, items -> basics(items) >= 3), 0.02);
        assertEquals(0.0, share("station", 1f, items -> basics(items) >= 4), 0.0);
    }

    @Test
    void uniquesRollSeparatelyAndOnlyWhereTheSiteAllowsThem() {
        assertEquals(0.19, share("station", 1f, items -> items.stream().anyMatch(item -> "relic".equals(item.definitionId()))), 0.02);
        assertEquals(0.0, share("probe", 1f, items -> items.stream().anyMatch(item -> "relic".equals(item.definitionId()))), 0.0);
    }

    @Test
    void aUniqueThatCannotDropYetIsNeverPickedAndTheRollWithoutASectorDropsNoUniques() {
        assertEquals(0.0, share("station", 1f, definition -> false, items -> items.stream().anyMatch(item -> "relic".equals(item.definitionId()))), 0.0);
        Random random = new Random(2L);
        for (int i = 0; i < 200; i++) {
            assertTrue(HullItemDrops.roll("station", random).stream().noneMatch(item -> "relic".equals(item.definitionId())));
        }
    }

    @Test
    void aChanceMultiplierScalesEveryRoll() {
        assertEquals(0.25, share("probe", 0.5f, items -> !items.isEmpty()), 0.02);
    }

    @Test
    void unknownOrMalformedSitesDropNothing() {
        assertFalse(HullItemDrops.hasRule("broken"));
        assertTrue(HullItemDrops.roll("nowhere", new Random(1L)).isEmpty());
        assertTrue(HullItemDrops.roll(null, new Random(1L)).isEmpty());
    }

    @Test
    void basicDrawsNeverPickAUniqueAndUniqueDrawsOnlyPickUniques() {
        Random random = new Random(5L);
        for (int i = 0; i < 200; i++) {
            assertEquals(MILITARY, HullItemDrops.pickBasic(random).id());
            assertEquals("relic", HullItemDrops.pickUnique(random, definition -> true).id());
        }
    }

    @Test
    void theShippedTableGivesTheAgreedChances() throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(HullItemDrops.DATA_PATH), StandardCharsets.UTF_8);
        JSONArray rows = CDL.toJSONArray(csv.replace("\r\n", "\n"));
        HullItemDrops.register(rows);

        assertEquals(rows.length(), countRules(rows));
        Map<String, Float> firstChances = Map.of("derelict_probe", 0.5f, "derelict_survey_ship", 0.8f, "derelict_mothership", 1f,
                "station_research", 1f, "station_mining", 1f, "orbital_habitat", 1f, HullItemDrops.TECH_MINING_FIRST_FIND, 0.8f,
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
        HullItemDrops.registerOtherMod(new JSONArray()
                .put(new JSONObject().put("site", "rat_abyss_research").put("chances", "1").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", "probe").put("chances", "1").put("uniqueChance", "0")));

        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            luna.when(() -> LunaSettings.getBoolean("exiledSector", SalvageSiteCompat.DROPS_FIELD_ID)).thenReturn(true);
            assertTrue(HullItemDrops.hasRule("rat_abyss_research"));
            assertEquals(1, HullItemDrops.roll("rat_abyss_research", new Random(1L)).size());
            assertEquals(0.5, share("probe", 1f, items -> !items.isEmpty()), 0.02);

            luna.when(() -> LunaSettings.getBoolean("exiledSector", SalvageSiteCompat.DROPS_FIELD_ID)).thenReturn(false);
            assertFalse(HullItemDrops.hasRule("rat_abyss_research"));
            assertTrue(HullItemDrops.roll("rat_abyss_research", new Random(1L)).isEmpty());
            assertTrue(HullItemDrops.hasRule("probe"));
        }
    }

    @Test
    void everyCompatFileBelongsToAListedModAndOnlyNamesSitesOutsideTheVanillaTable() throws Exception {
        Path folder = RealSkillData.projectRoot().resolve("data/config/exiledSector/compat/salvage");
        Set<String> files = new HashSet<>();
        try (var listing = Files.list(folder)) {
            listing.forEach(path -> files.add(path.getFileName().toString()));
        }
        Set<String> expected = new HashSet<>();
        SalvageSiteCompat.SOURCES.forEach(source -> expected.add(source.modId() + ".csv"));
        assertEquals(expected, files);

        String base = Files.readString(RealSkillData.projectRoot().resolve(HullItemDrops.DATA_PATH), StandardCharsets.UTF_8);
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
            if (HullItemDrops.hasRule(rows.getJSONObject(i).getString("site"))) {
                count++;
            }
        }
        return count;
    }

    private static List<Map<String, Integer>> materials(String site, float mult) {
        Random random = new Random(13L);
        List<Map<String, Integer>> rolls = new java.util.ArrayList<>();
        for (int i = 0; i < TRIALS; i++) {
            rolls.add(HullItemDrops.rollMaterials(site, random, mult));
        }
        return rolls;
    }

    private static double materialShare(List<Map<String, Integer>> rolls, java.util.function.Predicate<Map<String, Integer>> test) {
        return rolls.stream().filter(test).count() / (double) rolls.size();
    }

    private static boolean hasKernel(Map<String, Integer> roll) {
        return java.util.Arrays.stream(CraftingCurrency.values()).anyMatch(currency -> roll.containsKey(currency.commodityId()));
    }

    @Test
    void partsDropAlongsideHullItemsWithEachSlotAddingOneToThree() {
        List<Map<String, Integer>> station = materials("station", 1f);
        double average = station.stream().mapToInt(roll -> roll.getOrDefault(HullItemDisassembly.PARTS_COMMODITY_ID, 0)).average().orElse(0);

        assertEquals(1.0, materialShare(station, roll -> roll.containsKey(HullItemDisassembly.PARTS_COMMODITY_ID)), 0.0);
        assertEquals((1 + 0.6 + 0.3) * 2, average, 0.1);
        assertTrue(station.stream().allMatch(roll -> roll.getOrDefault(HullItemDisassembly.PARTS_COMMODITY_ID, 0) <= 9));
        assertEquals(0.5, materialShare(materials("probe", 1f), roll -> roll.containsKey(HullItemDisassembly.PARTS_COMMODITY_ID)), 0.02);
        assertEquals(0.25, materialShare(materials("probe", 0.5f), roll -> roll.containsKey(HullItemDisassembly.PARTS_COMMODITY_ID)), 0.02);
    }

    @Test
    void kernelsDropAtAShareOfTheFirstChanceWeightedTowardsTheCheapOnes() {
        List<Map<String, Integer>> station = materials("station", 1f);
        List<Map<String, Integer>> kernels = station.stream().filter(HullItemDropsTest::hasKernel).toList();

        assertEquals(HullItemDrops.KERNEL_CHANCE_SHARE, kernels.size() / (double) TRIALS, 0.02);
        assertEquals(0.5 * HullItemDrops.KERNEL_CHANCE_SHARE, materialShare(materials("probe", 1f), HullItemDropsTest::hasKernel), 0.02);
        assertEquals(0.6, materialShare(kernels, roll -> roll.containsKey(CraftingCurrency.AUGMENTATION.commodityId())), 0.03);
        assertEquals(0.1, materialShare(kernels, roll -> roll.containsKey(CraftingCurrency.TRANSPOSITION.commodityId())), 0.02);
    }

    @Test
    void sitesWithoutARuleDropNoMaterials() {
        assertTrue(HullItemDrops.rollMaterials("nowhere", new Random(1L)).isEmpty());
        assertTrue(HullItemDrops.rollMaterials(null, new Random(1L)).isEmpty());
    }

    @Test
    void fractionalBattlePartsRoundUpAsAChance() {
        Random random = new Random(5L);
        double total = 0;
        for (int i = 0; i < TRIALS; i++) {
            int parts = HullItemDrops.wholeParts(2.25f, random);
            assertTrue(parts == 2 || parts == 3);
            total += parts;
        }
        assertEquals(2.25, total / TRIALS, 0.02);
        assertEquals(0, HullItemDrops.wholeParts(0f, random));
        assertEquals(0, HullItemDrops.wholeParts(Float.NaN, random));
    }
}
