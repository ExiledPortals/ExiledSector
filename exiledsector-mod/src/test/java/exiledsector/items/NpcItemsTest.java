package exiledsector.items;

import exiledlib.MockSector;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcSkillTreeBuilder;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static exiledsector.items.ItemFixtures.SUBROUTINE;
import static exiledsector.items.ItemFixtures.subroutineRow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcItemsTest {

    private static final String HEGEMONY_ANCHOR = "wormhole_anchor_hegemony";

    private MockSector mockSector;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        ItemDefinitions.register(new JSONArray().put(subroutineRow(SUBROUTINE)));
    }

    @AfterEach
    void tearDown() {
        NpcItems.clearCache();
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
        RealSkillData.clear();
        mockSector.close();
    }

    private static void registerHegemonyAnchor(boolean withExit) throws Exception {
        if (withExit) {
            SkillType exitType = new SkillType.Builder("wormhole_exit", "exit", "a.png", SkillTier.WORMHOLE_EXIT).build();
            SkillTree.registerType(exitType);
            SkillTree.register(new SkillNode("hegemony_2", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of("hegemony")));
        }
        WormholeAnchorDefinitions.register(new JSONArray().put(AnchorRows.row(HEGEMONY_ANCHOR, "hegemony", "hegemony_2")));
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
    void aVolumesAnchorIsTheSameShortIdOnEveryShip() throws Exception {
        registerHegemonyAnchor(true);

        String anchorId = NpcItems.anchorFor("hegemony");

        assertEquals(NpcItems.id(HEGEMONY_ANCHOR, 0L), anchorId);
        assertTrue(NpcItems.isAnchor(anchorId));
        assertFalse(NpcItems.isAnchor(NpcItems.id(SUBROUTINE, 0L)));
        assertNull(NpcItems.anchorFor("tritachyon"));
        assertNull(NpcItems.anchorFor(null));
    }

    @Test
    void aVolumeWhoseAreaIsOffHasNoAnchor() throws Exception {
        registerHegemonyAnchor(false);

        assertNull(NpcItems.anchorFor("hegemony"));
    }

    @Test
    void unsocketingEverythingLeavesOnlyThePlayersOwnItems() {
        SkillType rootType = new SkillType.Builder("root_type", "root", "a.png", SkillTier.ROOT).build();
        SkillType socketType = new SkillType.Builder("socket", "socket", "a.png", SkillTier.SOCKET).build();
        SkillTree.registerType(rootType);
        SkillTree.registerType(socketType);
        SkillTree.register(new SkillNode("root", rootType, List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("socket_a", socketType, List.of("root"), 0f, 0f));
        SkillTree.register(new SkillNode("socket_b", socketType, List.of("root"), 0f, 0f));
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 2);
        ShipTrees.allocate(data, SkillTree.get("socket_a"), 1);
        ShipTrees.allocate(data, SkillTree.get("socket_b"), 1);
        data.socketItem("socket_a", NpcItems.id(HEGEMONY_ANCHOR, 0L));
        data.socketItem("socket_b", "socketable_7");

        assertEquals(List.of("socket_a"), NpcItems.unsocketAll(data));
        assertEquals(Map.of("socket_b", "socketable_7"), data.getSocketedItems());
    }
}
