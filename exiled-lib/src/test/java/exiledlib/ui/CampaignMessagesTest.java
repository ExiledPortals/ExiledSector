package exiledlib.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignMessagesTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private CampaignUIAPI campaignUi;

    @BeforeEach
    void setUp() {
        sector = mock(SectorAPI.class);
        campaignUi = mock(CampaignUIAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignUi);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void aPlainMessageKeepsItsPercentSignsBecauseTheEngineShowsItVerbatim() {
        CampaignMessages.plain("Hull +5% stronger", Color.WHITE);

        verify(campaignUi).addMessage("Hull +5% stronger", Color.WHITE);
    }

    @Test
    void aHighlightedMessageEscapesPercentSignsBecauseTheEngineFormatsIt() {
        CampaignMessages.highlighted("Relic +5% aboard Fleet", Color.WHITE, "Relic", "Fleet", Color.RED, Color.YELLOW);

        verify(campaignUi).addMessage("Relic +5%% aboard Fleet", Color.WHITE, "Relic", "Fleet", Color.RED, Color.YELLOW);
    }

    @Test
    void messagesAreDroppedQuietlyWithoutACampaignUi() {
        when(sector.getCampaignUI()).thenReturn(null);

        assertFalse(CampaignMessages.available());
        assertDoesNotThrow(() -> CampaignMessages.plain("text", Color.WHITE));
        assertDoesNotThrow(() -> CampaignMessages.highlighted("text", Color.WHITE, "a", "b", Color.RED, Color.RED));
    }

    @Test
    void messagesAreDroppedQuietlyWithoutASector() {
        globalMock.when(Global::getSector).thenReturn(null);

        assertFalse(CampaignMessages.available());
        assertDoesNotThrow(() -> CampaignMessages.plain("text", Color.WHITE));
    }

    @Test
    void theCampaignUiIsAvailableWhileASectorIsLoaded() {
        assertTrue(CampaignMessages.available());
    }
}
