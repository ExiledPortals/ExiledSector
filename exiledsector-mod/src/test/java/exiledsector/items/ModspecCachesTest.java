package exiledsector.items;

import exiledsector.items.ItemDataIntegrity.Severity;
import exiledsector.items.ItemDataIntegrity.Violation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModspecCachesTest {

    private static final String CACHE = "cache";
    private static final String TRIPLE_CACHE = "triple_cache";

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        for (SkillNode node : List.of(node("core_notable_a", SkillTier.NOTABLE, "core", List.of()), node("core_notable_b", SkillTier.NOTABLE, "core", List.of()),
                node("core_choice", SkillTier.NOTABLE, "core", List.of("core_notable_a_type")), node("core_small", SkillTier.SMALL, "core", List.of()),
                node("hegemony_notable", SkillTier.NOTABLE, "hegemony", List.of()), node("hegemony_keystone", SkillTier.KEYSTONE, "hegemony", List.of()),
                node("abyss_notable", SkillTier.NOTABLE, "abyss", List.of()))) {
            SkillTree.register(node);
        }
        JSONArray rows = new JSONArray()
                .put(cacheRow(CACHE, "0.9;0.09;0.01").put(ModspecCaches.EXCLUDED_NODES_COLUMN, "core_notable_b")
                        .put(ModspecCaches.SALVAGE_CHANCE_COLUMN, "1").put(ModspecCaches.DROP_CHANCE_COLUMN, "0.5"))
                .put(cacheRow(TRIPLE_CACHE, "0;0;1"))
                .put(cacheRow("broken_cache", "0;0"))
                .put(ItemFixtures.subroutineRow("doctrine_chip", SubroutineKind.DOCTRINE));
        ItemDefinitions.register(rows);
        ModspecCaches.register(rows);
        ItemDrops.register(new JSONArray().put(new JSONObject().put("site", "derelict").put("chances", "1").put("uniqueChance", "0")));
    }

    @AfterEach
    void tearDown() {
        ModspecCaches.clear();
        ItemDrops.clear();
        ItemDefinitions.clear();
        SkillTree.clearTypes();
        SkillTree.clearNodes();
        RealSkillData.clear();
    }

    private static JSONObject cacheRow(String id, String nodeCountChances) throws Exception {
        return ItemFixtures.subroutineRow(id, SubroutineKind.MODSPEC_CACHE).put(ModspecCaches.NODE_COUNT_CHANCES_COLUMN, nodeCountChances);
    }

    private static SkillNode node(String id, SkillTier tier, String region, List<String> optionIds) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).effects(List.of()).optionalOptionIds(optionIds).build();
        return new SkillNode(id, type, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of(region));
    }

    @Test
    void aCacheRollsFromTheNotablesOfEveryVolumeLeavingOutExcludedAndOptionalOnes() {
        assertEquals(List.of("abyss_notable", "core_notable_a", "hegemony_notable"), SubroutineNodePools.candidates(ItemDefinitions.get(CACHE)));

        Subroutine rolled = assertInstanceOf(Subroutine.class, new ItemStore().add(ItemDefinitions.get(CACHE), 42L));
        assertFalse(rolled.rolledNodeIds().isEmpty());
        assertEquals(CorruptionOutcome.INNATE, rolled.corruption());
    }

    @Test
    void aCacheRollsDistinctNodesUpToItsNodeCount() {
        Subroutine triple = assertInstanceOf(Subroutine.class, new ItemStore().add(ItemDefinitions.get(TRIPLE_CACHE), 7L));
        assertEquals(3, triple.rolledNodeIds().size());
        assertEquals(3, new HashSet<>(triple.rolledNodeIds()).size());
        assertTrue(List.of("abyss_notable", "core_notable_a", "core_notable_b", "hegemony_notable").containsAll(triple.rolledNodeIds()));
    }

    @Test
    void theNodeCountFollowsTheChances() {
        ModspecCaches.CacheBase base = ModspecCaches.base(CACHE);
        int[] counts = new int[4];
        Random random = new Random(3L);
        int trials = 100_000;
        for (int trial = 0; trial < trials; trial++) {
            counts[base.nodeCount(random.nextLong())]++;
        }
        assertEquals(0, counts[0]);
        assertEquals(0.9, counts[1] / (double) trials, 0.01);
        assertEquals(0.09, counts[2] / (double) trials, 0.005);
        assertEquals(0.01, counts[3] / (double) trials, 0.002);
        assertEquals(3, ModspecCaches.base(TRIPLE_CACHE).nodeCount(1L));
    }

    @Test
    void aRowWithoutAnyNodeCountChanceIsNotACache() {
        assertNull(ModspecCaches.base("broken_cache"));
        assertNull(ModspecCaches.base("doctrine_chip"));
        assertTrue(ModspecCaches.isValidNodeCountChances("0.9;0.09;0.01"));
        assertFalse(ModspecCaches.isValidNodeCountChances("0;0"));
        assertFalse(ModspecCaches.isValidNodeCountChances("1;;1"));
        assertFalse(ModspecCaches.isValidNodeCountChances("-1;2"));
        assertFalse(ModspecCaches.isValidNodeCountChances(""));
    }

    @Test
    void aCacheDropsFromFleetsOfAnyFactionAndFromSalvage() {
        FactionFleetDrops.Source fleetSource = FactionFleetDrops.source(CACHE);
        assertNotNull(fleetSource);
        assertTrue(fleetSource.anyFaction());
        assertEquals(0.5f, fleetSource.chance(), 1e-6f);
        assertEquals(0.05f, FactionFleetDrops.chance(fleetSource, Map.of("independent", 10f)), 1e-6f);
        assertNull(FactionFleetDrops.source(TRIPLE_CACHE));

        Set<String> salvaged = new HashSet<>();
        Random random = new Random(11L);
        for (int trial = 0; trial < 50; trial++) {
            ItemDrops.roll("derelict", random, ItemChances.of(1f), definition -> true).forEach(item -> salvaged.add(item.definition().id()));
        }
        assertEquals(Set.of(CACHE), salvaged);
    }

    @Test
    void nodesWhoseAreaIsOffNeverRoll() {
        SkillTree.unregister("abyss_notable");

        assertEquals(List.of("core_notable_a", "hegemony_notable"), SubroutineNodePools.candidates(ItemDefinitions.get(CACHE)));
        assertTrue(FactionFleetDrops.available(ItemDefinitions.get(CACHE)));
    }

    @Test
    void theIntegrityCheckReportsBadCacheRows() throws Exception {
        Map<String, SkillNode> treeNodes = new LinkedHashMap<>(SkillTree.getAllNodes());
        JSONArray checkedRows = new JSONArray()
                .put(cacheRow("fine", "1").put(ModspecCaches.SALVAGE_CHANCE_COLUMN, "0.1"))
                .put(cacheRow("bad_chances", "1;x").put(ModspecCaches.DROP_CHANCE_COLUMN, "lots"))
                .put(cacheRow("excluding", "1").put(ModspecCaches.EXCLUDED_NODES_COLUMN,
                                "core_notable_a; core_notable_b; hegemony_notable; abyss_notable; core_small; missing")
                        .put(ModspecCaches.DROP_CHANCE_COLUMN, "0.1"))
                .put(ItemFixtures.subroutineRow("doctrine_chip", SubroutineKind.DOCTRINE).put(ModspecCaches.SALVAGE_CHANCE_COLUMN, "0.1"));

        List<String> messages = SubroutineDataRules.checkModspecCaches(checkedRows, treeNodes).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();

        assertEquals(List.of(
                "bad_chances has the node count chances \"1;x\"; expected numbers of 0 or more separated by ;, at least one above 0",
                "bad_chances has the drop chance \"lots\"; expected a number of 0 or more",
                "excluding excludes core_small, which is not a notable",
                "excluding excludes missing, which is not in the skill tree",
                "excluding has no notable left to roll",
                "doctrine_chip fills salvage_chance, but only MODSPEC_CACHE subroutines use it"), messages);
    }

    @Test
    void theShippedCacheIsValidAndHasNotablesToRoll() throws Exception {
        ModspecCaches.clear();
        ItemDefinitions.clear();
        RealSkillData.load();
        ItemFixtures.registerShippedItems();
        JSONArray shippedRows = HullItemRows.shippedRows(SubroutineDefinitions.dataPath());
        ModspecCaches.register(shippedRows);

        assertEquals(List.of(), SubroutineDataRules.checkModspecCaches(shippedRows, SkillTree.getDeclaredNodes()));
        assertEquals(1, ModspecCaches.bases().size());
        ModspecCaches.CacheBase base = ModspecCaches.bases().iterator().next();
        ItemDefinition definition = ItemDefinitions.get(base.definitionId());
        assertTrue(definition.unique());
        assertFalse(SubroutineNodePools.candidates(definition).isEmpty());
        assertEquals(List.of(0.9f, 0.09f, 0.01f), base.nodeCountChances());
        assertTrue(base.dropChance() > 0f);
        assertTrue(base.salvageChance() > 0f);
    }
}
