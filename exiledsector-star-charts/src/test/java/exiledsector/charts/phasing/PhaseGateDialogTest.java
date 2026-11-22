package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhaseGateDialogTest {

    private MockSector mockSector;
    private MockedStatic<Misc> miscMock;
    private final Map<String, Object> gateMarketMemory = new HashMap<>();
    private final Map<String, Object> phaseGateMemory = new HashMap<>();
    private MarketAPI market;
    private SectorEntityToken phaseGate;
    private InteractionDialogAPI dialog;
    private OptionPanelAPI options;
    private final PhasedPlanetRecord job = new PhasedPlanetRecord("run1", "planet_3", "Charted Space III", "frozen", 140f, null,
            List.of("cold"), "Charted Space", null, List.of(), 6, List.of());

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getColor(anyString())).thenReturn(Color.WHITE);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        miscMock = Mockito.mockStatic(Misc.class, Mockito.CALLS_REAL_METHODS);
        miscMock.when(() -> Misc.fadeAndExpire(any())).thenAnswer(invocation -> null);
        market = mock(MarketAPI.class);
        when(market.getId()).thenReturn("home");
        when(market.getName()).thenReturn("Home");
        MemoryAPI marketMemory = PhaseLockTest.mapMemory(gateMarketMemory);
        when(market.getMemoryWithoutUpdate()).thenReturn(marketMemory);
        EconomyAPI economy = mock(EconomyAPI.class);
        when(economy.getMarket("home")).thenReturn(market);
        when(mockSector.sector.getEconomy()).thenReturn(economy);

        phaseGate = mock(SectorEntityToken.class);
        when(phaseGate.getCustomEntityType()).thenReturn(PhasePull.GATE_TYPE);
        FactionAPI faction = mock(FactionAPI.class);
        when(phaseGate.getFaction()).thenReturn(faction);
        CharacterDataAPI characterData = mock(CharacterDataAPI.class);
        when(mockSector.sector.getCharacterData()).thenReturn(characterData);
        MemoryAPI gateMemory = PhaseLockTest.mapMemory(phaseGateMemory);
        when(phaseGate.getMemoryWithoutUpdate()).thenReturn(gateMemory);
        phaseGateMemory.put(PhasePull.GATE_MARKET_KEY, "home");
        when(mockSector.sector.getEntityById(PhasePull.gateId(market))).thenReturn(phaseGate);

        dialog = mock(InteractionDialogAPI.class, RETURNS_DEEP_STUBS);
        options = mock(OptionPanelAPI.class);
        when(dialog.getOptionPanel()).thenReturn(options);
        TextPanelAPI text = mock(TextPanelAPI.class);
        when(dialog.getTextPanel()).thenReturn(text);
        when(dialog.getInteractionTarget()).thenReturn(phaseGate);
    }

    @AfterEach
    void tearDown() {
        miscMock.close();
        mockSector.close();
    }

    @Test
    void aRunningPullOffersACancelBehindAConfirmation() {
        PhaseGateState.startJob(market, job);

        new PhaseGateDialog().init(dialog);

        verify(options).addOption(anyString(), eq(PhaseGateDialog.CANCEL));
        verify(options).addOptionConfirmation(eq(PhaseGateDialog.CANCEL), anyString(), anyString(), anyString());
        verify(options).addOption(anyString(), eq(PhaseGateDialog.LEAVE));
    }

    @Test
    void cancellingEndsThePullAndFadesTheGate() {
        PhaseGateState.startJob(market, job);
        PhaseGateDialog gateDialog = new PhaseGateDialog();
        gateDialog.init(dialog);

        gateDialog.optionSelected("Cancel phasing", PhaseGateDialog.CANCEL);

        assertFalse(PhaseGateState.hasJobEntry(market));
        miscMock.verify(() -> Misc.fadeAndExpire(phaseGate));
        verify(dialog, never()).dismiss();
    }

    @Test
    void aGateWithoutAPullOffersOnlyLeave() {
        new PhaseGateDialog().init(dialog);

        verify(options, never()).addOption(anyString(), eq(PhaseGateDialog.CANCEL));
        verify(options).addOption(anyString(), eq(PhaseGateDialog.LEAVE));
        assertTrue(PhasePull.isPhaseGate(phaseGate));
    }
}
