package exiledsector.hullitems;

import exiledsector.skills.ShipSkillData;
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

    private final HullItemStore store = new HullItemStore();
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
        HullItemDefinitions.register(new JSONArray()
                .put(HullItemFixtures.row("chip", "subroutine", "HULL_MULT:4:6"))
                .put(HullItemFixtures.row("gunners", "weapon_mount", "HULL_MULT:4:6").put("unique", "TRUE")));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        HullItemDefinitions.clear();
    }

    private ShipSkillData shipWith(HullItem inFirst, HullItem inSecond) {
        ShipSkillData shipData = new ShipSkillData();
        shipData.allocate(firstSocket, 3);
        shipData.allocate(secondSocket, 3);
        shipData.socketItem(firstSocket.getId(), inFirst.id());
        shipData.socketItem(secondSocket.getId(), inSecond.id());
        ships.put("ship", shipData);
        return shipData;
    }

    @Test
    void aTreeSocketOnlyTakesSubroutines() {
        HullItem chip = store.add(HullItemDefinitions.get("chip"), 1L);
        HullItem gunners = store.add(HullItemDefinitions.get("gunners"), 2L);

        assertTrue(chip.canSocketInto(firstSocket));
        assertFalse(gunners.canSocketInto(firstSocket));
        assertTrue(gunners.canSocketInto(HullItemType.WEAPON_MOUNT));
        assertFalse(gunners.canSocketInto(HullItemType.BRIDGE));
        assertFalse(chip.canSocketInto(HullItemType.SUBROUTINE));
    }

    @Test
    void itemsThatNoLongerFitATreeSocketAreReturnedToStorageOnce() {
        HullItem chip = store.add(HullItemDefinitions.get("chip"), 1L);
        HullItem gunners = store.add(HullItemDefinitions.get("gunners"), 2L);
        ShipSkillData shipData = shipWith(chip, gunners);

        assertEquals(List.of(gunners), SocketTypeMigration.returnMisfits(ships, store));
        assertEquals(chip.id(), shipData.getSocketedItem(firstSocket.getId()));
        assertNull(shipData.getSocketedItem(secondSocket.getId()));
        assertEquals(List.of(chip, gunners), store.owned());
        assertEquals(List.of(), SocketTypeMigration.returnMisfits(ships, store));
    }

    @Test
    void itemsWhoseDefinitionIsMissingAreLeftInPlace() {
        HullItem chip = store.add(HullItemDefinitions.get("chip"), 1L);
        HullItem gunners = store.add(HullItemDefinitions.get("gunners"), 2L);
        ShipSkillData shipData = shipWith(chip, gunners);
        HullItemDefinitions.clear();

        assertEquals(List.of(), SocketTypeMigration.returnMisfits(ships, store));
        assertEquals(gunners.id(), shipData.getSocketedItem(secondSocket.getId()));
    }

    @Test
    void aTransposedItemThatChangedTypeLeavesItsTreeSocket() {
        HullItem chip = store.add(HullItemDefinitions.get("chip"), 1L);
        HullItem gunners = store.add(HullItemDefinitions.get("gunners"), 2L);
        ShipSkillData shipData = shipWith(chip, gunners);

        assertFalse(SocketCustody.unsocketIfMisfit(ships, chip));
        assertTrue(SocketCustody.unsocketIfMisfit(ships, gunners));
        assertNull(shipData.getSocketedItem(secondSocket.getId()));
        assertEquals(chip.id(), shipData.getSocketedItem(firstSocket.getId()));
    }
}
