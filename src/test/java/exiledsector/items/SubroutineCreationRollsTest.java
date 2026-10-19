package exiledsector.items;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.i18n.StyledText;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static exiledsector.items.ItemFixtures.subroutineRow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubroutineCreationRollsTest {

    private static final String VOLUME = "volume_sub";
    private static final String BYPASS = "bypass_sub";
    private static final String DOCTRINE = "doctrine_sub";
    private static final String UNIQUE = "unique_sub";
    private static final List<String> FIRST_POOL = List.of("notable_c", "notable_a", "notable_b", "notable_d");
    private static final List<String> SECOND_POOL = List.of("notable_x", "notable_y");

    private final ItemStore store = new ItemStore();

    @BeforeEach
    void setUp() throws Exception {
        registerDefinitions();
    }

    @AfterEach
    void tearDown() {
        SubroutineNodePools.clear();
        NpcItems.clearCache();
        ItemDefinitions.clear();
        SkillTree.clearNodes();
    }

    private static void registerDefinitions() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(subroutineRow(VOLUME, SubroutineKind.VOLUME))
                .put(subroutineRow(BYPASS, SubroutineKind.BYPASS))
                .put(subroutineRow(DOCTRINE, SubroutineKind.DOCTRINE))
                .put(subroutineRow(UNIQUE, SubroutineKind.UNIQUE).put("unique", "true")));
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        ItemSaveAliases.register(xstream);
        return xstream;
    }

    private Subroutine stored(String definitionId, long seed) {
        return (Subroutine) store.add(ItemDefinitions.get(definitionId), seed);
    }

    private static List<String> plainLines(Item item) {
        return item.headerLines().stream().map(StyledText::plain).toList();
    }

    @Test
    void volumeAndBypassSubroutinesAreBornCorruptedOnEveryCreationPath() {
        for (String definitionId : List.of(VOLUME, BYPASS)) {
            List<Item> created = new ArrayList<>();
            created.add(stored(definitionId, 3L));
            created.add(Item.create("loose", definitionId, 4L, List.of()));
            created.add(ItemData.rolled(ItemDefinitions.get(definitionId), 5L).preview());
            created.add(ItemData.of(new ItemData(definitionId, 6L).toSpecialItem()).preview());
            created.add(NpcItems.resolve(NpcItems.id(definitionId, 7L)));
            created.add(store.add(NpcItems.item(NpcItems.id(definitionId, 8L))));
            created.add(new Subroutine(new UnresolvedItem("converted", definitionId, 9L, List.of())));

            for (Item item : created) {
                assertTrue(item.corrupted(), definitionId + " " + item.id());
                assertSame(CorruptionOutcome.INNATE, item.corruption(), definitionId + " " + item.id());
            }
        }
    }

    @Test
    void doctrineAndUniqueSubroutinesAreNotBornCorrupted() {
        for (String definitionId : List.of(DOCTRINE, UNIQUE)) {
            assertFalse(stored(definitionId, 3L).corrupted(), definitionId);
            assertFalse(NpcItems.resolve(NpcItems.id(definitionId, 7L)).corrupted(), definitionId);
            assertFalse(new Subroutine(new UnresolvedItem("converted", definitionId, 9L, List.of())).corrupted(), definitionId);
        }
    }

    @Test
    void kindPropertiesMatchThePlan() {
        assertTrue(SubroutineKind.VOLUME.bornCorrupted() && SubroutineKind.VOLUME.rollsNode());
        assertTrue(SubroutineKind.BYPASS.bornCorrupted() && SubroutineKind.BYPASS.rollsNode());
        assertFalse(SubroutineKind.DOCTRINE.bornCorrupted() || SubroutineKind.DOCTRINE.rollsNode());
        assertFalse(SubroutineKind.UNIQUE.bornCorrupted() || SubroutineKind.UNIQUE.rollsNode());
        assertEquals(1, SubroutineKind.DOCTRINE.socketLimitPerShip());
        assertEquals(1, SubroutineKind.BYPASS.socketLimitPerShip());
        assertFalse(SubroutineKind.VOLUME.limitsSocketsPerShip());
        assertFalse(SubroutineKind.UNIQUE.limitsSocketsPerShip());
    }

    @Test
    void aBornCorruptedSubroutineRefusesEveryCurrencyThatAnUncorruptedOneTakes() {
        Item doctrine = stored(DOCTRINE, 3L);
        assertTrue(ItemCrafting.canUse(CraftingCurrency.CORRUPTION, doctrine, definition -> true));
        assertTrue(ItemCrafting.canUse(CraftingCurrency.REPLICATION, doctrine, definition -> true));

        for (String definitionId : List.of(VOLUME, BYPASS)) {
            Item bornCorrupted = stored(definitionId, 3L);
            for (CraftingCurrency currency : CraftingCurrency.values()) {
                assertFalse(ItemCrafting.canUse(currency, bornCorrupted, definition -> true), currency + " on " + definitionId);
            }
        }
    }

    @Test
    void theTooltipSaysCorruptedWithoutClaimingAnythingChanged() {
        List<String> lines = plainLines(stored(VOLUME, 3L));

        assertTrue(lines.contains("Corrupted from the start"), lines.toString());
        assertFalse(lines.contains("Corrupted"), lines.toString());
    }

    @Test
    void theNodePickIsDeterministicAndIgnoresCandidateOrder() {
        List<String> reversed = new ArrayList<>(FIRST_POOL);
        Collections.reverse(reversed);
        Set<String> seen = new HashSet<>();
        for (long seed = -20; seed < 20; seed++) {
            String picked = SubroutineNodePools.pick(FIRST_POOL, seed);
            assertEquals(picked, SubroutineNodePools.pick(reversed, seed));
            assertEquals(picked, SubroutineNodePools.pick(FIRST_POOL, seed));
            assertEquals(picked, SubroutineNodePools.pick(List.of("notable_a", "notable_b", "notable_a", "notable_c", "notable_d", " "), seed));
            seen.add(picked);
        }
        assertEquals(Set.copyOf(FIRST_POOL), seen);
        assertEquals(List.of("notable_c", "notable_b", "notable_x"), List.of(SubroutineNodePools.pick(FIRST_POOL, 42L), SubroutineNodePools.pick(FIRST_POOL, -7L),
                SubroutineNodePools.pick(SECOND_POOL, 42L)), "NPC item ids decode to these nodes, so the pick must never change");
        assertNull(SubroutineNodePools.pick(List.of(), 1L));
        assertNull(SubroutineNodePools.pick(null, 1L));
    }

    @Test
    void withoutAPoolANodeRollingKindLeavesTheNodeEmptyAndDoesNotCrash() {
        Subroutine volume = stored(VOLUME, 3L);
        SubroutineNodePools.use(SubroutineKind.BYPASS, definition -> List.of());

        assertNull(volume.rolledNodeId());
        assertNull(stored(BYPASS, 3L).rolledNodeId());
        assertTrue(plainLines(volume).stream().noneMatch(line -> line.startsWith("Encoded node")), plainLines(volume).toString());
    }

    @Test
    void aBrokenPoolRollsNoNodeInsteadOfCrashing() {
        SubroutineNodePools.use(SubroutineKind.VOLUME, definition -> {
            throw new IllegalStateException("tree not ready");
        });

        assertNull(stored(VOLUME, 3L).rolledNodeId());
    }

    @Test
    void onlyKindsThatRollANodeGetOneAndEachPathAgreesOnIt() {
        SubroutineNodePools.use(SubroutineKind.VOLUME, definition -> FIRST_POOL);
        SubroutineNodePools.use(SubroutineKind.DOCTRINE, definition -> FIRST_POOL);
        String expected = SubroutineNodePools.pick(FIRST_POOL, 42L);

        assertEquals(expected, stored(VOLUME, 42L).rolledNodeId());
        assertEquals(expected, ((Subroutine) NpcItems.resolve(NpcItems.id(VOLUME, 42L))).rolledNodeId());
        assertEquals(expected, ((Subroutine) store.add(NpcItems.item(NpcItems.id(VOLUME, 42L)))).rolledNodeId());
        assertEquals(expected, ((Subroutine) ItemData.of(new ItemData(VOLUME, 42L).toSpecialItem()).preview()).rolledNodeId());
        assertNull(stored(DOCTRINE, 42L).rolledNodeId());
        assertNull(stored(UNIQUE, 42L).rolledNodeId());
    }

    @Test
    void theRolledNodeIsFrozenThroughCopiesAndSavesWhenThePoolChanges() {
        SubroutineNodePools.use(SubroutineKind.BYPASS, definition -> FIRST_POOL);
        Subroutine original = stored(BYPASS, 11L);
        String rolledNodeId = original.rolledNodeId();
        assertNotNull(rolledNodeId);
        String xml = xstream().toXML(store);

        SubroutineNodePools.use(SubroutineKind.BYPASS, definition -> SECOND_POOL);
        Subroutine reloaded = (Subroutine) ((ItemStore) xstream().fromXML(xml)).owned().get(0);
        Subroutine mirror = (Subroutine) store.addMirrorOf(original);
        Subroutine converted = new Subroutine(original);

        assertTrue(xml.contains("<rolledNodeId>" + rolledNodeId + "</rolledNodeId>"), xml);
        assertEquals(rolledNodeId, reloaded.rolledNodeId());
        assertEquals(CorruptionOutcome.INNATE, reloaded.corruption());
        assertEquals(rolledNodeId, mirror.rolledNodeId());
        assertEquals(rolledNodeId, converted.rolledNodeId());
        assertTrue(SECOND_POOL.contains(stored(BYPASS, 11L).rolledNodeId()), "a new item rolls from the current pool");
    }

    @Test
    void theRolledNodeSurvivesALoadWhileTheDefinitionIsMissing() throws Exception {
        SubroutineNodePools.use(SubroutineKind.VOLUME, definition -> FIRST_POOL);
        String rolledNodeId = stored(VOLUME, 12L).rolledNodeId();
        String xml = xstream().toXML(store);

        ItemDefinitions.clear();
        Item unresolved = ((ItemStore) xstream().fromXML(xml)).owned().get(0);
        assertEquals(UnresolvedItem.class, unresolved.getClass());
        String resaved = xstream().toXML(unresolved);

        SubroutineNodePools.use(SubroutineKind.VOLUME, definition -> SECOND_POOL);
        registerDefinitions();
        Item restored = (Item) xstream().fromXML(resaved);

        assertEquals(Subroutine.class, restored.getClass());
        assertEquals(rolledNodeId, ((Subroutine) restored).rolledNodeId());
        assertEquals(CorruptionOutcome.INNATE, restored.corruption());
    }

    @Test
    void theTooltipNamesTheRolledNodeAndCopesWithANodeThatIsGone() {
        SkillType keystone = new SkillType.Builder("test_keystone", "Safety Overrides", "", SkillTier.KEYSTONE).build();
        SkillTree.register(new SkillNode("notable_a", keystone, List.of(), 0f, 0f));
        SubroutineNodePools.use(SubroutineKind.BYPASS, definition -> List.of("notable_a"));
        Subroutine known = stored(BYPASS, 1L);
        SubroutineNodePools.use(SubroutineKind.BYPASS, definition -> List.of("removed_node"));
        Subroutine orphaned = stored(BYPASS, 2L);

        assertTrue(plainLines(known).contains("Encoded node: Safety Overrides"), plainLines(known).toString());
        assertEquals("removed_node", orphaned.rolledNodeId());
        assertTrue(plainLines(orphaned).contains("Encoded node: unknown (no longer in the skill tree)"), plainLines(orphaned).toString());
    }

    @Test
    void aSubroutineFitsATreeSocketOnlyWhileItsDefinitionHasAKind() {
        SkillNode socket = new SkillNode("socket", new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build(), List.of(), 0f, 0f);
        SkillNode notable = new SkillNode("notable", new SkillType.Builder("notable_type", "Notable", "", SkillTier.NOTABLE).build(), List.of(), 0f, 0f);
        Subroutine unique = stored(UNIQUE, 1L);
        Subroutine withoutDefinition = new Subroutine(new UnresolvedItem("orphan", "not_loaded", 1L, List.of()));

        assertTrue(unique.canSocketInto(socket));
        assertFalse(unique.canSocketInto(notable));
        assertFalse(withoutDefinition.canSocketInto(socket));
    }
}
