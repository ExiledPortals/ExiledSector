package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.WormholeAnchorDefinitions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OmegaAnchorAcquisitionTest {

    private final SectorAPI sector = mock(SectorAPI.class);
    private final MemoryAPI playerMemory = mock(MemoryAPI.class);
    private final CargoAPI emptyCargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
        when(emptyCargo.getStacksCopy()).thenReturn(List.of());
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchor("wormhole_anchor_omega", "omega", "omega_wormhole_1"))
                .put(anchor("wormhole_anchor_hegemony", "hegemony", "hegemony_2")));
    }

    @AfterEach
    void tearDown() {
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
    }

    @Test
    void holdingAnOmegaAnchorOpensTheOmegaGate() {
        OmegaAnchorAcquisition.recordIfHeld(sector, emptyCargo, List.of(item("wormhole_anchor_hegemony"), item("wormhole_anchor_omega")));

        verify(playerMemory).set(FactionEncounterGate.OMEGA.flagKey(), true);
    }

    @Test
    void otherItemsLeaveTheGateShut() {
        OmegaAnchorAcquisition.recordIfHeld(sector, emptyCargo, List.of(item("wormhole_anchor_hegemony")));
        OmegaAnchorAcquisition.recordIfHeld(sector, null, List.of());

        verify(playerMemory, never()).set(anyString(), any());
    }

    private static Item item(String definitionId) {
        Item item = mock(Item.class);
        when(item.definitionId()).thenReturn(definitionId);
        return item;
    }

    private static JSONObject anchor(String id, String volume, String exitNode) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id);
    }
}
