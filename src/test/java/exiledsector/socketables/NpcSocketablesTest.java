package exiledsector.socketables;

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

import static exiledsector.socketables.SocketableFixtures.MILITARY;
import static exiledsector.socketables.SocketableFixtures.MILITARY_PREFIXES;
import static exiledsector.socketables.SocketableFixtures.MILITARY_SUFFIXES;
import static exiledsector.socketables.SocketableFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcSocketablesTest {

    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SocketableDefinitions.register(new JSONArray()
                .put(row(MILITARY, "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("relic", "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES).put("unique", "true"))
                .put(row("core", "ai_core", MILITARY_PREFIXES, MILITARY_SUFFIXES))
                .put(row("unweighted", "subroutine", MILITARY_PREFIXES, MILITARY_SUFFIXES).put("rarity", "0")));
    }

    @AfterEach
    void tearDown() {
        NpcSocketables.clearCache();
        SocketableDefinitions.clear();
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
        String id = NpcSocketables.id(MILITARY, -7L);

        assertTrue(NpcSocketables.isNpcId(id));
        assertEquals(new SocketableItemData(MILITARY, -7L), NpcSocketables.item(id));
        assertFalse(NpcSocketables.isNpcId("socketable_3"));
        assertNull(NpcSocketables.item("npc:missing_seed"));
        assertNull(NpcSocketables.item("npc:x/not_a_number"));
        assertNull(NpcSocketables.item(null));
    }

    @Test
    void theFirstChanceStartsAtFivePercentPlusOnePerLevelAndTheSecondOpensAtLevelFifteen() {
        assertEquals(0.06f, NpcSocketables.firstChance(1), 1e-6f);
        assertEquals(0.20f, NpcSocketables.firstChance(15), 1e-6f);
        assertEquals(0f, NpcSocketables.secondChance(14));
        assertEquals(0.05f, NpcSocketables.secondChance(15), 1e-6f);
        assertEquals(0.05f, NpcSocketables.secondChance(30), 1e-6f);
    }

    @Test
    void rollingCountsTheFirstAndSecondChancesIndependently() {
        assertEquals(1, NpcSocketables.rollCount(15, fixedFloats(0.19f, 0.5f)));
        assertEquals(2, NpcSocketables.rollCount(15, fixedFloats(0.19f, 0.04f)));
        assertEquals(1, NpcSocketables.rollCount(15, fixedFloats(0.5f, 0.04f)));
        assertEquals(0, NpcSocketables.rollCount(14, fixedFloats(0.5f, 0.0f)));
    }

    @Test
    void onlyWeightedNonUniqueSubroutinesCanBeDrawnForFleets() {
        Set<String> drawn = new HashSet<>();
        Random random = new Random(3L);
        for (int i = 0; i < 200; i++) {
            drawn.add(NpcSocketables.pickDefinition(random).id());
        }

        assertEquals(Set.of(MILITARY), drawn);
    }

    @Test
    void aPreviewRollsTheSameEffectsAsTheItemWouldOnceOwned() {
        String id = NpcSocketables.id(MILITARY, 99L);

        Socketable preview = SocketableStore.lookup(id);
        Socketable owned = SocketableStore.get().add(SocketableDefinitions.get(MILITARY), 99L);

        assertNotNull(preview);
        assertEquals(owned.effects(), preview.effects());
        assertSame(preview, SocketableStore.lookup(id));
        assertNull(SocketableStore.lookup(NpcSocketables.id("removed_definition", 1L)));
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
        data.socketItem("socket_1", NpcSocketables.id(MILITARY, 5L));

        NpcSocketables.claimForPlayer(data);

        List<Socketable> owned = SocketableStore.get().owned();
        assertEquals(1, owned.size());
        assertEquals(owned.get(0).id(), data.getSocketedItem("socket_1"));
        assertEquals(5L, owned.get(0).seed());
        assertTrue(NpcSocketables.carriedBy(data).isEmpty());
    }
}
