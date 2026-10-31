package exiledsector.charts.run;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.LiveAnomalyRuns;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.relay.RelayDialog;
import exiledsector.charts.anomaly.relay.RelayKind;
import exiledsector.charts.anomaly.relay.RelayNetwork;
import exiledsector.charts.wormhole.ChartWormholeDialog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartCampaignPluginTest {

    private static SectorEntityToken wormhole(String roleTag) {
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.hasTag(ChartIds.ENTITY_TAG)).thenReturn(true);
        when(entity.hasTag(roleTag)).thenReturn(true);
        return entity;
    }

    @Test
    void isTransientSoItIsNeverSaved() {
        assertTrue(new ChartCampaignPlugin().isTransient());
        assertEquals(ChartCampaignPlugin.ID, new ChartCampaignPlugin().getId());
    }

    @Test
    void picksOurDialogForBothWormholes() {
        for (String roleTag : new String[]{ChartIds.OUTER_WORMHOLE_TAG, ChartIds.INNER_WORMHOLE_TAG}) {
            PluginPick<InteractionDialogPlugin> pick = new ChartCampaignPlugin().pickInteractionDialogPlugin(wormhole(roleTag));

            assertInstanceOf(ChartWormholeDialog.class, pick.plugin);
            assertEquals(CampaignPlugin.PickPriority.MOD_SPECIFIC, pick.priority);
        }
    }

    private static SectorEntityToken relay() {
        SectorEntityToken relay = mock(SectorEntityToken.class);
        MemoryAPI memory = mock(MemoryAPI.class);
        when(relay.hasTag(RelayNetwork.RELAY_TAG)).thenReturn(true);
        when(relay.getMemoryWithoutUpdate()).thenReturn(memory);
        when(memory.getString(RelayNetwork.KIND_MEMORY_KEY)).thenReturn(RelayKind.NAV_BUOY.key());
        return relay;
    }

    @Test
    void picksOurDialogForRelayNetworkRelaysOverVanillasObjectiveDialog() {
        try (MockSector ignored = new MockSector()) {
            SectorEntityToken relay = LiveAnomalyRuns.placedIn(relay(), LiveAnomalyRuns.systemWith(RelayNetwork.ANOMALY_ID));

            PluginPick<InteractionDialogPlugin> pick = new ChartCampaignPlugin().pickInteractionDialogPlugin(relay);

            assertInstanceOf(RelayDialog.class, pick.plugin);
            assertEquals(CampaignPlugin.PickPriority.MOD_SPECIFIC, pick.priority);
        }
    }

    @Test
    void leavesEveryOtherEntityToTheGame() {
        try (MockSector ignored = new MockSector()) {
            SectorEntityToken outsideAnyRun = relay();
            StarSystemAPI system = LiveAnomalyRuns.systemWith(SpatialBreach.ANOMALY_ID);
            SectorEntityToken breachCore = LiveAnomalyRuns.placedIn(mock(SectorEntityToken.class), system);
            when(breachCore.hasTag(SpatialBreach.CORE_TAG)).thenReturn(true);
            SectorEntityToken relayWithoutANetwork = LiveAnomalyRuns.placedIn(relay(), system);

            assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(mock(SectorEntityToken.class)));
            assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(outsideAnyRun));
            assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(breachCore));
            assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(relayWithoutANetwork));
            assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(null));
        }
    }
}
