package exiledsector.charts.objective;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.MockSector;
import exiledsector.charts.MapMemory;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.items.WormholeTechItems;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayUpgradeRewardsTest {

    private MockSector mockSector;
    private MockedStatic<VaultsOfKnowledgeCompat> vaults;
    private final MemoryAPI sectorMemory = MapMemory.create();
    private CargoAPI cargo;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        cargo = mockSector.cargo();
        vaults = Mockito.mockStatic(VaultsOfKnowledgeCompat.class);
        vaults.when(VaultsOfKnowledgeCompat::isModEnabled).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        vaults.close();
        mockSector.close();
    }

    private static SpecialItemData item(String itemId) {
        return argThat(data -> data != null && itemId.equals(data.getId()) && data.getData() == null);
    }

    @Test
    void aTierFourRunGrantsNothing() {
        assertEquals(List.of(), GatewayUpgradeRewards.completed(mockSector.sector, 4));

        verify(cargo, never()).addSpecial(any(), anyFloat());
        verify(mockSector.sector, never()).addTransientScript(any());
    }

    @Test
    void aTierFiveRunGrantsTheAdvancedUpgradeOnceAndQueuesThePopup() {
        assertEquals(List.of(WormholeTechItems.UPGRADE_ADVANCED), GatewayUpgradeRewards.completed(mockSector.sector, 5));
        assertEquals(List.of(), GatewayUpgradeRewards.completed(mockSector.sector, 7));

        verify(cargo, times(1)).addSpecial(item(WormholeTechItems.UPGRADE_ADVANCED), eq(1f));
        assertEquals(List.of(WormholeTechItems.UPGRADE_ADVANCED), GatewayUpgradeRewards.pending(mockSector.sector));
        verify(mockSector.sector).addTransientScript(argThat((EveryFrameScript script) -> script instanceof GatewayUpgradeNotice));
    }

    @Test
    void aTierTenRunOnAFreshSaveGrantsBoth() {
        assertEquals(List.of(WormholeTechItems.UPGRADE_ADVANCED, WormholeTechItems.UPGRADE_PERFECTED),
                GatewayUpgradeRewards.completed(mockSector.sector, 10));

        verify(cargo).addSpecial(item(WormholeTechItems.UPGRADE_ADVANCED), eq(1f));
        verify(cargo).addSpecial(item(WormholeTechItems.UPGRADE_PERFECTED), eq(1f));
        assertTrue(sectorMemory.getBoolean(GatewayUpgradeRewards.ADVANCED_GRANTED));
        assertTrue(sectorMemory.getBoolean(GatewayUpgradeRewards.PERFECTED_GRANTED));
        assertEquals(List.of(WormholeTechItems.UPGRADE_ADVANCED, WormholeTechItems.UPGRADE_PERFECTED),
                new GatewayUpgradeDialog(GatewayUpgradeRewards.pending(mockSector.sector)).itemIds());
    }

    @Test
    void nothingDropsWithoutVaultsOfKnowledge() {
        vaults.when(VaultsOfKnowledgeCompat::isModEnabled).thenReturn(false);

        assertEquals(List.of(), GatewayUpgradeRewards.completed(mockSector.sector, 16));
        verify(cargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void loadingASaveThatAlreadyClearedHighTiersGrantsTheMissingUpgrades() {
        AtlasState state = new AtlasState(1L);
        state.clearTier(3);
        state.clearTier(11);
        mockSector.persistentData.put(AtlasState.PERSISTENT_KEY, state);
        sectorMemory.set(GatewayUpgradeRewards.ADVANCED_GRANTED, true);

        GatewayUpgradeRewards.onGameLoad(mockSector.sector);

        verify(cargo, never()).addSpecial(item(WormholeTechItems.UPGRADE_ADVANCED), anyFloat());
        verify(cargo).addSpecial(item(WormholeTechItems.UPGRADE_PERFECTED), eq(1f));
        assertEquals(List.of(WormholeTechItems.UPGRADE_PERFECTED), GatewayUpgradeRewards.pending(mockSector.sector));
    }

    @Test
    void aPopupQueuedBeforeASaveIsShownAgainAfterLoading() {
        sectorMemory.set(GatewayUpgradeRewards.PENDING_KEY, WormholeTechItems.UPGRADE_ADVANCED);

        GatewayUpgradeRewards.onGameLoad(mockSector.sector);

        verify(mockSector.sector).addTransientScript(argThat((EveryFrameScript script) -> script instanceof GatewayUpgradeNotice));
        GatewayUpgradeRewards.clearPending(mockSector.sector);
        assertEquals(List.of(), GatewayUpgradeRewards.pending(mockSector.sector));
    }
}
