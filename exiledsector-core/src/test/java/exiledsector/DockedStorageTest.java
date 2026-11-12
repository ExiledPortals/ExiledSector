package exiledsector;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DockedStorageTest {

    private MockSector mockSector;
    private MockedStatic<Misc> misc;
    private final MarketAPI market = mock(MarketAPI.class);
    private final CargoAPI storageCargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        misc = Mockito.mockStatic(Misc.class);
        misc.when(() -> Misc.getStorageCargo(market)).thenReturn(storageCargo);
    }

    @AfterEach
    void tearDown() {
        misc.close();
        mockSector.close();
    }

    private void dockAt(MarketAPI dockedMarket) {
        CampaignUIAPI campaignUI = mock(CampaignUIAPI.class);
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(mockSector.sector.getCampaignUI()).thenReturn(campaignUI);
        when(campaignUI.getCurrentInteractionDialog()).thenReturn(dialog);
        when(dialog.getInteractionTarget()).thenReturn(entity);
        when(entity.getMarket()).thenReturn(dockedMarket);
    }

    @Test
    void theStorageOfTheDockedMarketCountsOnlyWithAccess() {
        assertNull(DockedStorage.cargoOrNull());
        dockAt(market);
        assertNull(DockedStorage.cargoOrNull());
        misc.when(() -> Misc.playerHasStorageAccess(market)).thenReturn(true);
        assertEquals(storageCargo, DockedStorage.cargoOrNull());
    }

    @Test
    void thePlayerCargoComesFirstAndStorageOnlyWhileDocked() {
        CargoAPI playerCargo = mockSector.cargo();
        assertEquals(List.of(playerCargo), DockedStorage.withPlayerCargo(playerCargo));
        dockAt(market);
        misc.when(() -> Misc.playerHasStorageAccess(market)).thenReturn(true);
        assertEquals(List.of(playerCargo, storageCargo), DockedStorage.withPlayerCargo(playerCargo));
        assertEquals(List.of(storageCargo), DockedStorage.withPlayerCargo(null));
    }
}
