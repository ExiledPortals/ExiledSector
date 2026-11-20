package exiledsector.items.subroutine.doctrine;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.MockSector;
import exiledsector.ProjectFiles;
import exiledsector.items.HullItemRows;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDataChecks;
import exiledsector.items.ItemDataIntegrity.Severity;
import exiledsector.items.ItemDataIntegrity.Violation;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemFixtures;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemTraits;
import exiledsector.items.ItemType;
import exiledsector.items.drop.FactionFleetDrops;
import exiledsector.items.drop.ItemQuantity;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.subroutine.SubroutineDataRules;
import exiledsector.items.subroutine.SubroutineDefinitions;
import exiledsector.items.subroutine.SubroutineKind;
import exiledsector.skills.ClearedSkillTreeExtension;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.radius.NodeRadii;
import exiledsector.skills.radius.RadiusSize;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClearedSkillTreeExtension.class)
class DoctrineArchiveDataTest {

    private static final Set<String> REWRITE_DOCTRINES = Set.of("doctrine_archive_tritachyon", "doctrine_archive_luddic", "doctrine_archive_remnant");
    private static final Set<String> AUGMENT_DOCTRINES = Set.of("doctrine_archive_hegemony", "doctrine_archive_sindrian_diktat",
            "doctrine_archive_pirate", "doctrine_archive_persean_league");
    private static final Set<String> INFESTED_DOCTRINES = Set.of("doctrine_archive_threat", "doctrine_archive_shrouded");

