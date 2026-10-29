package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CampaignMessagesTest {

    private MockSector mockSector;
    private CampaignUIAPI campaignUi;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        campaignUi = mock(CampaignUIAPI.class);
        when(mockSector.sector.getCampaignUI()).thenReturn(campaignUi);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
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
        when(mockSector.sector.getCampaignUI()).thenReturn(null);

        assertFalse(CampaignMessages.available());
        assertDoesNotThrow(() -> CampaignMessages.plain("text", Color.WHITE));
        assertDoesNotThrow(() -> CampaignMessages.highlighted("text", Color.WHITE, "a", "b", Color.RED, Color.RED));
    }

    @Test
    void messagesAreDroppedQuietlyWithoutASector() {
        mockSector.global.when(Global::getSector).thenReturn(null);

        assertFalse(CampaignMessages.available());
        assertDoesNotThrow(() -> CampaignMessages.plain("text", Color.WHITE));
    }

    @Test
    void theCampaignUiIsAvailableWhileASectorIsLoaded() {
        assertTrue(CampaignMessages.available());
    }
}
