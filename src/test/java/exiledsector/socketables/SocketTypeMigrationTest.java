package exiledsector.socketables;

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

    private final SocketableStore store = new SocketableStore();
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
        SocketableDefinitions.register(new JSONArray()
                .put(SocketableFixtures.row("chip", "subroutine", "HULL_MULT:4:6"))
                .put(SocketableFixtures.row("gunners", "weapon_mount", "HULL_MULT:4:6").put("unique", "TRUE")));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        SocketableDefinitions.clear();
    }

    private ShipSkillData shipWith(Socketable inFirst, Socketable inSecond) {
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
        Socketable chip = store.add(SocketableDefinitions.get("chip"), 1L);
        Socketable gunners = store.add(SocketableDefinitions.get("gunners"), 2L);

        assertTrue(chip.canSocketInto(firstSocket));
        assertFalse(gunners.canSocketInto(firstSocket));
        assertTrue(gunners.canSocketInto(SocketType.WEAPON_MOUNT));
        assertFalse(gunners.canSocketInto(SocketType.BRIDGE));
        assertFalse(chip.canSocketInto(SocketType.SUBROUTINE));
    }

    @Test
    void itemsThatNoLongerFitATreeSocketAreReturnedToStorageOnce() {
        Socketable chip = store.add(SocketableDefinitions.get("chip"), 1L);
        Socketable gunners = store.add(SocketableDefinitions.get("gunners"), 2L);
        ShipSkillData shipData = shipWith(chip, gunners);

        assertEquals(List.of(gunners), SocketTypeMigration.returnMisfits(ships, store));
        assertEquals(chip.id(), shipData.getSocketedItem(firstSocket.getId()));
        assertNull(shipData.getSocketedItem(secondSocket.getId()));
        assertEquals(List.of(chip, gunners), store.owned());
        assertEquals(List.of(), SocketTypeMigration.returnMisfits(ships, store));
    }

    @Test
    void itemsWhoseDefinitionIsMissingAreLeftInPlace() {
        Socketable chip = store.add(SocketableDefinitions.get("chip"), 1L);
        Socketable gunners = store.add(SocketableDefinitions.get("gunners"), 2L);
        ShipSkillData shipData = shipWith(chip, gunners);
        SocketableDefinitions.clear();

        assertEquals(List.of(), SocketTypeMigration.returnMisfits(ships, store));
        assertEquals(gunners.id(), shipData.getSocketedItem(secondSocket.getId()));
    }

    @Test
    void aTransposedItemThatChangedTypeLeavesItsTreeSocket() {
        Socketable chip = store.add(SocketableDefinitions.get("chip"), 1L);
        Socketable gunners = store.add(SocketableDefinitions.get("gunners"), 2L);
        ShipSkillData shipData = shipWith(chip, gunners);

        assertFalse(SocketCustody.unsocketIfMisfit(ships, chip));
        assertTrue(SocketCustody.unsocketIfMisfit(ships, gunners));
        assertNull(shipData.getSocketedItem(secondSocket.getId()));
        assertEquals(chip.id(), shipData.getSocketedItem(firstSocket.getId()));
    }
}
