package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearnedPhantomConflictsTest {

    private final String[] stored = {null};
    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        when(settings.getHullModSpec(anyString())).thenReturn(mock(HullModSpecAPI.class));
        when(settings.getHullSpec(anyString())).thenReturn(mock(ShipHullSpecAPI.class));
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        LearnedPhantomConflicts.useStorage(new LearnedPhantomConflicts.Storage() {
            @Override
            public String read() {
                return stored[0];
            }

            @Override
            public void write(String contents) {
                stored[0] = contents;
            }
        });
    }

    @AfterEach
    void tearDown() {
        LearnedPhantomConflicts.useCommonFolder();
        globalMock.close();
    }

    @Test
    void aLearnedHullModConflictBlocksOnlyShipsCarryingThatHullMod() {
        assertTrue(LearnedPhantomConflicts.learnHullMod("safetyoverrides", "csp_cetanframe"));
        assertFalse(LearnedPhantomConflicts.learnHullMod("safetyoverrides", "csp_cetanframe"));

        assertEquals("csp_cetanframe", LearnedPhantomConflicts.conflictFor("safetyoverrides", "cetan", Set.of("csp_cetanframe")::contains).hullModId());
        assertNull(LearnedPhantomConflicts.conflictFor("safetyoverrides", "wolf", Set.<String>of()::contains));
        assertNull(LearnedPhantomConflicts.conflictFor("fluxbreakers", "cetan", Set.of("csp_cetanframe")::contains));
    }

    @Test
    void aLearnedHullConflictBlocksThatHullWhateverItCarries() {
        LearnedPhantomConflicts.learnHull("safetyoverrides", "gr_nunki");

        assertTrue(LearnedPhantomConflicts.conflictFor("safetyoverrides", "gr_nunki", Set.<String>of()::contains).isWholeHull());
        assertNull(LearnedPhantomConflicts.conflictFor("safetyoverrides", "wolf", Set.<String>of()::contains));
        assertNull(LearnedPhantomConflicts.conflictFor("safetyoverrides", null, Set.<String>of()::contains));
    }

    @Test
    void learnedConflictsSurviveAReloadFromStorage() {
        LearnedPhantomConflicts.learnHullMod("safetyoverrides", "csp_cetanframe");
        LearnedPhantomConflicts.learnHull("safetyoverrides", "gr_nunki");

        LearnedPhantomConflicts.load();

        assertTrue(LearnedPhantomConflicts.conflictFor("safetyoverrides", "gr_nunki", Set.<String>of()::contains).isWholeHull());
        assertEquals("csp_cetanframe", LearnedPhantomConflicts.conflictFor("safetyoverrides", "x", Set.of("csp_cetanframe")::contains).hullModId());
    }

    @Test
    void entriesForHullModsOrHullsNoLongerLoadedAreDroppedOnLoad() {
        LearnedPhantomConflicts.learnHullMod("safetyoverrides", "removed_mod");
        LearnedPhantomConflicts.learnHull("safetyoverrides", "removed_hull");
        when(settings.getHullModSpec("removed_mod")).thenReturn(null);
        when(settings.getHullSpec("removed_hull")).thenThrow(new RuntimeException("not found"));

        LearnedPhantomConflicts.load();

        assertNull(LearnedPhantomConflicts.conflictFor("safetyoverrides", "removed_hull", Set.of("removed_mod")::contains));
    }

    @Test
    void unreadableStorageLeavesNoConflicts() {
        stored[0] = "{not json";

        LearnedPhantomConflicts.load();

        assertNull(LearnedPhantomConflicts.conflictFor("safetyoverrides", "gr_nunki", Set.<String>of()::contains));
    }
}
