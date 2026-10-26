package exiledsector.charts;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.anomaly.SpatialBreach;
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

    @Test
    void leavesEveryOtherEntityToTheGame() {
        SectorEntityToken breachCore = mock(SectorEntityToken.class);
        when(breachCore.hasTag(SpatialBreach.CORE_TAG)).thenReturn(true);

        assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(mock(SectorEntityToken.class)));
        assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(breachCore));
        assertNull(new ChartCampaignPlugin().pickInteractionDialogPlugin(null));
    }
}
