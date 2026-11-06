package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ruthless_sector.ModPlugin;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuthlessSectorCompatTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaMock;
    private final ModManagerAPI modManager = mock(ModManagerAPI.class);

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(settings.getScriptClassLoader()).thenReturn(RuthlessSectorCompatTest.class.getClassLoader());
        when(modManager.isModEnabled(RuthlessSectorCompat.MOD_ID)).thenReturn(true);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        lunaMock = Mockito.mockStatic(LunaSettings.class);
        playerSetting(true);
        ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES = true;
        RuthlessSectorCompat.resetForTests();
    }

    @AfterEach
    void tearDown() {
        RuthlessSectorCompat.resetForTests();
        ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES = true;
        lunaMock.close();
        globalMock.close();
    }

    private void playerSetting(Boolean gainReputation) {
        lunaMock.when(() -> LunaSettings.getBoolean(RuthlessSectorCompat.MOD_ID, RuthlessSectorCompat.VICTORY_REPUTATION_SETTING))
                .thenReturn(gainReputation);
    }

    @Test
    void victoryReputationIsOffInsideAChartAndBackOnOutside() {
        RuthlessSectorCompat.matchVictoryReputationTo(true);
        assertFalse(ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES);

        RuthlessSectorCompat.matchVictoryReputationTo(false);
        assertTrue(ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES);
    }

    @Test
    void aSettingsReloadInsideAChartIsSwitchedOffAgainAtTheNextEncounter() {
        RuthlessSectorCompat.matchVictoryReputationTo(true);
        ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES = true;

        RuthlessSectorCompat.matchVictoryReputationTo(true);

        assertFalse(ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES);
    }

    @Test
    void leavingAChartRestoresThePlayersOwnSetting() {
        playerSetting(false);
        RuthlessSectorCompat.matchVictoryReputationTo(true);

        RuthlessSectorCompat.matchVictoryReputationTo(false);

        assertFalse(ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES);
    }

    @Test
    void outsideChartsRuthlessSectorsValueIsLeftAlone() {
        ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES = false;

        RuthlessSectorCompat.matchVictoryReputationTo(false);

        assertFalse(ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES);
    }

    @Test
    void withoutRuthlessSectorNothingIsTouched() {
        when(modManager.isModEnabled(RuthlessSectorCompat.MOD_ID)).thenReturn(false);

        RuthlessSectorCompat.matchVictoryReputationTo(true);

        assertTrue(ModPlugin.GAIN_REPUTATION_FOR_IMPRESSIVE_VICTORIES);
    }

    @Test
    void theSwitchResolvesAgainstRuthlessSectorsField() {
        assertEquals(List.of(), RuthlessSectorCompat.missingFeatures());
    }
}
