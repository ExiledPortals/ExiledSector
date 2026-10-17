package exiledsector.hullitems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.NpcSkillTreeBuilder;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcHullItemsTest {

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        HullItemDefinitions.register(new JSONArray()
                .put(row(MILITARY, "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("relic", "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES).put("unique", "true"))
                .put(row("gunners", "weapon_mount", MILITARY_PREFIXES, MILITARY_SUFFIXES).put("unique", "true"))
                .put(row("core", "reactor", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("unweighted", "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES).put("rarity", "0")));
    }

    @AfterEach
    void tearDown() {
        NpcHullItems.clearCache();
        HullItemDefinitions.clear();
        RealSkillData.clear();
        globalMock.close();
    }

    private static Random fixedFloats(float... values) {
        return new Random() {
            private int next;

            @Override
            public float nextFloat() {
                return values[next++];
            }
        };
    }

    @Test
    void anIdCarriesTheDefinitionAndSeedAndRejectsAnythingElse() {
        String id = NpcHullItems.id(MILITARY, -7L);

        assertTrue(NpcHullItems.isNpcId(id));
        assertEquals(new HullItemData(MILITARY, -7L), NpcHullItems.item(id));
        assertFalse(NpcHullItems.isNpcId("socketable_3"));
        assertNull(NpcHullItems.item("npc:missing_seed"));
        assertNull(NpcHullItems.item("npc:x/not_a_number"));
        assertNull(NpcHullItems.item(null));
    }

    @Test
    void theFirstChanceStartsAtFivePercentPlusOnePerLevelAndTheSecondOpensAtLevelFifteen() {
        assertEquals(0.06f, NpcHullItems.firstChance(1), 1e-6f);
        assertEquals(0.20f, NpcHullItems.firstChance(15), 1e-6f);
        assertEquals(0f, NpcHullItems.secondChance(14));
        assertEquals(0.05f, NpcHullItems.secondChance(15), 1e-6f);
        assertEquals(0.05f, NpcHullItems.secondChance(30), 1e-6f);
    }

    @Test
    void rollingCountsTheFirstAndSecondChancesIndependently() {
        assertEquals(1, NpcHullItems.rollCount(15, fixedFloats(0.19f, 0.5f)));
        assertEquals(2, NpcHullItems.rollCount(15, fixedFloats(0.19f, 0.04f)));
        assertEquals(1, NpcHullItems.rollCount(15, fixedFloats(0.5f, 0.04f)));
        assertEquals(0, NpcHullItems.rollCount(14, fixedFloats(0.5f, 0.0f)));
    }

    @Test
    void onlyWeightedNonUniqueSubroutinesCanBeDrawnForFleets() {
        Set<String> drawn = new HashSet<>();
        Random random = new Random(3L);
        for (int i = 0; i < 200; i++) {
            drawn.add(NpcHullItems.pickDefinition(random, definition -> false).id());
        }

        assertEquals(Set.of(MILITARY), drawn);
    }

    @Test
    void fleetTreeSocketsNeverDrawAFrameworkUnique() {
        Random random = new Random(11L);
        for (int i = 0; i < 20000; i++) {
            assertEquals(SocketType.SUBROUTINE, NpcHullItems.pickDefinition(random, definition -> true).kind());
        }
    }

    @Test
    void aSmallShareOfFleetHullItemsAreUniquesWhenAUniqueCanDrop() {
        Random random = new Random(5L);
        int uniques = 0;
        int trials = 20000;
        for (int i = 0; i < trials; i++) {
            if (NpcHullItems.pickDefinition(random, definition -> true).unique()) {
                uniques++;
            }
        }

        assertEquals(NpcHullItems.UNIQUE_SHARE, uniques / (float) trials, 0.004f);
    }

    @Test
    void onlyUniqueSocketedItemsAreReportedAsCarriedUniques() {
        SkillType rootType = new SkillType.Builder("root_type", "root", "a.png", SkillTier.ROOT).build();
        SkillType socketType = new SkillType.Builder("socket", "socket", "a.png", SkillTier.SOCKET).build();
        SkillTree.registerType(rootType);
        SkillTree.registerType(socketType);
        SkillTree.register(new SkillNode("root", rootType, List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("socket_a", socketType, List.of("root"), 0f, 0f));
        SkillTree.register(new SkillNode("socket_b", socketType, List.of("root"), 0f, 0f));
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 2);
        data.allocate(SkillTree.get("socket_a"), 1);
        data.allocate(SkillTree.get("socket_b"), 1);
        data.socketItem("socket_a", NpcHullItems.id(MILITARY, 1L));
        data.socketItem("socket_b", NpcHullItems.id("relic", 2L));

        List<HullItem> uniques = NpcHullItems.uniquesCarriedBy(data);

        assertEquals(List.of("relic"), uniques.stream().map(HullItem::definitionId).toList());
        assertTrue(NpcHullItems.uniquesCarriedBy(null).isEmpty());
    }

    @Test
    void aPreviewRollsTheSameEffectsAsTheItemWouldOnceOwned() {
        String id = NpcHullItems.id(MILITARY, 99L);

        HullItem preview = HullItemStore.lookup(id);
        HullItem owned = HullItemStore.get().add(HullItemDefinitions.get(MILITARY), 99L);

        assertNotNull(preview);
        assertEquals(owned.effects(), preview.effects());
        assertSame(preview, HullItemStore.lookup(id));
        assertNull(HullItemStore.lookup(NpcHullItems.id("removed_definition", 1L)));
    }

    @Test
    void claimingForThePlayerMovesNpcItemsIntoStorageAndKeepsThemInTheirSocket() {
        SkillType rootType = new SkillType.Builder("root_type", "root", "a.png", SkillTier.ROOT).build();
        SkillType socketType = new SkillType.Builder("socket", "socket", "a.png", SkillTier.SOCKET).build();
        SkillTree.registerType(rootType);
        SkillTree.registerType(socketType);
        SkillTree.register(new SkillNode("root", rootType, List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("socket_1", socketType, List.of("root"), 0f, 0f));
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 1);
        data.allocate(SkillTree.get("socket_1"), 1);
        data.socketItem("socket_1", NpcHullItems.id(MILITARY, 5L));

        NpcHullItems.claimForPlayer(data);

        List<HullItem> owned = HullItemStore.get().owned();
        assertEquals(1, owned.size());
        assertEquals(owned.get(0).id(), data.getSocketedItem("socket_1"));
        assertEquals(5L, owned.get(0).seed());
        assertTrue(NpcHullItems.carriedBy(data).isEmpty());
    }
}
