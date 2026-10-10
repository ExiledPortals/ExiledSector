package exiledsector.socketables;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SocketableDisassemblyTest {

    private final SocketableStore store = new SocketableStore();
    private final Map<String, ShipSkillData> ships = new HashMap<>();
    private final CargoAPI cargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        SocketableFixtures.registerMilitary();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        SocketableDefinitions.clear();
    }

    @Test
    void partsScaleWithRarity() {
        assertEquals(3, SocketableRarity.COMMON.disassemblyParts());
        assertEquals(5, SocketableRarity.RARE.disassemblyParts());
        assertEquals(10, SocketableRarity.UNIQUE.disassemblyParts());
    }

    @Test
    void disassemblingRemovesTheItemAndAddsItsPartsToCargo() {
        Socketable socketable = store.add(SocketableDefinitions.get(SocketableFixtures.MILITARY), 7L);
        int expected = socketable.rarity().disassemblyParts();

        assertEquals(expected, SocketableDisassembly.disassemble(socketable, cargo, store, ships));

        assertNull(store.find(socketable.id()));
        verify(cargo).addCommodity(SocketableDisassembly.PARTS_COMMODITY_ID, expected);
    }

    @Test
    void anInstalledItemIsNeitherRemovedNorPaidOut() {
        SkillNode socket = new SkillNode("socket", new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build(),
                List.of(), 0f, 0f);
        SkillTree.register(socket);
        Socketable socketable = store.add(SocketableDefinitions.get(SocketableFixtures.MILITARY), 7L);
        ShipSkillData data = new ShipSkillData();
        data.allocate(socket, 3);
        data.socketItem("socket", socketable.id());
        ships.put("ship-a", data);

        assertEquals(0, SocketableDisassembly.disassemble(socketable, cargo, store, ships));

        assertNotNull(store.find(socketable.id()));
        verify(cargo, never()).addCommodity(anyString(), anyFloat());
    }

    @Test
    void anItemNoLongerInStorageIsNotPaidOutTwice() {
        Socketable socketable = store.add(SocketableDefinitions.get(SocketableFixtures.MILITARY), 7L);
        SocketableDisassembly.disassemble(socketable, cargo, store, ships);

        assertEquals(0, SocketableDisassembly.disassemble(socketable, cargo, store, ships));
    }
}
