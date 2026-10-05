package exiledsector.socketables;

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

import static exiledsector.socketables.SocketableFixtures.MILITARY;
import static exiledsector.socketables.SocketableFixtures.MILITARY_PREFIXES;
import static exiledsector.socketables.SocketableFixtures.MILITARY_SUFFIXES;
import static exiledsector.socketables.SocketableFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableDropsTest {

    private static final int TRIALS = 20000;

    @BeforeEach
    void setUp() throws Exception {
        SocketableDefinitions.register(new JSONArray()
                .put(row(MILITARY, "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("relic", "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES).put("unique", "true")));
        SocketableDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "probe").put("chances", "0.5").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", "station").put("chances", "1;0.6;0.3").put("uniqueChance", "0.19"))
                .put(new JSONObject().put("site", "broken").put("chances", "lots").put("uniqueChance", "0")));
    }

    @AfterEach
    void tearDown() {
        SocketableDrops.clear();
        SocketableDefinitions.clear();
    }

    private static double share(String site, float mult, java.util.function.Predicate<List<SocketableItemData>> test) {
        return share(site, mult, definition -> true, test);
    }

    private static double share(String site, float mult, java.util.function.Predicate<SocketableDefinition> uniqueAllowed,
                                java.util.function.Predicate<List<SocketableItemData>> test) {
        Random random = new Random(11L);
        int hits = 0;
        for (int i = 0; i < TRIALS; i++) {
            if (test.test(SocketableDrops.roll(site, random, mult, uniqueAllowed))) {
                hits++;
            }
        }
        return hits / (double) TRIALS;
    }

    private static long basics(List<SocketableItemData> items) {
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
            assertTrue(SocketableDrops.roll("station", random).stream().noneMatch(item -> "relic".equals(item.definitionId())));
        }
    }

    @Test
    void aChanceMultiplierScalesEveryRoll() {
        assertEquals(0.25, share("probe", 0.5f, items -> !items.isEmpty()), 0.02);
    }

    @Test
    void unknownOrMalformedSitesDropNothing() {
        assertFalse(SocketableDrops.hasRule("broken"));
        assertTrue(SocketableDrops.roll("nowhere", new Random(1L)).isEmpty());
        assertTrue(SocketableDrops.roll(null, new Random(1L)).isEmpty());
    }

    @Test
    void basicDrawsNeverPickAUniqueAndUniqueDrawsOnlyPickUniques() {
        Random random = new Random(5L);
        for (int i = 0; i < 200; i++) {
            assertEquals(MILITARY, SocketableDrops.pickBasic(random).id());
            assertEquals("relic", SocketableDrops.pickUnique(random, definition -> true).id());
        }
    }

    @Test
    void theShippedTableGivesTheAgreedChances() throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(SocketableDrops.DATA_PATH), StandardCharsets.UTF_8);
        JSONArray rows = CDL.toJSONArray(csv.replace("\r\n", "\n"));
        SocketableDrops.register(rows);

        assertEquals(rows.length(), countRules(rows));
        Map<String, Float> firstChances = Map.of("derelict_probe", 0.5f, "derelict_survey_ship", 0.8f, "derelict_mothership", 1f,
                "station_research", 1f, "station_mining", 1f, "orbital_habitat", 1f, SocketableDrops.TECH_MINING_FIRST_FIND, 0.8f,
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
        SocketableDrops.registerOtherMod(new JSONArray()
                .put(new JSONObject().put("site", "rat_abyss_research").put("chances", "1").put("uniqueChance", "0"))
                .put(new JSONObject().put("site", "probe").put("chances", "1").put("uniqueChance", "0")));

        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            luna.when(() -> LunaSettings.getBoolean("exiledSector", SalvageSiteCompat.DROPS_FIELD_ID)).thenReturn(true);
            assertTrue(SocketableDrops.hasRule("rat_abyss_research"));
            assertEquals(1, SocketableDrops.roll("rat_abyss_research", new Random(1L)).size());
            assertEquals(0.5, share("probe", 1f, items -> !items.isEmpty()), 0.02);

            luna.when(() -> LunaSettings.getBoolean("exiledSector", SalvageSiteCompat.DROPS_FIELD_ID)).thenReturn(false);
            assertFalse(SocketableDrops.hasRule("rat_abyss_research"));
            assertTrue(SocketableDrops.roll("rat_abyss_research", new Random(1L)).isEmpty());
            assertTrue(SocketableDrops.hasRule("probe"));
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

        String base = Files.readString(RealSkillData.projectRoot().resolve(SocketableDrops.DATA_PATH), StandardCharsets.UTF_8);
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
            if (SocketableDrops.hasRule(rows.getJSONObject(i).getString("site"))) {
                count++;
            }
        }
        return count;
    }
}
