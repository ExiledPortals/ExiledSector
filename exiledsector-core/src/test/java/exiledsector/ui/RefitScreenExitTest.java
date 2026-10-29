package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefitScreenExitTest {

    private static final String MESSAGE = "Wormhole opened.";

    private MockSector mockSector;
    private CampaignUIAPI campaignUi;
    private final AtomicBoolean panelGone = new AtomicBoolean();
    private final AtomicInteger coreDismissals = new AtomicInteger();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        campaignUi = mock(CampaignUIAPI.class);
        when(mockSector.sector.getCampaignUI()).thenReturn(campaignUi);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getColor(anyString())).thenReturn(Color.GREEN);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private RefitScreenExit exit(InteractionDialogAPI hostDialog, boolean dismissWorks) {
        return new RefitScreenExit(MESSAGE, hostDialog, panelGone::get, ui -> {
            coreDismissals.incrementAndGet();
            return dismissWorks;
        });
    }

    @Test
    void theRefitScreenAndTheMarketDialogItSitsInCloseInTurnThenTheMessageShows() {
        InteractionDialogAPI market = mock(InteractionDialogAPI.class);
        when(campaignUi.getCurrentInteractionDialog()).thenReturn(market);
        when(campaignUi.getCurrentCoreTab()).thenReturn(CoreUITabId.REFIT);
        RefitScreenExit exit = exit(market, true);

        exit.advance(0f);
        assertEquals(RefitScreenExit.Step.AWAIT_PANEL_REMOVAL, exit.step());
        assertEquals(0, coreDismissals.get(), "the skill tree panel goes first");

        panelGone.set(true);
        exit.advance(0f);
        assertEquals(RefitScreenExit.Step.AWAIT_CORE_GONE, exit.step());
        assertEquals(1, coreDismissals.get());
        exit.advance(0f);
        verify(market, never()).dismiss();

        when(campaignUi.getCurrentCoreTab()).thenReturn(null);
        exit.advance(0f);

        assertTrue(exit.isDone());
        assertTrue(exit.runWhilePaused());
        verify(market).dismiss();
        verify(campaignUi).addMessage(eq(MESSAGE), any(Color.class));
        exit.advance(0f);
        verify(campaignUi, times(1)).addMessage(eq(MESSAGE), any(Color.class));
        assertEquals(1, coreDismissals.get());
    }

    @Test
    void withoutAHostDialogOnlyTheRefitScreenCloses() {
        panelGone.set(true);
        when(campaignUi.getCurrentCoreTab()).thenReturn(CoreUITabId.REFIT, (CoreUITabId) null);
        RefitScreenExit exit = exit(null, true);

        exit.advance(0f);
        exit.advance(0f);

        assertTrue(exit.isDone());
        assertEquals(1, coreDismissals.get());
        verify(campaignUi).addMessage(eq(MESSAGE), any(Color.class));
    }

    @Test
    void aDialogThatOpenedAfterwardsIsNeverDismissed() {
        panelGone.set(true);
        InteractionDialogAPI market = mock(InteractionDialogAPI.class);
        InteractionDialogAPI later = mock(InteractionDialogAPI.class);
        when(campaignUi.getCurrentInteractionDialog()).thenReturn(later);
        RefitScreenExit exit = exit(market, true);

        exit.advance(0f);

        assertTrue(exit.isDone());
        assertEquals(0, coreDismissals.get(), "no core screen was open");
        verify(later, never()).dismiss();
        verify(market, never()).dismiss();
    }

    @Test
    void whenTheRefitScreenCannotBeClosedTheWormholeStillReportsAndNothingElseIsTouched() {
        panelGone.set(true);
        InteractionDialogAPI market = mock(InteractionDialogAPI.class);
        when(campaignUi.getCurrentInteractionDialog()).thenReturn(market);
        when(campaignUi.getCurrentCoreTab()).thenReturn(CoreUITabId.REFIT);
        RefitScreenExit exit = exit(market, false);

        exit.advance(0f);

        assertTrue(exit.isDone());
        verify(market, never()).dismiss();
        verify(campaignUi).addMessage(eq(MESSAGE), any(Color.class));
    }

    @Test
    void aRefitScreenThatStaysOpenIsLeftAloneAfterTheWait() {
        panelGone.set(true);
        InteractionDialogAPI market = mock(InteractionDialogAPI.class);
        when(campaignUi.getCurrentInteractionDialog()).thenReturn(market);
        when(campaignUi.getCurrentCoreTab()).thenReturn(CoreUITabId.REFIT);
        RefitScreenExit exit = exit(market, true);

        for (int frame = 0; frame <= RefitScreenExit.MAX_WAIT_FRAMES; frame++) {
            exit.advance(0f);
            assertFalse(exit.isDone(), "frame " + frame);
        }
        exit.advance(0f);

        assertTrue(exit.isDone());
        verify(market, never()).dismiss();
        verify(campaignUi).addMessage(eq(MESSAGE), any(Color.class));
    }

    @Test
    void aStuckLunaLibPanelStopsBlockingAfterTheWait() {
        when(campaignUi.getCurrentCoreTab()).thenReturn(CoreUITabId.REFIT);
        RefitScreenExit exit = exit(null, true);

        for (int frame = 0; frame < RefitScreenExit.MAX_WAIT_FRAMES; frame++) {
            exit.advance(0f);
        }
        assertEquals(0, coreDismissals.get());
        exit.advance(0f);

        assertEquals(1, coreDismissals.get());
    }

    @Test
    void leavingOutsideTheCampaignDoesNothing() {
        mockSector.global.when(Global::getSector).thenReturn(null);

        RefitScreenExit.leave(null, MESSAGE);

        verify(campaignUi, never()).addMessage(anyString(), any(Color.class));
    }
}
