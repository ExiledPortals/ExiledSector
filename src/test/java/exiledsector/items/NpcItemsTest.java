package exiledsector.items;

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

import static exiledsector.items.ItemFixtures.SUBROUTINE;
import static exiledsector.items.ItemFixtures.MILITARY_PREFIXES;
import static exiledsector.items.ItemFixtures.MILITARY_SUFFIXES;
import static exiledsector.items.ItemFixtures.row;
import static exiledsector.items.ItemFixtures.subroutineRow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcItemsTest {

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        ItemDefinitions.register(new JSONArray()
                .put(subroutineRow(SUBROUTINE))
                .put(subroutineRow("relic", SubroutineKind.UNIQUE).put("unique", "true"))
                .put(row("gunners", "weapon_mount", MILITARY_PREFIXES, MILITARY_SUFFIXES).put("unique", "true"))
                .put(row("core", "reactor", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(subroutineRow("unweighted").put("rarity", "0")));
    }

    @AfterEach
    void tearDown() {
        NpcItems.clearCache();
        ItemDefinitions.clear();
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
        String id = NpcItems.id(SUBROUTINE, -7L);

        assertTrue(NpcItems.isNpcId(id));
        assertEquals(new ItemData(SUBROUTINE, -7L), NpcItems.item(id));
        assertFalse(NpcItems.isNpcId("socketable_3"));
        assertNull(NpcItems.item("npc:missing_seed"));
        assertNull(NpcItems.item("npc:x/not_a_number"));
        assertNull(NpcItems.item(null));
    }

    @Test
    void theFirstChanceStartsAtFivePercentPlusOnePerLevelAndTheSecondOpensAtLevelFifteen() {
        assertEquals(0.06f, NpcItems.firstChance(1), 1e-6f);
        assertEquals(0.20f, NpcItems.firstChance(15), 1e-6f);
        assertEquals(0f, NpcItems.secondChance(14));
        assertEquals(0.05f, NpcItems.secondChance(15), 1e-6f);
        assertEquals(0.05f, NpcItems.secondChance(30), 1e-6f);
    }

    @Test
    void rollingCountsTheFirstAndSecondChancesIndependently() {
        assertEquals(1, NpcItems.rollCount(15, fixedFloats(0.19f, 0.5f)));
        assertEquals(2, NpcItems.rollCount(15, fixedFloats(0.19f, 0.04f)));
        assertEquals(1, NpcItems.rollCount(15, fixedFloats(0.5f, 0.04f)));
        assertEquals(0, NpcItems.rollCount(14, fixedFloats(0.5f, 0.0f)));
    }

    @Test
    void fleetsNeverDrawAPlayerOnlySubroutineKind() {
        Random random = new Random(3L);
        for (int i = 0; i < 200; i++) {
            assertNull(NpcItems.pickDefinition(random, definition -> false));
        }
    }

    @Test
    void fleetTreeSocketsNeverDrawASectionUnique() {
        Random random = new Random(11L);
        for (int i = 0; i < 20000; i++) {
            ItemDefinition picked = NpcItems.pickDefinition(random, definition -> true);
            if (picked != null) {
                assertEquals(ItemKind.SUBROUTINE, picked.kind());
            }
        }
    }

    @Test
    void aSmallShareOfFleetHullItemsAreUniquesWhenAUniqueCanDrop() {
        Random random = new Random(5L);
        int uniques = 0;
        int trials = 20000;
        for (int i = 0; i < trials; i++) {
            ItemDefinition picked = NpcItems.pickDefinition(random, definition -> true);
            if (picked != null && picked.unique()) {
                uniques++;
            }
        }

        assertEquals(NpcItems.UNIQUE_SHARE, uniques / (float) trials, 0.004f);
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
        data.socketItem("socket_a", NpcItems.id(SUBROUTINE, 1L));
        data.socketItem("socket_b", NpcItems.id("relic", 2L));

        List<Item> uniques = NpcItems.uniquesCarriedBy(data);

        assertEquals(List.of("relic"), uniques.stream().map(Item::definitionId).toList());
        assertTrue(NpcItems.uniquesCarriedBy(null).isEmpty());
    }

    @Test
    void aPreviewRollsTheSameEffectsAsTheItemWouldOnceOwned() {
        String id = NpcItems.id(SUBROUTINE, 99L);

        Item preview = ItemStore.lookup(id);
        Item owned = ItemStore.get().add(ItemDefinitions.get(SUBROUTINE), 99L);

        assertNotNull(preview);
        assertEquals(owned.effects(), preview.effects());
        assertSame(preview, ItemStore.lookup(id));
        assertNull(ItemStore.lookup(NpcItems.id("removed_definition", 1L)));
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
        data.socketItem("socket_1", NpcItems.id(SUBROUTINE, 5L));

        NpcItems.claimForPlayer(data);

        List<Item> owned = ItemStore.get().owned();
        assertEquals(1, owned.size());
        assertEquals(owned.get(0).id(), data.getSocketedItem("socket_1"));
        assertEquals(5L, owned.get(0).seed());
        assertTrue(NpcItems.carriedBy(data).isEmpty());
    }
}
