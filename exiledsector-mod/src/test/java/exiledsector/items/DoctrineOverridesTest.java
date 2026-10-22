package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.items.Item;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemModifiers;
import exiledlib.items.ItemStore;
import exiledsector.effects.DoctrineRevertConflicts;
import exiledsector.effects.DoctrineRevertRelease;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.NodeEligibility;
import exiledlib.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledlib.skills.SkillTypeEffect;
import exiledsector.skills.radius.NodeRadii;
import exiledlib.skills.skilleffect.SkillEffect;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DoctrineOverridesTest {

    private static final String TRANSFORM = "doctrine_transform";
    private static final String AUGMENT = "doctrine_augment";
    private static final String NARROW = "doctrine_narrow";
    private static final String UNIQUE_RELIC = "relic";
    private static final List<String> NOTABLE_POOL = List.of("faction_a", "faction_b", "faction_c");

    private final Map<String, Object> persistentData = new HashMap<>();
    private MockedStatic<Global> globalMock;
    private SkillNode socket;
    private SkillNode nearNotable;
    private SkillNode otherNearNotable;
    private SkillNode nearSmall;
    private SkillNode nearKeystone;
    private SkillNode farNotable;

    @BeforeEach
    void setUp() throws Exception {
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SkillTree.clear();
        NodeRadii.register(new JSONObject("{ \"small\": 150, \"medium\": 250, \"large\": 350 }"));
        registerType(type("original", SkillTier.NOTABLE, "HULL_MULT", 5f).build());
        registerType(type("original_two", SkillTier.NOTABLE, "HULL_MULT", 6f).build());
        registerType(type("small_hull", SkillTier.SMALL, "HULL_MULT", 2f).build());
        registerType(type("old_keystone", SkillTier.KEYSTONE, "HULL_MULT", 10f).build());
        registerType(type("faction_a", SkillTier.NOTABLE, "ARMOR_PERCENT", 10f).build());
        registerType(type("faction_b", SkillTier.NOTABLE, "ARMOR_PERCENT", 11f).build());
        registerType(type("faction_c", SkillTier.NOTABLE, "ARMOR_PERCENT", 12f).build());
        registerType(type("faction_keystone", SkillTier.KEYSTONE, "ARMOR_PERCENT", 30f).build());
        registerType(new SkillType.Builder("plain", "Plain", "a.png", SkillTier.SMALL).build());
        registerType(new SkillType.Builder("socket_type", "Socket", "a.png", SkillTier.SOCKET).build());
        socket = node("socket", "socket_type", 0f, 0f);
        nearNotable = node("near_notable", "original", 100f, 0f);
        otherNearNotable = node("other_near_notable", "original_two", 0f, 100f);
        nearSmall = node("near_small", "small_hull", -100f, 0f);
        nearKeystone = node("near_keystone", "old_keystone", 0f, -100f);
        farNotable = node("far_notable", "original", 5000f, 0f);
        ItemModifiers.register(new JSONArray()
                .put(ItemFixtures.modifierRow("ARMOR_PERCENT_TEST_DOCTRINE", "prefix", "doctrine", "ARMOR_PERCENT:2:4", ""))
                .put(ItemFixtures.modifierRow("HULL_PERCENT_TEST_DOCTRINE", "prefix", "doctrine", "HULL_PERCENT:1:3", "")));
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.subroutineRow(TRANSFORM, SubroutineKind.DOCTRINE))
                .put(ItemFixtures.subroutineRow(AUGMENT, SubroutineKind.DOCTRINE))
                .put(ItemFixtures.subroutineRow(NARROW, SubroutineKind.DOCTRINE))
                .put(ItemFixtures.subroutineRow(UNIQUE_RELIC, SubroutineKind.UNIQUE).put("unique", "true").put("lines", "HULL_MULT:1:1")
                        .put("source", "faction:pirates")));
        DoctrineArchives.register(new JSONArray()
                .put(archiveRow(TRANSFORM, "TRANSFORM", "medium").put("notables", String.join("; ", NOTABLE_POOL))
                        .put("keystones", "faction_keystone").put("small_nodes", "plain"))
                .put(archiveRow(AUGMENT, "AUGMENT", "medium").put("modifiers", "@ARMOR_PERCENT_TEST_DOCTRINE; @HULL_PERCENT_TEST_DOCTRINE")
                        .put("min_lines", "1").put("max_lines", "2"))
                .put(archiveRow(NARROW, "TRANSFORM", "medium").put("notables", "original_two")));
    }

    @AfterEach
    void tearDown() {
        DoctrineOverrides.clearCache();
        DoctrineArchives.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
        SkillTree.clear();
        NodeRadii.reset();
        globalMock.close();
    }

    private static SkillType.Builder type(String id, SkillTier tier, String effectName, float magnitude) {
        return new SkillType.Builder(id, id, "a.png", tier).effects(List.of(new SkillTypeEffect(SkillEffect.byName(effectName), magnitude)));
    }

    private static void registerType(SkillType type) {
        SkillTree.registerType(type);
    }

    private static SkillNode node(String id, String typeId, float x, float y) {
        SkillNode node = new SkillNode(id, SkillTree.getType(typeId), List.of(), x, y);
        SkillTree.register(node);
        return node;
    }

    private static JSONObject archiveRow(String id, String behaviour, String radius) throws Exception {
        return new JSONObject().put("id", id).put("behaviour", behaviour).put("radius", radius);
    }

    private ShipSkillData shipWith(String definitionId, long seed, SkillNode... allocated) {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, socket, 0);
        for (SkillNode node : allocated) {
            ShipTrees.allocate(data, node, 0);
        }
        Item doctrine = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(definitionId), seed));
        data.socketItem(socket.getId(), doctrine.id());
        return data;
    }

    @Test
    void aTransformDoctrineRewritesNotablesKeystonesAndSmallNodesInRangeOnly() {
        ShipSkillData data = shipWith(TRANSFORM, 4471L, nearNotable);

        SkillType notable = nearNotable.resolveEffectiveType(data);
        assertTrue(NOTABLE_POOL.contains(notable.getId()), notable.getId());
        assertEquals("faction_keystone", nearKeystone.resolveEffectiveType(data).getId());
        assertEquals("plain", nearSmall.resolveEffectiveType(data).getId());
        assertEquals("original", farNotable.resolveEffectiveType(data).getId());
        assertEquals("socket_type", socket.resolveEffectiveType(data).getId());
        DoctrineRewrite rewrite = DoctrineOverrides.rewrite(data, nearNotable.getId());
        assertEquals("original", rewrite.originalType().getId());
        assertEquals(DoctrineRewriter.revision(4471L), rewrite.revision());
    }

    @Test
    void theRewriteIsAPureFunctionOfTheSeedAndTheNode() {
        ShipSkillData first = shipWith(TRANSFORM, 77L, nearNotable);
        ShipSkillData second = shipWith(TRANSFORM, 77L);

        assertEquals(nearNotable.resolveEffectiveType(first).getId(), nearNotable.resolveEffectiveType(second).getId());
        assertEquals(DoctrineRewriter.candidateOrder(77L, nearNotable.getId(), NOTABLE_POOL),
                DoctrineRewriter.candidateOrder(77L, nearNotable.getId(), List.of("faction_c", "faction_a", "faction_b")));
        Set<String> firstPicks = new java.util.HashSet<>();
        for (long seed = 0; seed < 40; seed++) {
            firstPicks.add(DoctrineRewriter.candidateOrder(seed, nearNotable.getId(), NOTABLE_POOL).get(0));
        }
        assertEquals(Set.copyOf(NOTABLE_POOL), firstPicks);
    }

    @Test
    void twoNotablesInRangeNeverTakeTheSameReplacement() {
        for (long seed = 0; seed < 40; seed++) {
            ShipSkillData data = shipWith(TRANSFORM, seed);
            assertNotEquals(nearNotable.resolveEffectiveType(data).getId(), otherNearNotable.resolveEffectiveType(data).getId());
        }
    }

    @Test
    void removingTheDoctrineRevertsTheNodesAndLeavesThemAllocated() {
        ShipSkillData data = shipWith(TRANSFORM, 9L, nearNotable);
        assertNotEquals("original", nearNotable.resolveEffectiveType(data).getId());

        data.unsocketItem(socket.getId());

        assertEquals("original", nearNotable.resolveEffectiveType(data).getId());
        assertTrue(data.isAllocated(nearNotable.getId()));
    }

    @Test
    void aDoctrineInAnUnallocatedSocketRewritesNothing() {
        ShipSkillData data = shipWith(TRANSFORM, 9L, nearNotable);
        ShipTrees.deallocate(data, socket);

        assertEquals("original", nearNotable.resolveEffectiveType(data).getId());
        assertTrue(DoctrineOverrides.layer(data).isEmpty());
    }

    @Test
    void anIneligibleReplacementSkipsToTheNextCandidateInSeedOrder() {
        long seed = 12L;
        String firstChoice = DoctrineRewriter.candidateOrder(seed, nearNotable.getId(), NOTABLE_POOL).get(0);
        SkillType capitalOnly = type(firstChoice, SkillTier.NOTABLE, "ARMOR_PERCENT", 10f).requiredHullSizes(EnumSet.of(HullSize.CAPITAL_SHIP)).build();
        SkillTree.registerType(capitalOnly);
        ShipSkillData data = shipWith(TRANSFORM, seed, nearNotable);
        DoctrineOverrides.bindShipFacts(data, () -> new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 100f, hullModId -> false));

        String replacement = nearNotable.resolveEffectiveType(data).getId();
        assertNotEquals(firstChoice, replacement);
        assertTrue(NOTABLE_POOL.contains(replacement));

        DoctrineOverrides.bindShipFacts(data, () -> new ShipFacts(HullSize.CAPITAL_SHIP, ShieldType.FRONT, false, 100f, hullModId -> false));
        assertEquals(firstChoice, nearNotable.resolveEffectiveType(data).getId());
    }

    @Test
    void aReplacementExclusiveWithAnAllocatedNodeElsewhereIsSkipped() {
        long seed = 31L;
        String firstChoice = DoctrineRewriter.candidateOrder(seed, nearNotable.getId(), NOTABLE_POOL).get(0);
        registerType(new SkillType.Builder("rival", "Rival", "a.png", SkillTier.NOTABLE).exclusiveSkillTypeIds(List.of(firstChoice)).build());
        SkillNode rival = node("rival_node", "rival", 9000f, 0f);
        ShipSkillData data = shipWith(TRANSFORM, seed, nearNotable, rival);

        assertNotEquals(firstChoice, nearNotable.resolveEffectiveType(data).getId());
    }

    @Test
    void aReplacementTheShipAlreadyHasElsewhereIsSkipped() {
        long seed = 31L;
        String firstChoice = DoctrineRewriter.candidateOrder(seed, nearNotable.getId(), NOTABLE_POOL).get(0);
        SkillNode alreadyOwned = node("owned_faction_node", firstChoice, 9000f, 0f);
        ShipSkillData data = shipWith(TRANSFORM, seed, nearNotable, alreadyOwned);

        assertNotEquals(firstChoice, nearNotable.resolveEffectiveType(data).getId());
    }

    @Test
    void aNodeThatKeepsItsOwnTypeReservesItSoNoOtherNodeDuplicatesIt() {
        ShipSkillData data = shipWith(NARROW, 8L, nearNotable, otherNearNotable);

        assertEquals("original", nearNotable.resolveEffectiveType(data).getId());
        assertEquals("original_two", otherNearNotable.resolveEffectiveType(data).getId());
        assertTrue(DoctrineOverrides.layer(data).rewrites().isEmpty());
    }

    @Test
    void aTransformedNodeNoLongerCarriesItsOriginalsExclusions() {
        SkillType exclusiveOriginal = new SkillType.Builder("exclusive_original", "X", "a.png", SkillTier.NOTABLE)
                .exclusiveSkillTypeIds(List.of("rival_type")).build();
        SkillType rival = new SkillType.Builder("rival_type", "R", "a.png", SkillTier.NOTABLE).build();
        SkillNode exclusiveNode = new SkillNode("exclusive_node", exclusiveOriginal, List.of(), 0f, 0f);

        assertTrue(new AllocatedNode(exclusiveNode, exclusiveOriginal).isExclusiveWith(rival));
        assertFalse(new AllocatedNode(exclusiveNode, SkillTree.getType("faction_a")).isExclusiveWith(rival));
    }

    @Test
    void removingATransformingDoctrineNamesTheInstalledHullModsItsOriginalsWouldStrip() {
        registerType(new SkillType.Builder("hull_mod_notable", "Hull Mod Notable", "a.png", SkillTier.NOTABLE).exclusiveHullModIds(List.of("rival_mod"))
                .build());
        SkillNode hullModNode = node("hull_mod_node", "hull_mod_notable", 100f, 100f);
        ShipSkillData data = shipWith(TRANSFORM, 21L, hullModNode);
        assertNotEquals("hull_mod_notable", hullModNode.resolveEffectiveType(data).getId());

        assertEquals(Set.of("rival_mod"), DoctrineRevertConflicts.hullModsLostOnRemoval(data, socket.getId(), hullModId -> true));
        assertEquals(Set.of(), DoctrineRevertConflicts.hullModsLostOnRemoval(data, socket.getId(), hullModId -> false));
        assertEquals(Set.of(), DoctrineRevertConflicts.hullModsLostOnRemoval(data, "elsewhere", hullModId -> true));
        ShipTrees.deallocate(data, hullModNode);
        assertEquals(Set.of(), DoctrineRevertConflicts.hullModsLostOnRemoval(data, socket.getId(), hullModId -> true));
    }

    private AllocationGate frigateGate(ShipSkillData data) {
        ShipFacts frigate = new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 100f, hullModId -> false);
        DoctrineOverrides.bindShipFacts(data, () -> frigate);
        return new AllocationGate(data, SkillTree.topology(), socket.getId(), new AllocationGate.Budget(100, 1, 100),
                NodeEligibility.Context.of(data, frigate, null));
    }

    @Test
    void aTransformedNodeIsCheckedAsItsReplacementNotItsOriginal() {
        registerType(type("capital_only", SkillTier.NOTABLE, "HULL_MULT", 5f).requiredHullSizes(EnumSet.of(HullSize.CAPITAL_SHIP)).build());
        SkillNode capitalOnly = node("capital_only_node", "capital_only", 100f, 100f);
        ShipSkillData data = shipWith(TRANSFORM, 14L);
        AllocationGate gate = frigateGate(data);

        assertNotEquals("capital_only", capitalOnly.resolveEffectiveType(data).getId());
        assertTrue(gate.allocation(capitalOnly).allowed());

        data.unsocketItem(socket.getId());
        assertEquals(NodeEligibility.Kind.WRONG_HULL_SIZE, frigateGate(data).allocation(capitalOnly).block().kind());
    }

    @Test
    void aTransformedNodeWhoseOriginalOffersOptionsAllocatesWithoutAskingAndRevertsToTheFirstOption() {
        registerType(type("opt_a", SkillTier.NOTABLE, "HULL_MULT", 1f).build());
        registerType(type("opt_b", SkillTier.NOTABLE, "HULL_MULT", 2f).build());
        registerType(new SkillType.Builder("choice", "Choice", "a.png", SkillTier.NOTABLE).optionalOptionIds(List.of("opt_a", "opt_b")).build());
        SkillNode choice = node("choice_node", "choice", -100f, 100f);
        ShipSkillData data = shipWith(TRANSFORM, 14L);
        AllocationGate gate = frigateGate(data);

        assertFalse(choice.offersOptions(data));
        assertTrue(gate.options(choice).isEmpty());
        assertTrue(gate.allocation(choice, null).allowed());
        assertFalse(gate.allocation(choice, SkillTree.getType("opt_a")).allowed());

        ShipTrees.allocate(data, choice, null, 0);
        assertNotEquals("opt_a", choice.resolveEffectiveType(data).getId());
        assertEquals(Set.of(choice.getId()), data.getReplacementAllocatedNodeIds());

        data.unsocketItem(socket.getId());
        assertEquals("opt_a", choice.resolveEffectiveType(data).getId());
        assertTrue(ShipTrees.forgetUnknownNodes(data, SkillTree.getAllNodes(), SkillTree.getAllTypes()).isEmpty());
        assertTrue(data.isAllocated(choice.getId()));
    }

    @Test
    void aRevertedNodeWhoseOriginalIsNotAllowedIsReleased() {
        registerType(type("capital_only", SkillTier.NOTABLE, "HULL_MULT", 5f).requiredHullSizes(EnumSet.of(HullSize.CAPITAL_SHIP)).build());
        SkillNode capitalOnly = node("capital_only_node", "capital_only", 100f, 100f);
        ShipSkillData data = shipWith(TRANSFORM, 14L);
        frigateGate(data);
        ShipTrees.allocate(data, capitalOnly, null, 0);
        ShipFacts frigate = new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 100f, hullModId -> false);

        assertTrue(DoctrineRevertRelease.release(data, frigate, SkillTree.topology(), socket.getId()).nodeIds().isEmpty());
        assertTrue(data.isAllocated(capitalOnly.getId()));

        data.unsocketItem(socket.getId());
        assertEquals(List.of(capitalOnly.getId()), DoctrineRevertRelease.release(data, frigate, SkillTree.topology(), socket.getId()).nodeIds());
        assertFalse(data.isAllocated(capitalOnly.getId()));
        assertTrue(data.getReplacementAllocatedNodeIds().isEmpty());
    }

    @Test
    void twoRevertedOriginalsThatExcludeEachOtherReleaseOnlyOne() {
        registerType(new SkillType.Builder("rival_a", "A", "a.png", SkillTier.NOTABLE).exclusiveSkillTypeIds(List.of("rival_b")).build());
        registerType(new SkillType.Builder("rival_b", "B", "a.png", SkillTier.NOTABLE).exclusiveSkillTypeIds(List.of("rival_a")).build());
        SkillNode first = node("a_rival_node", "rival_a", 100f, 100f);
        SkillNode second = node("b_rival_node", "rival_b", -100f, 100f);
        ShipSkillData data = shipWith(TRANSFORM, 14L);
        frigateGate(data);
        ShipTrees.allocate(data, first, null, 0);
        ShipTrees.allocate(data, second, null, 0);
        assertEquals(Set.of(first.getId(), second.getId()), data.getReplacementAllocatedNodeIds());
        ShipFacts frigate = new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 100f, hullModId -> false);

        data.unsocketItem(socket.getId());

        assertEquals(List.of(first.getId()), DoctrineRevertRelease.release(data, frigate, SkillTree.topology(), socket.getId()).nodeIds());
        assertTrue(data.isAllocated(second.getId()));
    }

    @Test
    void aRevertedNodeWhoseOriginalIsAllowedStaysAndIsNoLongerTracked() {
        ShipSkillData data = shipWith(TRANSFORM, 14L);
        frigateGate(data);
        ShipTrees.allocate(data, nearNotable, null, 0);
        ShipFacts frigate = new ShipFacts(HullSize.FRIGATE, ShieldType.FRONT, false, 100f, hullModId -> false);

        data.unsocketItem(socket.getId());

        assertTrue(DoctrineRevertRelease.release(data, frigate, SkillTree.topology(), socket.getId()).nodeIds().isEmpty());
        assertTrue(data.isAllocated(nearNotable.getId()));
        assertTrue(data.getReplacementAllocatedNodeIds().isEmpty());
    }

    @Test
    void anAugmentDoctrineAddsSeededLinesToEveryNodeInRangeThatOnlyApplyWhileAllocated() {
        ShipSkillData data = shipWith(AUGMENT, 5L, nearNotable);

        assertEquals("original", nearNotable.resolveEffectiveType(data).getId());
        for (SkillNode node : List.of(nearNotable, otherNearNotable, nearSmall, nearKeystone)) {
            DoctrineRewrite rewrite = DoctrineOverrides.rewrite(data, node.getId());
            assertNotNull(rewrite, node.getId());
            assertFalse(rewrite.transformed());
            int lineCount = rewrite.augmentLines().size();
            assertTrue(lineCount >= 1 && lineCount <= 2, node.getId() + " has " + lineCount);
        }
        assertNull(DoctrineOverrides.rewrite(data, farNotable.getId()));

        List<SkillTypeEffect> added = DoctrineOverrides.addedEffects(data, nearNotable, HullSize.CRUISER);
        List<SkillTypeEffect> applied = AllocatedSkillEffects.appliedEffects(data, new AllocatedNode(nearNotable, nearNotable.resolveEffectiveType(data)),
                HullSize.CRUISER);
        assertTrue(applied.containsAll(added));
        for (SkillTypeEffect effect : added) {
            String name = effect.effect().name();
            float magnitude = effect.magnitude();
            assertTrue(name.equals("ARMOR_PERCENT") ? magnitude >= 2f && magnitude <= 4f : magnitude >= 1f && magnitude <= 3f, name + " " + magnitude);
        }
        List<SkillEffect> gathered = AllocatedSkillEffects.forData(data, HullSize.CRUISER);
        long armorLinesGathered = gathered.stream().filter(effect -> effect.name().equals("ARMOR_PERCENT")).count();
        long armorLinesOnAllocated = added.stream().filter(effect -> effect.effect().name().equals("ARMOR_PERCENT")).count();
        assertEquals(armorLinesOnAllocated, armorLinesGathered);
    }

    @Test
    void theLayerIsCachedUntilTheShipChanges() {
        ShipSkillData data = shipWith(TRANSFORM, 3L, nearNotable);
        DoctrineLayer first = DoctrineOverrides.layer(data);

        assertSame(first, DoctrineOverrides.layer(data));
        ShipTrees.deallocate(data, nearNotable);
        assertNotSameLayer(first, DoctrineOverrides.layer(data));
    }

    private static void assertNotSameLayer(DoctrineLayer first, DoctrineLayer second) {
        assertFalse(first == second);
    }

    @Test
    void aShipWithoutSocketedItemsHasNoLayer() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.allocate(data, nearNotable, 0);

        assertTrue(DoctrineOverrides.layer(data).isEmpty());
        assertEquals("original", nearNotable.resolveEffectiveType(data).getId());
    }

    @Test
    void theRadiusRingCentresOnTheDoctrinesSocket() {
        Item doctrine = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(TRANSFORM), 1L));
        Item relic = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(UNIQUE_RELIC), 1L));

        SubroutineRadius.Footprint footprint = SubroutineRadius.footprint(doctrine, socket.getId());
        assertEquals(socket.getId(), footprint.centreNodeId());
        assertEquals(exiledsector.skills.radius.RadiusSize.MEDIUM, footprint.size());
        assertNull(SubroutineRadius.footprint(relic, socket.getId()));
    }

    @Test
    void theItemTooltipShowsTheFirmwareRevision() {
        Item doctrine = ItemStore.get().add(ItemData.rolled(ItemDefinitions.get(TRANSFORM), 4471L));
        List<String> lines = new ArrayList<>();
        doctrine.headerLines().forEach(line -> lines.add(line.toMarkup()));

        assertTrue(lines.stream().anyMatch(line -> line.contains(String.valueOf(DoctrineRewriter.revision(4471L)))), lines.toString());
    }
}
