package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import exiledsector.MockSector;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.wormhole.ColonyFreeAtlas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtlasAbilityTest {

    private MockSector mockSector;
    private final CampaignUIAPI campaignUI = mock(CampaignUIAPI.class);
    private CampaignFleetAPI playerFleet;
    private AtlasAbility ability;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        mockSector.global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        when(mockSector.sector.getCampaignUI()).thenReturn(campaignUI);
        playerFleet = mockSector.playerFleet();
        when(playerFleet.isPlayerFleet()).thenReturn(true);
        ability = new AtlasAbility();
        ability.init(ColonyFreeAtlas.ABILITY_ID, playerFleet);
        StarChartSettings.setNoColonyForTests(true);
    }

    @AfterEach
    void tearDown() {
        StarChartSettings.setNoColonyForTests(StarChartSettings.DEFAULT_NO_COLONY);
        mockSector.close();
    }

    @Test
    void pressingItOpensTheAtlasDialogOnThePlayerFleet() {
        assertTrue(ability.isUsable());

        ability.pressButton();

        verify(campaignUI).showInteractionDialog(any(AtlasAbilityDialog.class), eq(playerFleet));
    }

    @Test
    void itDoesNothingWhileADialogIsOpenOrTheSettingIsOff() {
        when(campaignUI.isShowingDialog()).thenReturn(true);
        assertFalse(ability.isUsable());
        when(campaignUI.isShowingDialog()).thenReturn(false);
        StarChartSettings.setNoColonyForTests(false);
        assertFalse(ability.isUsable());

        ability.pressButton();

        verify(campaignUI, never()).showInteractionDialog(any(), any());
    }
}
