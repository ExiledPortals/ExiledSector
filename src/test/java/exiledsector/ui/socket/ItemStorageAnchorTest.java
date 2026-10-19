package exiledsector.ui.socket;

import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.skills.SkillTree;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStorageAnchorTest {

    private static final String ANCHOR_ID = "wormhole_anchor_abyss";

    private Item anchor;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        WormholeAnchorDefinitions.register(new JSONArray().put(new JSONObject().put("id", ANCHOR_ID).put("volume", "abyss")
                .put("exit_node", "rat_abyss_wormhole_1").put("name", "Abyssal Lock").put("description", "A navigation lock.")));
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

        assertEquals("Abyssal Lock", row.name());
        assertTrue(row.searchText().contains("wormhole anchor"), row.searchText());
        assertTrue(row.searchText().contains("leads to the abyss volume"), row.searchText());
        assertNull(row.installedShipName());
    }

    @Test
    void anchorsAreListedWithSubroutinesAndNeverWithSectionItems() {
        Set<ItemKind> noChips = EnumSet.noneOf(ItemKind.class);

        assertEquals(ItemKind.SUBROUTINE, ItemStoragePanel.listedKind(anchor));
        assertTrue(ItemStoragePanel.showsItem(anchor, ItemKind.SUBROUTINE, false, noChips));
        assertTrue(ItemStoragePanel.showsItem(anchor, null, false, noChips));
        assertTrue(ItemStoragePanel.showsItem(anchor, null, false, EnumSet.of(ItemKind.SUBROUTINE)));
        assertFalse(ItemStoragePanel.showsItem(anchor, null, false, EnumSet.of(ItemKind.BRIDGE)));
        assertFalse(ItemStoragePanel.showsItem(anchor, ItemKind.BRIDGE, true, noChips));
        assertFalse(ItemStoragePanel.showsItem(anchor, null, true, noChips));
    }
}
