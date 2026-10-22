package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.input.InputEventAPI;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NpcTreeInspectInputTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private CampaignUIAPI campaignUI;

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        sector = mock(SectorAPI.class);
        campaignUI = mock(CampaignUIAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignUI);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getScreenWidth()).thenReturn(1920f);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
    }

    private static CampaignFleetAPI fleet(VisibilityLevel visibility) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getVisibilityLevelToPlayerFleet()).thenReturn(visibility);
        return fleet;
    }

    private static InputEventAPI keyDown(int key) {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.isKeyDownEvent()).thenReturn(true);
        when(event.getEventValue()).thenReturn(key);
        return event;
    }

    private InteractionDialogAPI openDialogWith(SectorEntityToken target) {
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        when(dialog.getInteractionTarget()).thenReturn(target);
        when(campaignUI.getCurrentInteractionDialog()).thenReturn(dialog);
        return dialog;
    }

    @Test
    void theInspectKeyInAFleetEncounterOpensTheInspectWindowOverTheDialog() {
        InteractionDialogAPI dialog = openDialogWith(fleet(VisibilityLevel.COMPOSITION_DETAILS));

        assertTrue(NpcTreeInspectInput.tryOpen());

        verify(dialog).showCustomVisualDialog(anyFloat(), anyFloat(), any(NpcFleetInspectDialog.class));
    }

    @Test
    void dialogsWithoutAFleetTargetAreIgnored() {
        InteractionDialogAPI dialog = openDialogWith(mock(SectorEntityToken.class));

        assertFalse(NpcTreeInspectInput.tryOpen());

        verify(dialog, never()).showCustomVisualDialog(anyFloat(), anyFloat(), any());
    }

    @Test
    void theKeyDoesNothingInsideTheInspectWindowItself() {
        InteractionDialogAPI dialog = openDialogWith(fleet(VisibilityLevel.COMPOSITION_DETAILS));
        when(dialog.getPlugin()).thenReturn(new NpcFleetInspectPlugin(mock(CampaignFleetAPI.class)));

        assertFalse(NpcTreeInspectInput.tryOpen());
    }

    @Test
    void onTheMapTheKeyInspectsTheHoveredFleet() {
        CampaignFleetAPI hovered = fleet(VisibilityLevel.COMPOSITION_DETAILS);
        when(sector.getMousedOverEntity()).thenReturn(hovered);
        when(campaignUI.showInteractionDialog(any(NpcFleetInspectPlugin.class), any())).thenReturn(true);

        assertTrue(NpcTreeInspectInput.tryOpen());

        verify(campaignUI).showInteractionDialog(any(NpcFleetInspectPlugin.class), Mockito.eq(hovered));
    }

    @Test
    void theKeyIsIgnoredWhileACoreTabOrMenuIsOpen() {
        CampaignFleetAPI hovered = fleet(VisibilityLevel.COMPOSITION_DETAILS);
        when(sector.getMousedOverEntity()).thenReturn(hovered);
        when(campaignUI.getCurrentCoreTab()).thenReturn(CoreUITabId.FLEET);

        assertFalse(NpcTreeInspectInput.tryOpen());
        verify(campaignUI, never()).showInteractionDialog(any(), any());
    }

    @Test
    void onlyFleetsSeenInEnoughDetailCanBeInspected() {
        CampaignFleetAPI player = fleet(VisibilityLevel.COMPOSITION_DETAILS);
        when(player.isPlayerFleet()).thenReturn(true);

        assertNull(NpcTreeInspectInput.inspectableFleet(fleet(VisibilityLevel.SENSOR_CONTACT)));
        assertNull(NpcTreeInspectInput.inspectableFleet(fleet(VisibilityLevel.NONE)));
        assertNull(NpcTreeInspectInput.inspectableFleet(player));
        assertNull(NpcTreeInspectInput.inspectableFleet(mock(SectorEntityToken.class)));
        assertNull(NpcTreeInspectInput.inspectableFleet(null));
        CampaignFleetAPI detailed = fleet(VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS);
        assertSame(detailed, NpcTreeInspectInput.inspectableFleet(detailed));
    }

    @Test
    void theInspectedSideIsTheTargetsWholeBattleSide() {
        CampaignFleetAPI target = fleet(VisibilityLevel.COMPOSITION_DETAILS);
        CampaignFleetAPI ally = fleet(VisibilityLevel.COMPOSITION_DETAILS);
        assertEquals(List.of(target), NpcTreeInspectInput.targetSide(target));

        BattleAPI battle = mock(BattleAPI.class);
        when(battle.getSideFor(target)).thenReturn(List.of(target, ally));
        when(target.getBattle()).thenReturn(battle);

        assertEquals(List.of(target, ally), NpcTreeInspectInput.targetSide(target));
    }

    @Test
    void theConfiguredKeyIsConsumedOnlyWhenTheWindowOpens() {
        openDialogWith(fleet(VisibilityLevel.COMPOSITION_DETAILS));
        InputEventAPI inspect = keyDown(NpcInspectConfig.DEFAULT_KEY);
        InputEventAPI other = keyDown(NpcInspectConfig.DEFAULT_KEY + 1);

        new NpcTreeInspectInput().processCampaignInputPreCore(List.of(inspect, other));

        verify(inspect).consume();
        verify(other, never()).consume();
    }

    @Test
    void aReboundKeyIsHonoured() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", NpcInspectConfig.KEYBIND_FIELD_ID)).thenReturn(42);
        openDialogWith(fleet(VisibilityLevel.COMPOSITION_DETAILS));
        InputEventAPI rebound = keyDown(42);
        InputEventAPI defaultKey = keyDown(NpcInspectConfig.DEFAULT_KEY);

        new NpcTreeInspectInput().processCampaignInputPreCore(List.of(defaultKey, rebound));

        verify(rebound).consume();
        verify(defaultKey, never()).consume();
    }
}
