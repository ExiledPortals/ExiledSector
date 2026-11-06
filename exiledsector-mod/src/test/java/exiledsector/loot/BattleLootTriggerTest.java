package exiledsector.loot;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import exiledsector.MockSector;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BattleLootTriggerTest {

    private static final String BRIDGE_ID = "military_bridge";

    private MockSector mockSector;
    private CargoStackAPI lootedBridge;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", BRIDGE_ID).put("name", "Military-grade Bridge")
                .put("icon", "graphics/icons/cargo/chip1.png").put("rarity", "20").put("prefixes", "FLUX_CAPACITY_MULT:4:6")
                .put("suffixes", "ARMOR_PERCENT:6:9").put("section", "bridge")));
        lootedBridge = mock(CargoStackAPI.class);
        when(lootedBridge.getSpecialDataIfSpecial()).thenReturn(new ItemData(BRIDGE_ID, 5L).toSpecialItem());
        when(lootedBridge.getSize()).thenReturn(1f);
        CargoAPI playerCargo = mockSector.cargo();
        when(playerCargo.getStacksCopy()).thenReturn(List.of(lootedBridge));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        mockSector.close();
    }

    private static BattleAPI battle(boolean playerInvolved) {
        BattleAPI battle = mock(BattleAPI.class);
        when(battle.isPlayerInvolved()).thenReturn(playerInvolved);
        return battle;
    }

    @Test
    void lootTakenAfterThePlayersBattleMovesFromCargoIntoStorage() {
        new BattleLootTrigger().reportBattleFinished(null, battle(true));

        assertEquals(List.of(5L), ItemStore.get().owned().stream().map(Item::seed).toList());
        verify(mockSector.cargo()).removeStack(lootedBridge);
    }

    @Test
    void aBattleWithoutThePlayerLeavesTheCargoAlone() {
        new BattleLootTrigger().reportBattleFinished(null, battle(false));

        assertTrue(ItemStore.get().owned().isEmpty());
        verify(mockSector.cargo(), never()).removeStack(lootedBridge);
    }
}
