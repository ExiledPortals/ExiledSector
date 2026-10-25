package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledlib.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartRunTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static ChartRun run(int entries) {
        return new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 100f, 200f, entries);
    }

    @Test
    void theGuaranteedBossTierUpIsRememberedOnceGranted() {
        ChartRun run = run(1);

        assertFalse(run.isBossTierUpGranted());
        run.grantBossTierUp();
        assertTrue(run.isBossTierUpGranted());
    }

    @Test
    void aNewRunIsOpenWithEveryEntryLeftAndDerivesItsIdsFromTheRunId() {
        ChartRun run = run(2);

        assertEquals(ChartRunState.OPEN, run.runState());
        assertEquals(2, run.entriesLeft());
        assertTrue(run.canEnter());
        assertEquals("exiledSector_chart_abc", run.systemId());
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

        assertEquals(ChartRunState.CLOSING, run.recordLeftSystem());
    }

    @Test
    void returningWithEntriesLeftKeepsTheRunOpenForAnotherEntry() {
        ChartRun run = run(2);

        assertFalse(run.recordArrival());
        run.requestReturn();
        assertEquals(ChartRunState.OPEN, run.recordLeftSystem());
        assertTrue(run.canEnter());
        assertTrue(run.recordArrival(), "the second and last entry collapses the outer wormhole");
        assertEquals(0, run.entriesLeft());
        assertFalse(run.canEnter());
    }

    @Test
    void leavingTheSystemWithoutReturningThroughTheWormholeEndsTheRunEvenWithEntriesLeft() {
        ChartRun run = run(3);
        run.recordArrival();

        assertEquals(ChartRunState.CLOSING, run.recordLeftSystem(), "death respawns and escapes close the run");
    }

    @Test
    void anAbortedReturnIsForgottenSoALaterEscapeStillClosesTheRun() {
        ChartRun run = run(3);
        run.recordArrival();
        run.requestReturn();
        run.cancelReturn();

        assertEquals(ChartRunState.CLOSING, run.recordLeftSystem());
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
    void aClosingRunResumesInsideWhenTheSaveHasThePlayerInTheSystem() {
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
        assertEquals(ChartRunState.CLOSING, adopted.recordLeftSystem());
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
        assertFalse(mockSector.persistentData.containsKey(ChartRuns.DATA_KEY));
    }

    @Test
    void theRunRemembersItsLayoutSeedAndTypeAsPlainValues() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        assertFalse(run.hasLayout());

        run.recordLayout(new ChartLayout(-9L, StarSystemType.TRINARY_2CLOSE, true, new Vector2f(), null));

        assertTrue(run.hasLayout());
        assertEquals(-9L, run.layoutSeed());
        assertEquals("TRINARY_2CLOSE", run.layoutType());
        assertTrue(run.isLayoutGenerated());
    }

    @Test
    void theRecordKeyIsNotCountedByTheSaveCheckerMarker() {
        assertFalse(ChartRuns.DATA_KEY.startsWith(ChartIds.PREFIX));
    }

    @Test
    void theRunKeepsItsBossTypeForTheObjectiveDescription() {
        ChartRun fleetRun = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        fleetRun.recordContent(new ChartContent(4, "pirates", ChartObjective.BOSS, List.of("b"), "b", 3, 2));
        ChartRun stationRun = new ChartRun("def", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        stationRun.recordContent(new ChartContent(4, "pirates", ChartObjective.BOSS, List.of("s"), "s", ChartBossType.STATION, 3, 2, List.of()));
        ChartRun clearRun = new ChartRun("ghi", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        clearRun.recordContent(new ChartContent(4, "pirates", ChartObjective.CLEAR, List.of("g"), "s", ChartBossType.STATION, 3, 2, List.of()));

        assertEquals(ChartBossType.FLEET, fleetRun.bossType());
        assertEquals(ChartBossType.STATION, stationRun.bossType());
        assertEquals("chart.objective.boss.fleet.description", ChartObjectiveIntel.descriptionKey(fleetRun));
        assertEquals("chart.objective.boss.station.description", ChartObjectiveIntel.descriptionKey(stationRun));
        assertEquals("chart.objective.clear.description", ChartObjectiveIntel.descriptionKey(clearRun));
    }

    @Test
    void theContentRecordKeepsOnlyIdsAndTracksTheObjectiveOnce() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        assertEquals(ChartTiers.LOWEST_TIER, run.tier());
        assertNull(run.objective());

        run.recordContent(new ChartContent(12, "remnant", ChartObjective.CLEAR, List.of("g1", "g2"), "b", 5, 9));

        assertEquals(12, run.tier());
        assertEquals("remnant", run.contentFaction());
        assertEquals(ChartObjective.CLEAR, run.objective());
        assertEquals(List.of("g1", "g2"), run.objectiveTargetIds());
        assertEquals(5, run.contentFleets());
        assertEquals(9, run.contentPois());
        assertFalse(run.markObjectiveComplete());
        assertFalse(run.claimReward());
        assertTrue(run.recordTargetGone("g1"));
        assertFalse(run.recordTargetGone("g1"));
        assertFalse(run.recordTargetGone("someone else"));
        assertFalse(run.isObjectiveMet());
        assertTrue(run.recordTargetGone("g2"));
        assertTrue(run.isObjectiveMet());
        assertTrue(run.markObjectiveComplete());
        assertFalse(run.markObjectiveComplete());
        assertFalse(run.recordTargetGone("g2"));
        assertTrue(run.claimReward());
        assertFalse(run.claimReward());
    }

    @Test
    void idsThatCannotBeJoinedSafelyAreDroppedAndAnEmptyObjectiveIsNeverMet() {
        assertEquals("a|b", ChartRun.join(List.of("a", "", "x|y", "b")));
        assertNull(ChartRun.join(List.of()));
        assertEquals(List.of(), ChartRun.split(null));
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(40, null, ChartObjective.BOSS, List.of(), null, 0, 0));
        assertEquals(ChartTiers.HIGHEST_TIER, run.tier());
        assertFalse(run.isObjectiveMet());
        assertFalse(run.recordTargetGone(null));
    }

    @Test
    void aSavedVaultRunLoadsAsClearWithoutItsVaultTarget() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        xstream.ignoreUnknownElements();
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(5, "pirates", ChartObjective.CLEAR, List.of("vault1"), null, 2, 4));
        String vaultXml = xstream.toXML(run)
                .replace("<objective>CLEAR</objective>", "<objective>" + ChartRun.LEGACY_VAULT_OBJECTIVE + "</objective><vaultEntityId>vault1</vaultEntityId>");

        ChartRun loaded = (ChartRun) xstream.fromXML(vaultXml);

        assertEquals(ChartObjective.CLEAR, loaded.objective());
        assertEquals(List.of(), loaded.objectiveTargetIds());
        assertEquals(List.of(), loaded.defeatedTargetIds());
        assertEquals(5, loaded.tier());
        assertFalse(loaded.isObjectiveMet());
    }
}
