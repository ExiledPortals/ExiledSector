package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartRunTest {

    private MockedStatic<Global> globalMock;
    private Map<String, Object> persistentData;

    @BeforeEach
    void setUp() {
        persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ChartRun run(int entries) {
        return new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 100f, 200f, entries);
    }

    @Test
    void aNewRunIsOpenWithEveryEntryLeftAndDerivesItsIdsFromTheRunId() {
        ChartRun run = run(2);

        assertEquals(ChartRunState.OPEN, run.runState());
        assertEquals(2, run.entriesLeft());
        assertTrue(run.canEnter());
        assertEquals("exiledSector_chart_abc", run.pocketSystemId());
        assertEquals("exiledSector_chart_outer_abc", run.outerWormholeId());
        assertEquals("exiledSector_chart_inner_abc", run.innerWormholeId());
    }

    @Test
    void entriesBelowOneAreRaisedToOne() {
        assertEquals(1, run(0).entriesAllowed());
    }

    @Test
    void withOneEntryArrivingCollapsesTheOuterWormholeAndLeavingEndsTheRun() {
        ChartRun run = run(1);

        assertTrue(run.recordArrival(), "the last entry collapses the outer wormhole behind the player");
        assertEquals(ChartRunState.INSIDE, run.runState());
        assertTrue(run.isOuterCollapsed());
        run.requestReturn();

        assertEquals(ChartRunState.CLOSING, run.recordLeftPocket());
    }

    @Test
    void returningWithEntriesLeftKeepsTheRunOpenForAnotherEntry() {
        ChartRun run = run(2);

        assertFalse(run.recordArrival());
        run.requestReturn();
        assertEquals(ChartRunState.OPEN, run.recordLeftPocket());
        assertTrue(run.canEnter());
        assertTrue(run.recordArrival(), "the second and last entry collapses the outer wormhole");
        assertEquals(0, run.entriesLeft());
        assertFalse(run.canEnter());
    }

    @Test
    void leavingThePocketWithoutReturningThroughTheWormholeEndsTheRunEvenWithEntriesLeft() {
        ChartRun run = run(3);
        run.recordArrival();

        assertEquals(ChartRunState.CLOSING, run.recordLeftPocket(), "death respawns and escapes close the run");
    }

    @Test
    void anAbortedReturnIsForgottenSoALaterEscapeStillClosesTheRun() {
        ChartRun run = run(3);
        run.recordArrival();
        run.requestReturn();
        run.cancelReturn();

        assertEquals(ChartRunState.CLOSING, run.recordLeftPocket());
    }

    @Test
    void collapsingTheOuterWormholeWorksOnlyFromOutside() {
        ChartRun outside = run(2);
        assertTrue(outside.collapseOuter());
        assertEquals(ChartRunState.CLOSING, outside.runState());
        assertFalse(outside.canEnter());

        ChartRun inside = run(2);
        inside.recordArrival();
        assertFalse(inside.collapseOuter());
        assertEquals(ChartRunState.INSIDE, inside.runState());
    }

    @Test
    void arrivalOnlyCountsFromTheOpenState() {
        ChartRun run = run(2);
        run.recordArrival();

        assertFalse(run.recordArrival());
        assertEquals(1, run.entriesUsed());
    }

    @Test
    void closingIsFinalOnceClosed() {
        ChartRun run = run(1);
        run.markClosed();
        run.beginClosing();
        run.resumeInside();

        assertEquals(ChartRunState.CLOSED, run.runState());
    }

    @Test
    void aClosingRunResumesInsideWhenTheSaveHasThePlayerInThePocket() {
        ChartRun run = run(1);
        run.recordArrival();
        run.beginClosing();
        run.resumeInside();

        assertEquals(ChartRunState.INSIDE, run.runState());
    }

    @Test
    void anAdoptedRunIsInsideWithTheWayInCollapsed() {
        ChartRun adopted = ChartRun.adoptedWhileInside("xyz", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f);

        assertEquals(ChartRunState.INSIDE, adopted.runState());
        assertTrue(adopted.isOuterCollapsed());
        assertEquals(0, adopted.entriesLeft());
        adopted.requestReturn();
        assertEquals(ChartRunState.CLOSING, adopted.recordLeftPocket());
    }

    @Test
    void onlyOneRunCanBeStoredAtATime() {
        ChartRun first = run(1);
        ChartRun second = new ChartRun("def", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);

        assertTrue(ChartRuns.store(first));
        assertFalse(ChartRuns.store(second));
        assertSame(first, ChartRuns.currentOrNull());
    }

    @Test
    void deletingAnotherRunsRecordLeavesTheCurrentOneAlone() {
        ChartRuns.store(run(1));

        ChartRuns.delete("someone_else");
        assertTrue(ChartRuns.hasRun());
        ChartRuns.delete("abc");
        assertNull(ChartRuns.currentOrNull());
        assertFalse(persistentData.containsKey(ChartRuns.DATA_KEY));
    }

    @Test
    void theRunRemembersItsLayoutSeedAndTypeAsPlainValues() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        assertFalse(run.hasLayout());

        run.recordLayout(new ChartLayout(-9L, StarSystemType.TRINARY_2CLOSE, true, new Vector2f()));

        assertTrue(run.hasLayout());
        assertEquals(-9L, run.layoutSeed());
        assertEquals("TRINARY_2CLOSE", run.layoutType());
        assertTrue(run.isLayoutGenerated());
    }

    @Test
    void theRecordKeyIsNotCountedByTheSaveCheckerMarker() {
        assertFalse(ChartRuns.DATA_KEY.startsWith(ChartIds.PREFIX));
    }
}
