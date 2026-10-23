package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import exerelin.campaign.SectorManager;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexFactionConfig;
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
import static org.mockito.Mockito.when;

class NexerelinCompatTest {

    private MockedStatic<Global> globalMock;
    private final ModManagerAPI modManager = mock(ModManagerAPI.class);

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(settings.getScriptClassLoader()).thenReturn(NexerelinCompatTest.class.getClassLoader());
        when(modManager.isModEnabled(NexerelinCompat.MOD_ID)).thenReturn(true);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        SectorManager.LIVE_FACTION_IDS.clear();
        NexConfig.CONFIGS_BY_FACTION.clear();
        globalMock.close();
    }

    private static void configure(String factionId, boolean playable) {
        NexFactionConfig config = new NexFactionConfig();
        config.playableFaction = playable;
        NexConfig.CONFIGS_BY_FACTION.put(factionId, config);
    }

    @Test
    void onlyAPlayableFactionNexerelinNoLongerHasAliveCountsAsEliminated() {
        configure("hegemony", true);
        configure("tritachyon", true);
        configure("remnant", false);
        SectorManager.LIVE_FACTION_IDS.add("tritachyon");

        assertTrue(NexerelinCompat.isEliminated("hegemony"));
        assertFalse(NexerelinCompat.isEliminated("tritachyon"));
        assertFalse(NexerelinCompat.isEliminated("remnant"));
        assertFalse(NexerelinCompat.isEliminated("rat_abyssals"));
        assertFalse(NexerelinCompat.isEliminated(null));
    }

    @Test
    void nothingIsEliminatedWithoutNexerelin() {
        configure("hegemony", true);
        when(modManager.isModEnabled(NexerelinCompat.MOD_ID)).thenReturn(false);

        assertFalse(NexerelinCompat.isEliminated("hegemony"));
    }

    @Test
    void theFactionLookupResolvesAgainstNexerelinsApi() {
        assertEquals(List.of(), NexerelinCompat.missingFeatures().stream().filter(feature -> feature.contains("isFactionAlive")).toList());
    }
}
