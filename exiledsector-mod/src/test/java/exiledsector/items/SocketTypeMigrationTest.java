package exiledsector.items;

import exiledlib.items.Item;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemStore;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketTypeMigrationTest {

    private final ItemStore store = new ItemStore();
    private final Map<String, ShipSkillData> ships = new LinkedHashMap<>();
    private SkillNode firstSocket;
    private SkillNode secondSocket;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        SkillType socketType = new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build();
        firstSocket = new SkillNode("socket_a", socketType, List.of(), 0f, 0f);
        secondSocket = new SkillNode("socket_b", socketType, List.of(), 0f, 0f);
        SkillTree.register(firstSocket);
        SkillTree.register(secondSocket);
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.subroutineRow("chip"))
                .put(ItemFixtures.row("gunners", "weapon_mount", "HULL_MULT:4:6").put("unique", "TRUE")));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        ItemDefinitions.clear();
    }

    private ShipSkillData shipWith(Item inFirst, Item inSecond) {
        ShipSkillData shipData = new ShipSkillData();
        ShipTrees.allocate(shipData, firstSocket, 3);
        ShipTrees.allocate(shipData, secondSocket, 3);
        shipData.socketItem(firstSocket.getId(), inFirst.id());
        shipData.socketItem(secondSocket.getId(), inSecond.id());
        ships.put("ship", shipData);
        return shipData;
    }

    @Test
    void aTreeSocketOnlyTakesSubroutines() {
        Item chip = store.add(ItemDefinitions.get("chip"), 1L);
        Item gunners = store.add(ItemDefinitions.get("gunners"), 2L);

        assertTrue(NodeSocketable.fits(chip, firstSocket));
        assertFalse(NodeSocketable.fits(gunners, firstSocket));
        assertTrue(HullItem.fits(gunners, ShipSection.WEAPON_MOUNT));
        assertFalse(HullItem.fits(gunners, ShipSection.BRIDGE));
        assertFalse(HullItem.fits(chip, ShipSection.BRIDGE));
    }

    @Test
    void itemsThatNoLongerFitATreeSocketAreReturnedToStorageOnce() {
        Item chip = store.add(ItemDefinitions.get("chip"), 1L);
        Item gunners = store.add(ItemDefinitions.get("gunners"), 2L);
        ShipSkillData shipData = shipWith(chip, gunners);

        assertEquals(List.of(gunners), SocketTypeMigration.returnMisfits(ships, store));
        assertEquals(chip.id(), shipData.getSocketedItem(firstSocket.getId()));
        assertNull(shipData.getSocketedItem(secondSocket.getId()));
        assertEquals(List.of(chip, gunners), store.owned());
        assertEquals(List.of(), SocketTypeMigration.returnMisfits(ships, store));
    }

    @Test
    void itemsWhoseDefinitionIsMissingAreLeftInPlace() {
        Item chip = store.add(ItemDefinitions.get("chip"), 1L);
        Item gunners = store.add(ItemDefinitions.get("gunners"), 2L);
        ShipSkillData shipData = shipWith(chip, gunners);
        ItemDefinitions.clear();

        assertEquals(List.of(), SocketTypeMigration.returnMisfits(ships, store));
        assertEquals(gunners.id(), shipData.getSocketedItem(secondSocket.getId()));
    }

    @Test
    void aTransposedItemThatChangedTypeLeavesItsTreeSocket() {
        Item chip = store.add(ItemDefinitions.get("chip"), 1L);
        Item gunners = store.add(ItemDefinitions.get("gunners"), 2L);
        ShipSkillData shipData = shipWith(chip, gunners);

        assertFalse(SocketCustody.unsocketIfMisfit(ships, chip));
        assertTrue(SocketCustody.unsocketIfMisfit(ships, gunners));
        assertNull(shipData.getSocketedItem(secondSocket.getId()));
        assertEquals(chip.id(), shipData.getSocketedItem(firstSocket.getId()));
    }
}
