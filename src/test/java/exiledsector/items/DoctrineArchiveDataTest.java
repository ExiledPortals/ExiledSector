package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.items.ItemDataIntegrity.Severity;
import exiledsector.items.ItemDataIntegrity.Violation;
import exiledsector.skills.ShipSkillData;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctrineArchiveDataTest {

    private static final Set<String> TRANSFORM_DOCTRINES = Set.of("doctrine_archive_tritachyon", "doctrine_archive_luddic", "doctrine_archive_remnant");
    private static final Set<String> AUGMENT_DOCTRINES = Set.of("doctrine_archive_hegemony", "doctrine_archive_sindrian_diktat",
            "doctrine_archive_pirate", "doctrine_archive_persean_league");

    @AfterEach
    void tearDown() {
        DoctrineArchives.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
        RealSkillData.clear();
    }

    private static List<Violation> checkShipped() throws Exception {
        RealSkillData.load();
        return ItemDataIntegrity.checkDoctrineArchives(ItemFixtures.shippedRows(ItemModifiers.DATA_PATH),
                ItemFixtures.shippedRows(ItemDefinitions.SUBROUTINES_PATH), ItemFixtures.shippedRows(DoctrineArchives.DATA_PATH), SkillTree.getAllTypes());
    }

    @Test
    void theShippedDoctrineDataHasNoIntegrityProblems() throws Exception {
        assertEquals(List.of(), checkShipped());
    }

    @Test
    void everyVanillaFactionDoctrineLoadsWithTheAgreedBehaviour() throws Exception {
        RealSkillData.load();
        ItemFixtures.registerShippedItems();
        DoctrineArchives.register(ItemFixtures.shippedRows(DoctrineArchives.DATA_PATH));

        Set<String> doctrineDefinitions = ItemDefinitions.ofType(ItemType.SUBROUTINE).stream()
                .filter(definition -> definition.subroutineKind() == SubroutineKind.DOCTRINE).map(ItemDefinition::id).collect(Collectors.toSet());
        Set<String> expected = new HashSet<>(TRANSFORM_DOCTRINES);
        expected.addAll(AUGMENT_DOCTRINES);
        assertEquals(expected, doctrineDefinitions);
        for (String definitionId : TRANSFORM_DOCTRINES) {
            DoctrineArchive archive = DoctrineArchives.get(definitionId);
            assertNotNull(archive, definitionId);
            assertTrue(archive.transforms(), definitionId);
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
        DoctrineArchives.register(ItemFixtures.shippedRows(DoctrineArchives.DATA_PATH));
        NodeRadii.register(RealSkillData.readJson(RealSkillData.projectRoot().resolve(
                NodeRadii.DATA_PATH)));
        SectorAPI sector = Mockito.mock(SectorAPI.class);
        Mockito.when(sector.getPersistentData()).thenReturn(new HashMap<>());
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSector).thenReturn(sector);
            List<SkillNode> sockets = SkillTree.getAllNodes().values().stream()
                    .filter(node -> node.getType().getTier() == SkillTier.SOCKET).toList();
            assertTrue(sockets.size() >= 6);
            for (DoctrineArchive archive : DoctrineArchives.all()) {
                int rewrittenNodes = 0;
                for (SkillNode socket : sockets) {
                    ShipSkillData data = new ShipSkillData();
                    data.allocate(socket, 0);
                    Item doctrine = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(archive.definitionId()), 4471L));
                    data.socketItem(socket.getId(), doctrine.id());
                    DoctrineLayer layer = DoctrineOverrides.layer(data);
                    for (DoctrineRewrite rewrite : layer.rewrites().values()) {
                        assertTrue(SkillTree.radii().nodeIdsWithin(socket.getId(), archive.radiusSize()).contains(rewrite.nodeId()));
                        if (archive.transforms()) {
                            SkillTier tier = SkillTree.get(rewrite.nodeId()).getType().getTier();
                            assertTrue(archive.replacementPool(tier).contains(rewrite.replacementType().getId()), rewrite.toString());
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
                String description = type.getDescriptionOverride();
                assertNotNull(description, type.getId());
                assertTrue(description.chars().noneMatch(Character::isDigit), type.getId() + ": " + description);
            }
        }
    }

    private static JSONArray modifiers() throws Exception {
        return new JSONArray()
                .put(ItemFixtures.modifierRow("ARMOR_PERCENT_TEST_DOCTRINE", "prefix", "doctrine", "ARMOR_PERCENT:2:4", ""))
                .put(ItemFixtures.modifierRow("HULL_PERCENT_PLATE", "prefix", "hull_item", "HULL_PERCENT:2:4", "Plated"))
                .put(ItemFixtures.modifierRow("FLUX_CAPACITY_PERCENT_TEST_DOCTRINE", "suffix", "doctrine", "FLUX_CAPACITY_PERCENT:2:4", ""));
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
        return ItemDataIntegrity.checkDoctrineArchives(modifiers(), subroutines(), doctrineRows, types()).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();
    }

    @Test
    void validDoctrineRowsPass() throws Exception {
        assertEquals(List.of(), errors(new JSONArray()
                .put(row("doc_a", "TRANSFORM", "large").put("notables", "notable_a").put("keystones", "keystone_a").put("small_nodes", "small_a"))
                .put(row("doc_b", "AUGMENT", "medium").put("modifiers", "@ARMOR_PERCENT_TEST_DOCTRINE:2").put("min_lines", "1").put("max_lines", "2"))));
    }

    @Test
    void badDoctrineRowsAreReported() throws Exception {
        List<String> errors = errors(new JSONArray()
                .put(row("doc_a", "TRANSFORM", "huge").put("notables", "keystone_a; missing_type").put("modifiers", "@ARMOR_PERCENT_TEST_DOCTRINE"))
                .put(row("doc_b", "AUGMENT", "medium").put("notables", "notable_a")
                        .put("modifiers", "@HULL_PERCENT_PLATE; @FLUX_CAPACITY_PERCENT_TEST_DOCTRINE; ARMOR_PERCENT:1:2").put("min_lines", "3")
                        .put("max_lines", "2"))
                .put(row("volume_chip", "SHUFFLE", "small")));

        assertTrue(errors.contains("doc_a has radius \"huge\"; expected small, medium or large"), errors.toString());
        assertTrue(errors.contains("doc_a lists keystone_a as a NOTABLE replacement, but it is a KEYSTONE node"), errors.toString());
        assertTrue(errors.contains("doc_a lists missing_type, which is not a skill type"), errors.toString());
        assertTrue(errors.contains("doc_a transforms nodes, so it lists no modifiers"), errors.toString());
        assertTrue(errors.contains("doc_b augments nodes, so it lists no replacement nodes"), errors.toString());
        assertTrue(errors.contains("doc_b needs min_lines of 1 or more and max_lines of at least min_lines"), errors.toString());
        assertTrue(errors.contains("doc_b is a doctrine, but HULL_PERCENT_PLATE only rolls on hull_item"), errors.toString());
        assertTrue(errors.contains("doc_b lists FLUX_CAPACITY_PERCENT_TEST_DOCTRINE in its prefix slot, but it is a suffix"), errors.toString());
        assertTrue(errors.contains("doc_b lists \"ARMOR_PERCENT:1:2\" inline; reference a modifier as @ID or @ID:weight"), errors.toString());
        assertTrue(errors.contains("volume_chip is not a DOCTRINE subroutine in " + ItemDefinitions.SUBROUTINES_PATH), errors.toString());
        assertTrue(errors.contains("volume_chip has behaviour \"SHUFFLE\"; expected one of TRANSFORM, AUGMENT"), errors.toString());
    }

    @Test
    void aDoctrineSubroutineWithoutARowAndASuffixDoctrineModifierAreReported() throws Exception {
        List<String> errors = errors(new JSONArray().put(row("doc_a", "TRANSFORM", "large").put("notables", "notable_a")));

        assertTrue(errors.contains("doc_b is a DOCTRINE subroutine with no row in " + DoctrineArchives.DATA_PATH), errors.toString());
        List<String> modifierErrors = ItemDataIntegrity.check(modifiers(), new JSONArray(), new JSONArray(), new JSONArray()).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();
        assertTrue(modifierErrors.contains("FLUX_CAPACITY_PERCENT_TEST_DOCTRINE rolls on doctrine, so its slot must be prefix"), modifierErrors.toString());
    }

    @Test
    void theLoaderSkipsRowsThatAreNotDoctrineSubroutines() throws Exception {
        ItemModifiers.register(modifiers());
        ItemDefinitions.register(subroutines());
        DoctrineArchives.register(new JSONArray()
                .put(row("doc_a", "TRANSFORM", "large").put("notables", "notable_a"))
                .put(row("doc_b", "AUGMENT", "small").put("modifiers", "@ARMOR_PERCENT_TEST_DOCTRINE"))
                .put(row("volume_chip", "TRANSFORM", "large").put("notables", "notable_a"))
                .put(row("unknown", "TRANSFORM", "large").put("notables", "notable_a")));

        assertEquals(RadiusSize.LARGE, DoctrineArchives.get("doc_a").radiusSize());
        assertEquals(1, DoctrineArchives.get("doc_b").minLines());
        assertEquals(1, DoctrineArchives.get("doc_b").modifierPool().size());
        assertNull(DoctrineArchives.get("volume_chip"));
        assertNull(DoctrineArchives.get("unknown"));
    }
    private static void loadShippedDoctrines() throws Exception {
        RealSkillData.load();
        ItemFixtures.registerShippedItems();
        DoctrineArchives.register(ItemFixtures.shippedRows(DoctrineArchives.DATA_PATH));
        NpcFactionVolumes.register(ItemFixtures.shippedRows("data/config/exiledSector/npc_faction_volumes.csv"));
    }

    @Test
    void onlyWardensOfTierFourteenAndFifteenChartsDropADoctrineArchive() throws Exception {
        loadShippedDoctrines();
        try {
            Random random = new Random(2L);
            int drops = 0;
            for (int trial = 0; trial < 2000; trial++) {
                assertNull(DoctrineArchives.rollForWarden(13, "hegemony", 10f, random, definition -> true));
                drops += DoctrineArchives.rollForWarden(14, "hegemony", 1f, random, definition -> true) == null ? 0 : 1;
                assertNotNull(DoctrineArchives.rollForWarden(15, "hegemony", 2f, random, definition -> true));
            }

            assertEquals(DoctrineArchives.WARDEN_CHANCE, drops / 2000f, 0.04f);
            assertNull(DoctrineArchives.rollForWarden(15, "hegemony", 2f, random, definition -> false));
        } finally {
            NpcFactionVolumes.clear();
        }
    }

    @Test
    void aWardenDropsItsOwnFactionsDoctrineAndAFactionWithoutOneDropsAny() throws Exception {
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

            assertEquals(TRANSFORM_DOCTRINES.size() + AUGMENT_DOCTRINES.size(), independentDrops.size());
        } finally {
            NpcFactionVolumes.clear();
        }
    }
}
