package exiledsector.items;

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

class ItemDisassemblyTest {

    private final ItemStore store = new ItemStore();
    private final Map<String, ShipSkillData> ships = new HashMap<>();
    private final CargoAPI cargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        ItemFixtures.registerMilitary();
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        ItemDefinitions.clear();
    }

    @Test
    void partsScaleWithRarity() {
        assertEquals(3, ItemRarity.COMMON.disassemblyParts());
        assertEquals(5, ItemRarity.RARE.disassemblyParts());
        assertEquals(10, ItemRarity.UNIQUE.disassemblyParts());
    }

    @Test
    void disassemblingRemovesTheItemAndAddsItsPartsToCargo() {
        Item item = store.add(ItemDefinitions.get(ItemFixtures.MILITARY), 7L);
        int expected = item.rarity().disassemblyParts();

        assertEquals(expected, ItemDisassembly.disassemble(item, CraftingMaterials.cargoOnly(cargo), store, ships));

        assertNull(store.find(item.id()));
        verify(cargo).addCommodity(ItemDisassembly.PARTS_COMMODITY_ID, expected);
    }

    @Test
    void anInstalledItemIsNeitherRemovedNorPaidOut() {
        SkillNode socket = new SkillNode("socket", new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build(),
                List.of(), 0f, 0f);
        SkillTree.register(socket);
        Item item = store.add(ItemDefinitions.get(ItemFixtures.MILITARY), 7L);
        ShipSkillData data = new ShipSkillData();
        data.allocate(socket, 3);
        data.socketItem("socket", item.id());
        ships.put("ship-a", data);

        assertEquals(0, ItemDisassembly.disassemble(item, CraftingMaterials.cargoOnly(cargo), store, ships));

        assertNotNull(store.find(item.id()));
        verify(cargo, never()).addCommodity(anyString(), anyFloat());
    }

    @Test
    void anItemNoLongerInStorageIsNotPaidOutTwice() {
        Item item = store.add(ItemDefinitions.get(ItemFixtures.MILITARY), 7L);
        ItemDisassembly.disassemble(item, CraftingMaterials.cargoOnly(cargo), store, ships);

        assertEquals(0, ItemDisassembly.disassemble(item, CraftingMaterials.cargoOnly(cargo), store, ships));
    }
}
