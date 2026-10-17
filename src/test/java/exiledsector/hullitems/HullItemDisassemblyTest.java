package exiledsector.hullitems;

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

class HullItemDisassemblyTest {

    private final HullItemStore store = new HullItemStore();
    private final Map<String, ShipSkillData> ships = new HashMap<>();
    private final CargoAPI cargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        HullItemFixtures.registerMilitary();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        HullItemDefinitions.clear();
    }

    @Test
    void partsScaleWithRarity() {
        assertEquals(3, HullItemRarity.COMMON.disassemblyParts());
        assertEquals(5, HullItemRarity.RARE.disassemblyParts());
        assertEquals(10, HullItemRarity.UNIQUE.disassemblyParts());
    }

    @Test
    void disassemblingRemovesTheItemAndAddsItsPartsToCargo() {
        HullItem hullItem = store.add(HullItemDefinitions.get(HullItemFixtures.MILITARY), 7L);
        int expected = hullItem.rarity().disassemblyParts();

        assertEquals(expected, HullItemDisassembly.disassemble(hullItem, CraftingMaterials.cargoOnly(cargo), store, ships));

        assertNull(store.find(hullItem.id()));
        verify(cargo).addCommodity(HullItemDisassembly.PARTS_COMMODITY_ID, expected);
    }

    @Test
    void anInstalledItemIsNeitherRemovedNorPaidOut() {
        SkillNode socket = new SkillNode("socket", new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build(),
                List.of(), 0f, 0f);
        SkillTree.register(socket);
        HullItem hullItem = store.add(HullItemDefinitions.get(HullItemFixtures.MILITARY), 7L);
        ShipSkillData data = new ShipSkillData();
        data.allocate(socket, 3);
        data.socketItem("socket", hullItem.id());
        ships.put("ship-a", data);

        assertEquals(0, HullItemDisassembly.disassemble(hullItem, CraftingMaterials.cargoOnly(cargo), store, ships));

        assertNotNull(store.find(hullItem.id()));
        verify(cargo, never()).addCommodity(anyString(), anyFloat());
    }

    @Test
    void anItemNoLongerInStorageIsNotPaidOutTwice() {
        HullItem hullItem = store.add(HullItemDefinitions.get(HullItemFixtures.MILITARY), 7L);
        HullItemDisassembly.disassemble(hullItem, CraftingMaterials.cargoOnly(cargo), store, ships);

        assertEquals(0, HullItemDisassembly.disassemble(hullItem, CraftingMaterials.cargoOnly(cargo), store, ships));
    }
}
