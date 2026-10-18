package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LocaleChain;
import exiledsector.i18n.RealCatalogue;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.StarChartFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartScreenTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        cargo = mock(CargoAPI.class);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(playerFleet.getCargo()).thenReturn(cargo);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        StarChartFixtures.clear();
    }

    private CargoStackAPI chartStack(int tier, float size) {
        ItemData chart = ItemData.rolledAtTier(ItemDefinitions.get("star_chart_single"), 40L + tier, tier);
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(new SpecialItemData(ItemData.STAR_CHART_ITEM_ID, chart.encode()));
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void openingMovesChartsFromCargoIntoStorageAndShowsThemByTier() {
        CargoStackAPI threes = chartStack(3, 2f);
        CargoStackAPI eight = chartStack(8, 1f);
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>(List.of(threes, eight)));
        StarChartBrowser browser = new StarChartBrowser();

        int moved = StarChartScreen.prepare(sector, browser);

        assertEquals(3, moved);
        verify(cargo).removeStack(threes);
        verify(cargo).removeStack(eight);
        assertEquals(3, ItemStore.starChartsIfCreated().owned().size());
        assertEquals(2, browser.count(3));
        assertEquals(1, browser.count(8));
        assertEquals(3, browser.selectedTier());
        assertEquals("3 Star Charts moved from your cargo into storage.", StarChartScreen.movedNotice(moved));
        assertEquals("1 Star Chart moved from your cargo into storage.", StarChartScreen.movedNotice(1));
        assertNull(StarChartScreen.movedNotice(0));
    }

    @Test
    void openingWithNoChartsAnywhereCreatesNoStorage() {
        when(cargo.getStacksCopy()).thenReturn(new ArrayList<>());
        StarChartBrowser browser = new StarChartBrowser();

        assertEquals(0, StarChartScreen.prepare(sector, browser));

        assertNull(ItemStore.starChartsIfCreated());
        assertEquals(0, browser.storedCount());
        verify(cargo, never()).removeStack(Mockito.any());
    }

    @Test
    void theScreenSplitsIntoAStorageColumnAndADetailColumn() {
        StarChartScreen.Layout layout = StarChartScreen.Layout.within(1200f, 800f);

        assertEquals(StarChartScreen.DETAIL_WIDTH, layout.detailWidth());
        assertEquals(1200f, layout.detailLeft() + layout.detailWidth(), 0.01f);
        assertEquals(layout.storageWidth() + StarChartScreen.PANEL_GAP, layout.detailLeft(), 0.01f);
        assertEquals(800f, layout.panelHeight());
        assertTrue(StarChartScreen.width(1920f) <= StarChartScreen.MAX_WIDTH);
        assertEquals(1280f - StarChartScreen.SCREEN_MARGIN * 2f, StarChartScreen.width(1280f));
        assertTrue(StarChartScreen.height(768f) < 768f);
    }

    @Test
    void theMovedNoticeUsesTheGameTextLanguageBecauseVanillaDrawsIt() {
        I18n.install(RealCatalogue.of(LocaleChain.SIMPLIFIED_CHINESE), RealCatalogue.english());

        assertEquals("2 Star Charts moved from your cargo into storage.", StarChartScreen.movedNotice(2));
    }

    private CampaignUIAPI campaignUi(InteractionDialogAPI currentDialog) {
        CampaignUIAPI campaignUi = mock(CampaignUIAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignUi);
        when(campaignUi.getCurrentInteractionDialog()).thenReturn(currentDialog);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getScreenWidth()).thenReturn(1920f);
        when(settings.getScreenHeight()).thenReturn(1080f);
        globalMock.when(Global::getSettings).thenReturn(settings);
        return campaignUi;
    }

    @Test
    void openingFromAnOpenDialogShowsTheScreenInsideItAndLeavesThatDialogOpenAfterwards() {
        InteractionDialogAPI currentDialog = mock(InteractionDialogAPI.class);
        CampaignUIAPI campaignUi = campaignUi(currentDialog);

        assertTrue(StarChartScreen.open());

        ArgumentCaptor<CustomVisualDialogDelegate> shown = ArgumentCaptor.forClass(CustomVisualDialogDelegate.class);
        verify(currentDialog).showCustomVisualDialog(eq(StarChartScreen.width(1920f)), eq(StarChartScreen.height(1080f)), shown.capture());
        verify(campaignUi, never()).showInteractionDialog(any(), any());
        StarChartScreen screen = assertInstanceOf(StarChartScreen.class, shown.getValue());
        screen.reportDismissed(0);
        verify(currentDialog, never()).dismiss();
    }

    @Test
    void openingWithoutADialogStartsTheStarChartDialogOnThePlayerFleet() {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        CampaignUIAPI campaignUi = campaignUi(null);
        when(campaignUi.showInteractionDialog(any(StarChartDialogPlugin.class), eq(playerFleet))).thenReturn(true, false);

        assertTrue(StarChartScreen.open());
        assertFalse(StarChartScreen.open());

        verify(campaignUi, times(2)).showInteractionDialog(any(StarChartDialogPlugin.class), eq(playerFleet));
    }

    @Test
    void openingOutsideTheCampaignDoesNothing() {
        assertFalse(StarChartScreen.open());

        globalMock.when(Global::getSector).thenReturn(null);
        assertFalse(StarChartScreen.open());
    }

    @Test
    void aSecondDismissalReportDoesNothing() {
        AtomicInteger dismissals = new AtomicInteger();
        StarChartScreen screen = new StarChartScreen(dismissals::incrementAndGet);
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        StarChartScreen dialogScreen = StarChartScreen.forDialog(dialog);

        screen.reportDismissed(0);
        screen.reportDismissed(0);
        dialogScreen.reportDismissed(0);
        dialogScreen.reportDismissed(0);

        assertEquals(1, dismissals.get());
        verify(dialog, times(1)).dismiss();
    }

    private static InputEventAPI escapeKey(boolean pressed) {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.isKeyboardEvent()).thenReturn(true);
        when(event.getEventValue()).thenReturn(Keyboard.KEY_ESCAPE);
        when(event.isKeyDownEvent()).thenReturn(pressed);
        when(event.isKeyUpEvent()).thenReturn(!pressed);
        return event;
    }

    @Test
    void escapeClosesOnReleaseLikeTheHullUpgradesScreenAndIsSwallowedWhileFading() {
        StarChartScreen screen = new StarChartScreen(null);
        InputEventAPI press = escapeKey(true);
        InputEventAPI release = escapeKey(false);
        InputEventAPI releaseWhileFading = escapeKey(false);

        screen.getCustomPanelPlugin().processInput(List.of(press));
        assertFalse(screen.isClosing());
        screen.getCustomPanelPlugin().processInput(List.of(release));
        assertTrue(screen.isClosing());
        screen.getCustomPanelPlugin().processInput(List.of(releaseWhileFading));

        verify(press).consume();
        verify(release).consume();
        verify(releaseWhileFading).consume();
    }

    @Test
    void anEscapeAlreadyHandledElsewhereIsIgnored() {
        StarChartScreen screen = new StarChartScreen(null);
        InputEventAPI release = escapeKey(false);
        when(release.isConsumed()).thenReturn(true);

        screen.getCustomPanelPlugin().processInput(List.of(release));

        assertFalse(screen.isClosing());
        verify(release, never()).consume();
    }
}
