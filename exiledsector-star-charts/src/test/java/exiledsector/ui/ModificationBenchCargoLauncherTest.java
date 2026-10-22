package exiledsector.ui;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.items.SocketCustody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ModificationBenchCargoLauncherTest {

    private static final float SCREEN_WIDTH = 1920f;
    private static final float SCREEN_HEIGHT = 1080f;

    public abstract static class CorePanel implements UIPanelAPI {
        public final List<Object> children = new ArrayList<>();

        public List<Object> getChildrenNonCopy() {
            return children;
        }
    }

    public abstract static class CampaignUi implements CampaignUIAPI {
        public Object core;

        public Object getCore() {
            return core;
        }
    }

    private MockedStatic<Global> globalMock;
    private MockedStatic<SocketCustody> custodyMock;
    private MockedStatic<SkillTreeSounds> soundsMock;
    private CorePanel corePanel;
    private PositionAPI placement;
    private CustomPanelAPI benchPanel;
    private CustomUIPanelPlugin benchPlugin;
    private SettingsAPI settings;
    private SectorAPI sector;
    private CampaignUi campaignUI;

    @BeforeEach
    void setUp() {
        corePanel = mock(CorePanel.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        placement = mock(PositionAPI.class, Mockito.RETURNS_SELF);
        doAnswer(invocation -> {
            corePanel.children.add(invocation.getArgument(0));
            return placement;
        }).when(corePanel).addComponent(any());
        benchPanel = mock(CustomPanelAPI.class);
        settings = mock(SettingsAPI.class);
        when(settings.getScreenWidth()).thenReturn(SCREEN_WIDTH);
        when(settings.getScreenHeight()).thenReturn(SCREEN_HEIGHT);
        when(settings.createCustom(anyFloat(), anyFloat(), any())).thenAnswer(invocation -> {
            benchPlugin = invocation.getArgument(2);
            when(benchPanel.getPlugin()).thenReturn(benchPlugin);
            return benchPanel;
        });
        campaignUI = mock(CampaignUi.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        campaignUI.core = corePanel;
        sector = mock(SectorAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignUI);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(sector);
        custodyMock = Mockito.mockStatic(SocketCustody.class);
        custodyMock.when(SocketCustody::reconcile).thenReturn(Map.of());
        soundsMock = Mockito.mockStatic(SkillTreeSounds.class);
    }

    @AfterEach
    void tearDown() {
        soundsMock.close();
        custodyMock.close();
        globalMock.close();
    }

    @Test
    void theBenchCoversTheScreenOnTheCoreUiAndOnlyOnce() {
        assertTrue(ModificationBenchCargoLauncher.open(campaignUI, null));

        verify(settings).createCustom(SCREEN_WIDTH, SCREEN_HEIGHT, benchPlugin);
        assertInstanceOf(ModificationBenchScreen.class, benchPlugin);
        verify(placement).inTL(0f, 0f);

        assertFalse(ModificationBenchCargoLauncher.open(campaignUI, null));
        verify(settings, times(1)).createCustom(anyFloat(), anyFloat(), any());
    }

    @Test
    void theStarChartsEntryLocksTheBenchToStarChartsAndTheInventoryEntryDoesNot() {
        ModificationBenchCargoLauncher.open(campaignUI, null);
        assertFalse(((ModificationBenchScreen) benchPlugin).starChartsOnly());
        corePanel.children.clear();

        assertTrue(ModificationBenchCargoLauncher.openStarCharts(campaignUI));

        assertTrue(((ModificationBenchScreen) benchPlugin).starChartsOnly());
    }

    @Test
    void closingRemovesTheBenchOnTheNextFrameAndThenRunsTheAfterCloseStep() {
        AtomicInteger afterCloseRuns = new AtomicInteger();
        ModificationBenchCargoLauncher.open(campaignUI, afterCloseRuns::incrementAndGet);

        InputEventAPI escape = mock(InputEventAPI.class);
        when(escape.isKeyUpEvent()).thenReturn(true);
        when(escape.getEventValue()).thenReturn(Keyboard.KEY_ESCAPE);
        benchPlugin.processInput(List.of(escape));
        benchPlugin.advance(1f);

        verify(corePanel, never()).removeComponent(any());
        ArgumentCaptor<EveryFrameScript> removal = ArgumentCaptor.forClass(EveryFrameScript.class);
        verify(sector).addTransientScript(removal.capture());
        assertTrue(removal.getValue().runWhilePaused());

        removal.getValue().advance(0f);
        removal.getValue().advance(0f);

        verify(corePanel, times(1)).removeComponent(benchPanel);
        assertEquals(1, afterCloseRuns.get());
        assertTrue(removal.getValue().isDone());
    }
}