    @AfterEach
    void tearDown() {
        DoctrineArchives.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static List<Violation> checkShipped() throws Exception {
        RealSkillData.load();
        return SubroutineDataRules.checkDoctrineArchives(HullItemRows.shippedRows(ItemModifiers.dataPath()),
                HullItemRows.shippedRows(SubroutineDefinitions.dataPath()), HullItemRows.shippedRows(DoctrineArchives.DATA_PATH), SkillTree.getAllTypes());
    }

    @Test
    void theShippedDoctrineDataHasNoIntegrityProblems() throws Exception {
        assertEquals(List.of(), checkShipped());
    }

    @Test
    void everyVanillaFactionDoctrineLoadsWithTheAgreedBehaviour() throws Exception {
        RealSkillData.load();
        ItemFixtures.registerShippedItems();
        DoctrineArchives.register(HullItemRows.shippedRows(DoctrineArchives.DATA_PATH));

        Set<String> doctrineDefinitions = ItemDefinitions.ofType(ItemType.SUBROUTINE).stream()
                .filter(definition -> SubroutineKind.of(definition) == SubroutineKind.DOCTRINE).map(ItemDefinition::id).collect(Collectors.toSet());
        Set<String> expected = new HashSet<>(REWRITE_DOCTRINES);
        expected.addAll(AUGMENT_DOCTRINES);
        expected.addAll(INFESTED_DOCTRINES);
        assertEquals(expected, doctrineDefinitions);
        Set<String> rewriting = new HashSet<>(REWRITE_DOCTRINES);
        rewriting.addAll(INFESTED_DOCTRINES);
        for (String definitionId : rewriting) {
            DoctrineArchive archive = DoctrineArchives.get(definitionId);
            assertNotNull(archive, definitionId);
            assertTrue(archive.rewritesNodes(), definitionId);
            for (String typeId : archive.keystoneTypeIds()) {
                assertEquals(SkillTier.KEYSTONE, SkillTree.getType(typeId).getTier(), typeId);
            }
            for (String typeId : archive.notableTypeIds()) {
                assertEquals(SkillTier.NOTABLE, SkillTree.getType(typeId).getTier(), typeId);
            }
        }
        for (String definitionId : AUGMENT_DOCTRINES) {
            DoctrineArchive archive = DoctrineArchives.get(definitionId);
            assertNotNull(archive, definitionId);
            assertTrue(archive.augments(), definitionId);
            assertTrue(archive.modifierPool().size() >= 3, definitionId);
        }
        assertTrue(DoctrineArchives.get("doctrine_archive_luddic").smallTypeIds().stream()
                .allMatch(typeId -> SkillTree.getType(typeId).getEffects().isEmpty()));
    }

    @Test
    void everyShippedDoctrineRewritesNodesAroundEveryStandardSocketOfTheRealTree() throws Exception {
        RealSkillData.load();
        ItemFixtures.registerShippedItems();
        DoctrineArchives.register(HullItemRows.shippedRows(DoctrineArchives.DATA_PATH));
        NodeRadii.register(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(
                NodeRadii.DATA_PATH)));
        try (MockSector mockSector = new MockSector()) {
            List<SkillNode> sockets = SkillTree.getAllNodes().values().stream()
                    .filter(node -> node.getType().getTier() == SkillTier.SOCKET).toList();
            assertTrue(sockets.size() >= 6);
            for (DoctrineArchive archive : DoctrineArchives.all()) {
                int rewrittenNodes = 0;
                for (SkillNode socket : sockets) {
                    ShipSkillData data = new ShipSkillData();
                    ShipTrees.allocate(data, socket, 0);
                    Item doctrine = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(archive.definitionId()), 4471L));
                    data.socketItem(socket.getId(), doctrine.id());
                    DoctrineLayer layer = DoctrineOverrides.layer(data);
                    for (DoctrineRewrite rewrite : layer.rewrites().values()) {
                        assertTrue(SkillTree.radii().nodeIdsWithin(socket.getId(), archive.radiusSize()).contains(rewrite.nodeId()));
                        if (archive.rewritesNodes()) {
                            SkillTier tier = SkillTree.get(rewrite.nodeId()).getType().getTier();
                            assertTrue(archive.replacementPool(tier).contains(rewrite.replacementType().getId()), rewrite.toString());
                            assertNull(rewrite.replacementType().getItemCost(), rewrite.toString());
                            assertTrue(rewrite.replacementType().getUnlockConditions().isEmpty(), rewrite.toString());
                        } else {
                            assertTrue(rewrite.augmented() && !rewrite.addedEffects(HullSize.CRUISER).isEmpty());
                        }
                    }
                    rewrittenNodes += layer.rewrites().size();
                    DoctrineOverrides.forget(data);
                }
                assertTrue(rewrittenNodes > 0, archive.definitionId());
            }
        } finally {
            NodeRadii.reset();
            DoctrineOverrides.clearCache();
        }
    }

    @Test
    void curatedDoctrineNodesHaveFlavourDescriptionsWithoutNumbers() throws Exception {
        RealSkillData.load();
        for (SkillType type : SkillTree.getAllTypes().values()) {
            if (type.getId().startsWith("doctrine_")) {
                String description = type.getFlavourSource();
                assertNotNull(description, type.getId());
                assertTrue(description.chars().noneMatch(Character::isDigit), type.getId() + ": " + description);
            }
        }
    }

    private static JSONArray modifiers() throws Exception {
        return new JSONArray()
                .put(HullItemRows.modifierRow("ARMOR_PERCENT_TEST_DOCTRINE", "prefix", "doctrine", "ARMOR_PERCENT:2:4", ""))
                .put(HullItemRows.modifierRow("HULL_PERCENT_PLATE", "prefix", "hull_item", "HULL_PERCENT:2:4", "Plated"))
                .put(HullItemRows.modifierRow("FLUX_CAPACITY_PERCENT_TEST_DOCTRINE", "suffix", "doctrine", "FLUX_CAPACITY_PERCENT:2:4", ""));
    }

    private static JSONArray subroutines() throws Exception {
        return new JSONArray()
                .put(ItemFixtures.subroutineRow("doc_a", SubroutineKind.DOCTRINE))
                .put(ItemFixtures.subroutineRow("doc_b", SubroutineKind.DOCTRINE))
                .put(ItemFixtures.subroutineRow("volume_chip", SubroutineKind.MODSPEC_CACHE));
    }

    private static Map<String, SkillType> types() {
        return Map.of(
                "notable_a", new SkillType.Builder("notable_a", "A", "a.png", SkillTier.NOTABLE).build(),
                "keystone_a", new SkillType.Builder("keystone_a", "K", "a.png", SkillTier.KEYSTONE).build(),
                "small_a", new SkillType.Builder("small_a", "S", "a.png", SkillTier.SMALL).build());
    }

    private static JSONObject row(String id, String behaviour, String radius) throws Exception {
        return new JSONObject().put("id", id).put("behaviour", behaviour).put("radius", radius);
    }

    private static List<String> errors(JSONArray doctrineRows) throws Exception {
        return SubroutineDataRules.checkDoctrineArchives(modifiers(), subroutines(), doctrineRows, types()).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();
    }

    @Test
    void validDoctrineRowsPass() throws Exception {
        assertEquals(List.of(), errors(new JSONArray()
                .put(row("doc_a", "REWRITE", "large").put("notables", "notable_a").put("keystones", "keystone_a").put("small_nodes", "small_a"))
                .put(row("doc_b", "AUGMENT", "medium").put("modifiers", "@ARMOR_PERCENT_TEST_DOCTRINE:2").put("min_lines", "1").put("max_lines", "2"))));
    }

    @Test
    void badDoctrineRowsAreReported() throws Exception {
        List<String> errors = errors(new JSONArray()
                .put(row("doc_a", "REWRITE", "huge").put("notables", "keystone_a; missing_type").put("modifiers", "@ARMOR_PERCENT_TEST_DOCTRINE"))
                .put(row("doc_b", "AUGMENT", "medium").put("notables", "notable_a")
                        .put("modifiers", "@HULL_PERCENT_PLATE; @FLUX_CAPACITY_PERCENT_TEST_DOCTRINE; ARMOR_PERCENT:1:2").put("min_lines", "3")
                        .put("max_lines", "2"))
                .put(row("volume_chip", "SHUFFLE", "small")));

        assertTrue(errors.contains("doc_a has radius \"huge\"; expected one of small, medium, large, extra_large"), errors.toString());
        assertTrue(errors.contains("doc_a lists keystone_a as a NOTABLE replacement, but it is a KEYSTONE node"), errors.toString());
        assertTrue(errors.contains("doc_a lists missing_type, which is not a skill type"), errors.toString());
        assertTrue(errors.contains("doc_a rewrites nodes, so it lists no modifiers"), errors.toString());
        assertTrue(errors.contains("doc_b augments nodes, so it lists no replacement nodes"), errors.toString());
        assertTrue(errors.contains("doc_b needs min_lines of 1 or more and max_lines of at least min_lines"), errors.toString());
        assertTrue(errors.contains("doc_b is a doctrine, but HULL_PERCENT_PLATE only rolls on hull_item"), errors.toString());
        assertTrue(errors.contains("doc_b lists FLUX_CAPACITY_PERCENT_TEST_DOCTRINE in its prefix slot, but it is a suffix"), errors.toString());
        assertTrue(errors.contains("doc_b lists \"ARMOR_PERCENT:1:2\" inline; reference a modifier as @ID or @ID:weight"), errors.toString());
        assertTrue(errors.contains("volume_chip is not a DOCTRINE subroutine in " + SubroutineDefinitions.dataPath()), errors.toString());
        assertTrue(errors.contains("volume_chip has behaviour \"SHUFFLE\"; expected one of REWRITE, AUGMENT"), errors.toString());
    }

    @Test
    void aDoctrineSubroutineWithoutARowAndASuffixDoctrineModifierAreReported() throws Exception {
        List<String> errors = errors(new JSONArray().put(row("doc_a", "REWRITE", "large").put("notables", "notable_a")));

        assertTrue(errors.contains("doc_b is a DOCTRINE subroutine with no row in " + DoctrineArchives.DATA_PATH), errors.toString());
        List<String> modifierErrors = ItemDataChecks.check(modifiers(), new JSONArray(), new JSONArray(), new JSONArray()).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();
        assertTrue(modifierErrors.contains("FLUX_CAPACITY_PERCENT_TEST_DOCTRINE rolls on doctrine, so its slot must be prefix"), modifierErrors.toString());
    }

    @Test
    void theLoaderSkipsRowsThatAreNotDoctrineSubroutines() throws Exception {
        ItemModifiers.register(modifiers());
        ItemDefinitions.register(subroutines());
        DoctrineArchives.register(new JSONArray()
                .put(row("doc_a", "REWRITE", "large").put("notables", "notable_a"))
                .put(row("doc_b", "AUGMENT", "small").put("modifiers", "@ARMOR_PERCENT_TEST_DOCTRINE"))
                .put(row("volume_chip", "REWRITE", "large").put("notables", "notable_a"))
                .put(row("unknown", "REWRITE", "large").put("notables", "notable_a")));

        assertEquals(RadiusSize.LARGE, DoctrineArchives.get("doc_a").radiusSize());
        assertEquals(1, DoctrineArchives.get("doc_b").minLines());
        assertEquals(1, DoctrineArchives.get("doc_b").modifierPool().size());
        assertNull(DoctrineArchives.get("volume_chip"));
        assertNull(DoctrineArchives.get("unknown"));
    }
    private static void loadShippedDoctrines() throws Exception {
        RealSkillData.load();
        ItemFixtures.registerShippedItems();
        DoctrineArchives.register(HullItemRows.shippedRows(DoctrineArchives.DATA_PATH));
        NpcFactionVolumes.register(HullItemRows.shippedRows("data/config/exiledSector/npc_faction_volumes.csv"));
    }

    @Test
    void bossesDropADoctrineArchiveAtEveryTierButLessOftenBelowTierFourteen() throws Exception {
        loadShippedDoctrines();
        try {
            Random random = new Random(2L);
            int tierFourteenDrops = 0;
            int tierOneDrops = 0;
            for (int trial = 0; trial < 20000; trial++) {
                tierFourteenDrops += DoctrineArchives.rollForBoss(14, "hegemony", ItemQuantity.BASE, random, definition -> true) == null ? 0 : 1;
                tierOneDrops += DoctrineArchives.rollForBoss(1, "hegemony", ItemQuantity.BASE, random, definition -> true) == null ? 0 : 1;
                assertNotNull(DoctrineArchives.rollForBoss(15, "hegemony", ItemQuantity.increasedBy(9900f), random, definition -> true));
            }

            assertEquals(0.07f, tierFourteenDrops / 20000f, 0.01f);
            assertEquals(0.01f, tierOneDrops / 20000f, 0.003f);
            assertEquals(DoctrineArchives.bossChance(14), DoctrineArchives.bossChance(15));
            assertTrue(DoctrineArchives.bossChance(ItemTraits.HIGHEST_TIER) > DoctrineArchives.bossChance(ItemTraits.HIGHEST_DROP_TIER));
            assertEquals(DoctrineArchives.bossChance(ItemTraits.HIGHEST_TIER), DoctrineArchives.bossChance(ItemTraits.HIGHEST_TIER + 1));
            for (int tier = 1; tier < 14; tier++) {
                assertTrue(DoctrineArchives.bossChance(tier) > 0f);
                assertTrue(DoctrineArchives.bossChance(tier) < DoctrineArchives.bossChance(tier + 1));
            }
            assertNull(DoctrineArchives.rollForBoss(15, "hegemony", ItemQuantity.increasedBy(9900f), random, definition -> false));
        } finally {
            NpcFactionVolumes.clear();
        }
    }

    @Test
    void aBossDropsItsOwnFactionsDoctrineAndAFactionWithoutOneDropsAny() throws Exception {
        loadShippedDoctrines();
        try {
            Random random = new Random(4L);
            Set<String> independentDrops = new HashSet<>();
            for (int trial = 0; trial < 300; trial++) {
                assertEquals("doctrine_archive_sindrian_diktat", DoctrineArchives.pickFor("sindrian_diktat", random, definition -> true).id());
                assertEquals("doctrine_archive_luddic", DoctrineArchives.pickFor("luddic_path", random, definition -> true).id());
                assertEquals("doctrine_archive_pirate", DoctrineArchives.pickFor("pirates", random, definition -> true).id());
                independentDrops.add(DoctrineArchives.pickFor("independent", random, definition -> true).id());
            }

            assertEquals(REWRITE_DOCTRINES.size() + AUGMENT_DOCTRINES.size(), independentDrops.size());
        } finally {
            NpcFactionVolumes.clear();
        }
    }

    @Test
    void theInfestedArchivesDropOnlyFromTheirOwnFleetsAndAreNotBornCorrupted() throws Exception {
        loadShippedDoctrines();
        try {
            assertEquals(DoctrineInfestation.THREAT, DoctrineArchives.get("doctrine_archive_threat").infestation());
            assertEquals(DoctrineInfestation.SHROUD, DoctrineArchives.get("doctrine_archive_shrouded").infestation());
            FactionFleetDrops.Source shrouded = FactionFleetDrops.source("doctrine_archive_shrouded");
            assertTrue(shrouded.guaranteed());
            assertEquals(Set.of("dweller"), shrouded.extraFactionIds());
            FactionFleetDrops.Source threat = FactionFleetDrops.source("doctrine_archive_threat");
            assertFalse(threat.guaranteed());
            assertEquals(Set.of("threat"), threat.extraFactionIds());
            assertEquals(0.15f, threat.chance(), 1e-6f);
            Random random = new Random(6L);
            for (int trial = 0; trial < 100; trial++) {
                assertEquals("doctrine_archive_threat", DoctrineArchives.pickFor("threat", random, definition -> true).id());
                assertFalse(INFESTED_DOCTRINES.contains(DoctrineArchives.pickFor("independent", random, definition -> true).id()));
            }
            for (String definitionId : DoctrineArchives.all().stream().map(DoctrineArchive::definitionId).toList()) {
                boolean infested = INFESTED_DOCTRINES.contains(definitionId);
                assertEquals(infested, DoctrineArchives.infested(definitionId), definitionId);
                assertEquals(infested, FactionFleetDrops.source(definitionId) != null, definitionId);
                assertFalse(Item.create("archive_" + definitionId, definitionId, 9L, List.of()).corrupted(), definitionId);
            }
        } finally {
            NpcFactionVolumes.clear();
        }
    }
}
