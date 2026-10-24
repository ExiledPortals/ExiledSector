package exiledsector.ui.cargo;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.ui.ModificationBenchCargoLauncher;
import exiledsector.ui.refit.RefitButtonConfig;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class CargoCraftingButtonTest {

    public abstract static class TradePanel implements UIPanelAPI {
        public final List<Object> children = new ArrayList<>();
        public final TransferHandler transfers = new TransferHandler();
        public int cargoViewRefreshes;
        public boolean showingMarketInfo;
        public Object colonyInfoButton;
        public Object marketCargoGrid;

        public List<Object> getChildrenNonCopy() {
            return children;
        }

        public boolean isShowingMarketInfo() {
            return showingMarketInfo;
        }

        public TransferHandler getTransferHandler() {
            return transfers;
        }

        public Object getMarketInfo() {
            return colonyInfoButton;
        }

        public Object getEntityCargoDisplay() {
            return marketCargoGrid;
        }

        public void updateCargoViews() {
            cargoViewRefreshes++;
        }
    }

    public abstract static class TransactionPanel implements UIPanelAPI {
        public Object getTakeAll() {
            return null;
        }
    }

    public static class TransferHandler {
        public boolean transaction;
        public Object pickedUpStack;

        public boolean transactionExists() {
            return transaction;
        }

        public Object getPickedUpStack() {
            return pickedUpStack;
        }
    }

    public static class CargoTab {
        public boolean lootMode;
        public Object tradePanel;

        public boolean isLootMode() {
            return lootMode;
        }

        public Object getTradePanel() {
            return tradePanel;
        }
    }

    public static class CoreUi {
        public Object currentTab;

        public Object getCurrentTab() {
            return currentTab;
        }
    }

    public abstract static class CampaignUi implements CampaignUIAPI {
        public final CoreUi core = new CoreUi();

        public Object getCore() {
            return core;
        }
    }

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<ModificationBenchCargoLauncher> launcherMock;
    private TradePanel tradePanel;
    private TransactionPanel transactionPanel;
    private PositionAPI placement;
    private CustomPanelAPI container;
    private TooltipMakerAPI element;
    private ButtonAPI button;
    private CustomUIPanelPlugin buttonPlugin;
    private SettingsAPI settings;
    private SectorAPI sector;
    private CampaignUi campaignUI;
    private CargoTab cargoTab;

    @BeforeEach
    void setUp() {
        CargoCraftingButton.resetForTests();
        tradePanel = mock(TradePanel.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        transactionPanel = mock(TransactionPanel.class, CALLS_REAL_METHODS);
        tradePanel.children.add(transactionPanel);
        placement = mock(PositionAPI.class, Mockito.RETURNS_SELF);
        doAnswer(invocation -> {
            tradePanel.children.add(invocation.getArgument(0));
            return placement;
        }).when(tradePanel).addComponent(any());

        container = mock(CustomPanelAPI.class);
        element = mock(TooltipMakerAPI.class);
        when(container.createUIElement(anyFloat(), anyFloat(), eq(false))).thenReturn(element);
        button = mock(ButtonAPI.class);
        when(element.addButton(any(), any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat())).thenReturn(button);

        settings = mock(SettingsAPI.class);
        when(settings.createCustom(anyFloat(), anyFloat(), any())).thenAnswer(invocation -> {
            buttonPlugin = invocation.getArgument(2);
            when(container.getPlugin()).thenReturn(buttonPlugin);
            return container;
        });
        campaignUI = mock(CampaignUi.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        cargoTab = new CargoTab();
        cargoTab.tradePanel = tradePanel;
        campaignUI.core.currentTab = cargoTab;
        sector = mock(SectorAPI.class, Mockito.RETURNS_DEEP_STUBS);
        when(sector.getCampaignUI()).thenReturn(campaignUI);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(sector);
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        launcherMock = Mockito.mockStatic(ModificationBenchCargoLauncher.class);
    }

    @AfterEach
    void tearDown() {
        launcherMock.close();
        lunaSettingsMock.close();
        globalMock.close();
        CargoCraftingButton.resetForTests();
    }

    @Test
    void theButtonSitsBelowTheTransactionPanelWithNoHotkeyByDefault() {
        CargoCraftingButton.attachIfMissing(tradePanel);

        verify(container).createUIElement(CargoCraftingButton.BUTTON_WIDTH, CargoCraftingButton.BUTTON_HEIGHT, false);
        verify(element).setButtonFontOrbitron20Bold();
        verify(element).addButton(any(), eq(CargoCraftingButton.BUTTON_ID), any(), any(), eq(Alignment.MID), eq(CutStyle.ALL),
                eq(CargoCraftingButton.BUTTON_WIDTH), eq(CargoCraftingButton.BUTTON_HEIGHT), eq(0f));
        verify(placement).belowLeft(transactionPanel, CargoCraftingButton.GAP_BELOW_TRANSACTION_PANEL);
        verify(button, never()).setShortcut(anyInt(), anyBoolean());
        verify(button, never()).setEnabled(anyBoolean());
        verify(container, never()).setOpacity(anyFloat());
    }

    @Test
    void dockedTheButtonSitsInTheMarketTitleRowAlignedWithTheCargoGrids() {
        Object colonyInfoButton = new Object();
        UIComponentAPI marketCargoGrid = mock(UIComponentAPI.class, Mockito.RETURNS_DEEP_STUBS);
        when(marketCargoGrid.getPosition().getWidth()).thenReturn(1204f);
        tradePanel.colonyInfoButton = colonyInfoButton;
        tradePanel.marketCargoGrid = marketCargoGrid;
        tradePanel.children.add(colonyInfoButton);

        CargoCraftingButton.attachIfMissing(tradePanel);

        verify(placement).inTL(1204f - CargoCraftingButton.BUTTON_WIDTH, 0f);
        verify(placement, never()).belowLeft(any(), anyFloat());
    }

    @Test
    void aColonyInfoButtonVanillaLeftOutOfTheLayoutKeepsTheButtonBelowTheTransactionPanel() {
        tradePanel.colonyInfoButton = new Object();

        CargoCraftingButton.attachIfMissing(tradePanel);

        verify(placement).belowLeft(transactionPanel, CargoCraftingButton.GAP_BELOW_TRANSACTION_PANEL);
    }

    @Test
    void withoutATransactionPanelNothingIsAttachedAndTheButtonStaysOff() {
        tradePanel.children.remove(transactionPanel);

        CargoCraftingButton.attachIfMissing(tradePanel);

        verify(settings, never()).createCustom(anyFloat(), anyFloat(), any());
        assertTrue(CargoCraftingButton.failed());
    }

    @Test
    void theColonyInfoViewHidesTheButtonAndBlocksPresses() {
        tradePanel.showingMarketInfo = true;
        CargoCraftingButton.attachIfMissing(tradePanel);

        verify(container).setOpacity(0f);
        verify(button).setEnabled(false);
        buttonPlugin.buttonPressed(CargoCraftingButton.BUTTON_ID);
        launcherMock.verify(() -> ModificationBenchCargoLauncher.open(any(), any()), never());

        tradePanel.showingMarketInfo = false;
        buttonPlugin.advance(0f);

        verify(container).setOpacity(1f);
        verify(button).setEnabled(true);
    }

    @Test
    void aBoundCraftingKeyBecomesTheShortcut() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", RefitButtonConfig.BENCH_KEYBIND_FIELD_ID)).thenReturn(Keyboard.KEY_J);

        CargoCraftingButton.attachIfMissing(tradePanel);

        verify(button).setShortcut(Keyboard.KEY_J, true);
    }

    @Test
    void anAttachedButtonIsNotAddedTwiceButComesBackAfterVanillaClearsThePanel() {
        CargoCraftingButton.attachIfMissing(tradePanel);
        CargoCraftingButton.attachIfMissing(tradePanel);
        verify(settings, times(1)).createCustom(anyFloat(), anyFloat(), any());

        tradePanel.children.remove(container);
        CargoCraftingButton.attachIfMissing(tradePanel);

        verify(settings, times(2)).createCustom(anyFloat(), anyFloat(), any());
    }

    @Test
    void aPendingTransactionOrAHeldStackDisablesTheButton() {
        CargoCraftingButton.attachIfMissing(tradePanel);

        buttonPlugin.advance(0f);
        verify(button, never()).setEnabled(anyBoolean());

        tradePanel.transfers.transaction = true;
        buttonPlugin.advance(0f);
        buttonPlugin.buttonPressed(CargoCraftingButton.BUTTON_ID);
        verify(button).setEnabled(false);
        launcherMock.verify(() -> ModificationBenchCargoLauncher.open(any(), any()), never());

        tradePanel.transfers.transaction = false;
        buttonPlugin.advance(0f);
        verify(button).setEnabled(true);

        tradePanel.transfers.pickedUpStack = new Object();
        buttonPlugin.advance(0f);
        verify(button, times(2)).setEnabled(false);
    }

    @Test
    void pressingTheButtonOpensTheBenchAndClosingItRefreshesTheCargoGrids() {
        CargoCraftingButton.attachIfMissing(tradePanel);

        buttonPlugin.buttonPressed("someOtherButton");
        buttonPlugin.buttonPressed(CargoCraftingButton.BUTTON_ID);

        ArgumentCaptor<Runnable> afterClose = ArgumentCaptor.forClass(Runnable.class);
        launcherMock.verify(() -> ModificationBenchCargoLauncher.open(eq(campaignUI), afterClose.capture()), times(1));
        afterClose.getValue().run();
        assertEquals(1, tradePanel.cargoViewRefreshes);
    }

    @Test
    void theTradePanelIsFoundThroughTheCurrentCoreTabButNotOnTheLootScreen() {
        assertSame(tradePanel, CargoCraftingButton.tradePanel(campaignUI));

        cargoTab.lootMode = true;

        assertNull(CargoCraftingButton.tradePanel(campaignUI));
    }

    @Test
    void onlyOpeningTheCargoTabStartsAWatcherAndReopeningReplacesIt() {
        CargoCraftingTabListener listener = new CargoCraftingTabListener();

        listener.reportAboutToOpenCoreTab(CoreUITabId.REFIT, null);
        verify(sector, never()).addTransientScript(any());

        listener.reportAboutToOpenCoreTab(CoreUITabId.CARGO, null);
        listener.reportAboutToOpenCoreTab(CoreUITabId.CARGO, null);

        ArgumentCaptor<EveryFrameScript> scripts = ArgumentCaptor.forClass(EveryFrameScript.class);
        verify(sector, times(2)).addTransientScript(scripts.capture());
        assertTrue(scripts.getAllValues().get(0).isDone());
        assertFalse(scripts.getAllValues().get(1).isDone());
        assertTrue(scripts.getAllValues().get(1).runWhilePaused());
    }

    @Test
    void theWatcherKeepsTheButtonAttachedUntilTheCargoTabCloses() {
        CargoCraftingTabListener.Watcher watcher = new CargoCraftingTabListener.Watcher();
        doReturn(CoreUITabId.CARGO).when(campaignUI).getCurrentCoreTab();

        watcher.advance(0f);
        watcher.advance(0f);
        assertInstanceOf(CargoCraftingButton.class, buttonPlugin);
        verify(settings, times(1)).createCustom(anyFloat(), anyFloat(), any());
        tradePanel.children.remove(container);
        watcher.advance(0f);
        verify(settings, times(2)).createCustom(anyFloat(), anyFloat(), any());

        doReturn(CoreUITabId.REFIT).when(campaignUI).getCurrentCoreTab();
        watcher.advance(0f);

        assertTrue(watcher.isDone());
    }

    @Test
    void theWatcherOnlyLooksForTheButtonAgainWhenItStopsAdvancing() {
        CargoCraftingTabListener.Watcher watcher = new CargoCraftingTabListener.Watcher();
        doReturn(CoreUITabId.CARGO).when(campaignUI).getCurrentCoreTab();
        watcher.advance(0f);
        tradePanel.children.remove(container);

        buttonPlugin.advance(0f);
        watcher.advance(0f);
        verify(settings, times(1)).createCustom(anyFloat(), anyFloat(), any());

        watcher.advance(0f);
        verify(settings, times(2)).createCustom(anyFloat(), anyFloat(), any());
    }

    @Test
    void theWatcherGivesUpWhenNoTradePanelTurnsUp() {
        CargoCraftingTabListener.Watcher watcher = new CargoCraftingTabListener.Watcher();
        doReturn(CoreUITabId.CARGO).when(campaignUI).getCurrentCoreTab();
        cargoTab.lootMode = true;

        for (int frame = 0; frame < CargoCraftingTabListener.MAX_WAIT_FRAMES; frame++) {
            watcher.advance(0f);
        }
        assertFalse(watcher.isDone());
        watcher.advance(0f);

        assertTrue(watcher.isDone());
        verify(settings, never()).createCustom(anyFloat(), anyFloat(), any());
    }
}
