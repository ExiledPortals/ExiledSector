package exiledsector.ui.socket;

import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemTypes;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.skills.SkillTree;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStorageAnchorTest {

    private static final String ANCHOR_ID = "wormhole_anchor_abyss";

    private static final String OTHER_ANCHOR_ID = "wormhole_anchor_stargazer";

    private Item anchor;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        WormholeAnchorDefinitions.register(new JSONArray().put(new JSONObject().put("id", ANCHOR_ID).put("volume", "abyss")
                .put("exit_node", "rat_abyss_wormhole_1").put("name", "Abyss Wormhole Anchor").put("description", "Opens a wormhole to the Abyss volume."))
                .put(new JSONObject().put("id", OTHER_ANCHOR_ID).put("volume", "stargazer").put("exit_node", "stargazer_wormhole_1")
                        .put("name", "Stargazer Wormhole Anchor").put("description", "Opens a wormhole to the Stargazer volume.")));
        anchor = ItemData.rolled(ItemDefinitions.get(ANCHOR_ID), 5L).preview();
    }

    @AfterEach
    void tearDown() {
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
    }

    @Test
    void aKindlessAnchorBuildsAStorageRowSearchableByItsNoun() {
        ItemStorageRow row = ItemStorageRow.of(anchor, 0, item -> null);

        assertEquals("Abyss Wormhole Anchor", row.name());
        assertTrue(row.searchLines().stream().anyMatch(line -> line.contains("wormhole anchor")), row.searchLines().toString());
        assertTrue(row.searchLines().stream().anyMatch(line -> line.contains("opens a wormhole to the abyss volume")), row.searchLines().toString());
        assertNull(row.installedShipName());
    }

    @Test
    void anchorsAreListedWithSubroutinesAndNeverWithSectionItems() {
        Set<ItemKind> noChips = EnumSet.noneOf(ItemKind.class);

        assertEquals(ItemKind.SUBROUTINE, ItemTypes.listedKind(anchor));
        assertTrue(ItemStoragePanel.showsItem(anchor, ItemKind.SUBROUTINE, false, noChips));
        assertTrue(ItemStoragePanel.showsItem(anchor, null, false, noChips));
        assertTrue(ItemStoragePanel.showsItem(anchor, null, false, EnumSet.of(ItemKind.SUBROUTINE)));
        assertFalse(ItemStoragePanel.showsItem(anchor, null, false, EnumSet.of(ItemKind.BRIDGE)));
        assertFalse(ItemStoragePanel.showsItem(anchor, ItemKind.BRIDGE, true, noChips));
        assertFalse(ItemStoragePanel.showsItem(anchor, null, true, noChips));
    }

    @Test
    void freeAnchorsOfOneKindShareATileCountingThemAll() {
        Item second = anchorCopy(6L);
        Item third = anchorCopy(7L);
        Item other = ItemData.rolled(ItemDefinitions.get(OTHER_ANCHOR_ID), 8L).preview();

        List<ItemStorageRow> rows = ItemStorageStacks.rows(List.of(anchor, other, second, third), item -> true, item -> null, item -> false);

        assertEquals(2, rows.size());
        ItemStorageRow stack = rows.stream().filter(row -> row.item().definitionId().equals(ANCHOR_ID)).findFirst().orElseThrow();
        assertEquals(3, stack.stackSize());
        assertEquals(3, stack.storeOrder());
        assertSame(third, stack.item());
        assertEquals(1, rows.stream().filter(row -> row.item() == other).findFirst().orElseThrow().stackSize());
    }

    @Test
    void anInstalledAnchorKeepsItsOwnTile() {
        Item installed = anchorCopy(6L);

        List<ItemStorageRow> rows = ItemStorageStacks.rows(List.of(anchor, installed), item -> true, item -> item == installed ? "Onslaught" : null,
                item -> false);

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.stackSize() == 1));
    }

    @Test
    void theSelectedAnchorFrontsItsStack() {
        Item second = anchorCopy(6L);

        List<ItemStorageRow> rows = ItemStorageStacks.rows(List.of(anchor, second), item -> true, item -> null, item -> item == anchor);

        assertEquals(1, rows.size());
        assertSame(anchor, rows.get(0).item());
        assertEquals(2, rows.get(0).stackSize());
    }

    private static Item anchorCopy(long seed) {
        return ItemData.rolled(ItemDefinitions.get(ANCHOR_ID), seed).preview();
    }
}
