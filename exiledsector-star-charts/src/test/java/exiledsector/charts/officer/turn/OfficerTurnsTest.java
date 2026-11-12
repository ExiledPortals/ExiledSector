package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasAnomalyChance;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.atlas.TerritoryContext;
import exiledsector.charts.atlas.TestTerritories;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerTurnsTest {

    private static final long SEED = 0x2c4eL;

    private static AtlasGenerator generator(AtlasState state) {
        AtlasRules rules = new AtlasRules(List.of(new AtlasRegion(AtlasRegion.NORMAL, 5f, 0, "", "", Map.of(), Map.of(), Map.of())),
                Map.of("compact", 1f), Map.of("pirates", 1f, "hegemony", 1f), AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER, List.of(),
                List.of());
        return new AtlasGenerator(SEED, rules, AtlasUnlocks.NONE, state);
    }

    @AfterEach
    void restoreDefaults() {
        OfficerTurns.useResolvers(null);
        OfficerTurnTables.useForTests(null);
    }

    @Test
    void otherBossKillsTakeATurnOnlyOnceEnoughHaveFallen() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        int killsPerTurn = OfficerTurnTables.rules().otherBossKillsPerTurn();

        for (int kill = 1; kill < killsPerTurn; kill++) {
            assertNull(OfficerTurns.bossKilled(generator, state, null, TerritoryContext.NONE));
            assertEquals(kill, state.otherBossKills());
        }
        TurnResult result = OfficerTurns.bossKilled(generator, state, null, TerritoryContext.NONE);

        assertNotNull(result);
        assertEquals(TurnCause.bossKills(), result.cause());
        assertEquals(0, state.otherBossKills());
        assertEquals(1, state.officerTurn());
        assertTrue(OfficerTurns.hasUnseenTurn(state));
        state.markOfficerTurnSeen();
        assertFalse(OfficerTurns.hasUnseenTurn(state));
    }

    @Test
    void beatingAnOfficerTakesATurnAtOnceAndLeavesTheKillCountAlone() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        int recordId = TestTerritories.establish(state, new AtlasCell(4, 0), "pirates").recordId();
        state.addOtherBossKill();

        TurnResult result = OfficerTurns.bossKilled(generator, state, recordId, TerritoryContext.NONE);

        assertEquals(TurnCause.officerDefeated(recordId), result.cause());
        assertEquals(1, state.otherBossKills());
        assertEquals(result, OfficerTurns.lastTurn(state));
        assertTrue(state.officer(recordId).hasFled());
        assertTrue(state.officer(recordId).lastChance());
    }

    @Test
    void anUnknownOfficerCountsAsAnotherBossKill() {
        AtlasState state = new AtlasState(SEED);

        assertNull(OfficerTurns.bossKilled(generator(state), state, 42, TerritoryContext.NONE));
        assertEquals(1, state.otherBossKills());
    }

    @Test
    void theOfficerOfTheOpenRunSitsTheTurnOut() {
        AtlasState state = new AtlasState(SEED);
        AtlasCell openRunCell = new AtlasCell(4, 0);
        int holderId = TestTerritories.establish(state, openRunCell, "pirates").recordId();

        assertEquals(holderId, OfficerTurns.openRunOfficerId(state, TurnCause.forced(), openRunCell));
        assertEquals(Intent.NO_OFFICER, OfficerTurns.openRunOfficerId(state, TurnCause.forced(), new AtlasCell(9, 9)));
        assertEquals(Intent.NO_OFFICER, OfficerTurns.openRunOfficerId(state, TurnCause.forced(), null));
        assertEquals(7, OfficerTurns.openRunOfficerId(state, TurnCause.officerVictorious(7), openRunCell));
    }

    @Test
    void listenersHearEveryTurnUntilRemovedAndAFailingListenerIsContained() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        List<TurnResult> heard = new ArrayList<>();
        OfficerTurnListener recorder = (turnState, result) -> heard.add(result);
        OfficerTurnListener failing = (turnState, result) -> {
            throw new IllegalStateException("listener failed");
        };
        OfficerTurns.addListener(failing);
        OfficerTurns.addListener(recorder);
        try {
            TurnResult result = OfficerTurns.take(generator, state, TurnCause.forced(), TerritoryContext.NONE);
            assertEquals(List.of(result), heard);
        } finally {
            OfficerTurns.removeListener(failing);
            OfficerTurns.removeListener(recorder);
        }
        OfficerTurns.take(generator, state, TurnCause.forced(), TerritoryContext.NONE);
        assertEquals(1, heard.size());
    }

    @Test
    void aTurnFirstHandsOverTheCommissionFactionsTerritoriesAndShowsTheReplacements() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = generator(state);
        AtlasCell centre = new AtlasCell(4, 0);
        AtlasOfficer predecessor = TestTerritories.establish(state, centre, "pirates");

        TurnResult result = OfficerTurns.take(generator, state, TurnCause.forced(), new TerritoryContext("pirates", null, FlagshipCatalogue.NONE));

        AtlasOfficer successor = state.officerAt(centre);
        assertEquals("hegemony", successor.factionId());
        assertEquals(predecessor.rankId(), successor.rankId());
        assertTrue(result.replacements().contains(new TurnResult.Replacement(predecessor.recordId(), successor.recordId())));
        assertEquals(predecessor, result.fallen().get(predecessor.recordId()));
        assertNull(result.standingChange(predecessor.recordId()).after());
        assertNull(result.standingChange(successor.recordId()).before());
        assertEquals(result, OfficerTurns.lastTurn(state));
    }

    @Test
    void aMissingResolverSetFallsBackToTheAbstractRoll() {
        OfficerTurns.useResolvers(IntentResolvers.ABSTRACT.with(IntentKind.PUSH, contest -> true));
        assertNotSame(IntentResolvers.ABSTRACT.forKind(IntentKind.PUSH), OfficerTurns.resolvers().forKind(IntentKind.PUSH));

        OfficerTurns.useResolvers(null);
        assertEquals(IntentResolvers.ABSTRACT, OfficerTurns.resolvers());
    }
}
