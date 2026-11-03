package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.MockSector;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.wormhole.ColonyFreeAtlas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class AtlasAbilityTest {

    private MockSector mockSector;
    private final CampaignUIAPI campaignUI = mock(CampaignUIAPI.class);
    private final SettingsAPI settings = mock(SettingsAPI.class);
    private CampaignFleetAPI playerFleet;
    private AtlasAbility ability;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        mockSector.global.when(Global::getSettings).thenReturn(settings);
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

    @Test
    void theAtlasOpensInsideTheAbilityDialogOnItsFirstFrameNotDuringInit() {
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, withSettings().extraInterfaces(UIPanelAPI.class));
        UIPanelAPI dialogPanel = (UIPanelAPI) dialog;
        when(dialog.getOptionPanel()).thenReturn(mock(OptionPanelAPI.class));
        when(dialogPanel.addComponent(any())).thenReturn(mock(PositionAPI.class));
        when(settings.createCustom(anyFloat(), anyFloat(), any())).thenReturn(mock(CustomPanelAPI.class));
        AtlasAbilityDialog plugin = new AtlasAbilityDialog();

        plugin.init(dialog);

        verify(settings, never()).createCustom(anyFloat(), anyFloat(), any());

        plugin.advance(0f);

        verify(dialogPanel).addComponent(any());
    }
}
