package exiledsector.charts.effect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartAutopilotPluginTest {

    private static final int TOGGLE_KEY = 44;
    private static final int OTHER_KEY = 17;

    private CombatEngineAPI engine;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.isInCampaign()).thenReturn(true);
        settings = mock(SettingsAPI.class);
        when(settings.getBoolean(ChartAutopilotPlugin.TURN_OFF_SETTING)).thenReturn(true);
        when(settings.getControlStringForEnumName(ChartAutopilotPlugin.TOGGLE_CONTROL)).thenReturn("Z");
        when(settings.getCodeFor("Z")).thenReturn(TOGGLE_KEY);
    }

    @AfterEach
    void tearDown() {
        ChartAutopilotPlugin.holdTurnOffSetting(settings, false);
        ChartPlayerModifiers.clear(null);
    }

    @Test
    void aForcedAutopilotChartSwallowsTheToggleKeyAndStopsManualControlTurningItOffUntilTheNextBattle() {
        ChartPlayerModifiers.setAutopilotForced(true);
        ChartAutopilotPlugin plugin = initialised();
        InputEventAPI toggle = keyDown(TOGGLE_KEY);
        InputEventAPI other = keyDown(OTHER_KEY);

        plugin.processInputPreCoreControls(0f, List.of(toggle, other));

        verify(settings).setBoolean(ChartAutopilotPlugin.TURN_OFF_SETTING, false);
        verify(toggle).consume();
        verify(other, never()).consume();

        ChartPlayerModifiers.setAutopilotForced(false);
        initialised();
        initialised();
        verify(settings, times(1)).setBoolean(ChartAutopilotPlugin.TURN_OFF_SETTING, true);
    }

    @Test
    void battlesOutsideAForcedChartAndSimulationsLeaveThePlayerInControl() {
        ChartAutopilotPlugin outside = initialised();
        InputEventAPI toggle = keyDown(TOGGLE_KEY);
        outside.processInputPreCoreControls(0f, List.of(toggle));
        outside.advance(0f, List.of());

        ChartPlayerModifiers.setAutopilotForced(true);
        when(engine.isSimulation()).thenReturn(true);
        ChartAutopilotPlugin simulation = initialised();
        simulation.processInputPreCoreControls(0f, List.of(toggle));
        simulation.advance(0f, List.of());

        verify(toggle, never()).consume();
        verify(engine, never()).getPlayerShip();
        verify(settings, never()).setBoolean(ChartAutopilotPlugin.TURN_OFF_SETTING, false);
    }

    @Test
    void onlyAPilotedShipWithoutAnAiNeedsTheAutopilotTurnedBackOn() {
        ShipAPI manual = mock(ShipAPI.class);
        ShipAPI onAutopilot = mock(ShipAPI.class);
        when(onAutopilot.getShipAI()).thenReturn(mock(ShipAIPlugin.class));
        ShipAPI shuttle = mock(ShipAPI.class);
        when(shuttle.isShuttlePod()).thenReturn(true);
        ShipAPI wreck = mock(ShipAPI.class);
        when(wreck.isHulk()).thenReturn(true);

        assertTrue(ChartAutopilotPlugin.needsAutopilot(manual));
        assertFalse(ChartAutopilotPlugin.needsAutopilot(onAutopilot));
        assertFalse(ChartAutopilotPlugin.needsAutopilot(shuttle));
        assertFalse(ChartAutopilotPlugin.needsAutopilot(wreck));
        assertFalse(ChartAutopilotPlugin.needsAutopilot(null));
    }

    @Test
    void anUnreadableToggleBindingSwallowsNoKey() {
        when(settings.getCodeFor("Z")).thenThrow(new IllegalArgumentException("unknown key"));
        assertEquals(ChartAutopilotPlugin.NO_KEY, ChartAutopilotPlugin.toggleKeyCode(settings));
    }

    private ChartAutopilotPlugin initialised() {
        ChartAutopilotPlugin plugin = new ChartAutopilotPlugin();
        try (MockedStatic<Global> globalMock = Mockito.mockStatic(Global.class)) {
            globalMock.when(Global::getSettings).thenReturn(settings);
            plugin.init(engine);
        }
        return plugin;
    }

    private static InputEventAPI keyDown(int key) {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.isKeyDownEvent()).thenReturn(true);
        when(event.getEventValue()).thenReturn(key);
        return event;
    }
}
