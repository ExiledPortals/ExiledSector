package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.officer.turn.TurnResult;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommissionHandoverTest {

    private static final long SEED = 0x7e221L;
    private static final String COMMISSION = "persean";
    private static final TerritoryContext COMMISSIONED = new TerritoryContext(COMMISSION, null, FlagshipCatalogue.NONE);
    private static final CarriedItem KERNELS = new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 2);
    private static final CarriedItem ENGRAM = CarriedItem.of(RewardKind.ENGRAM, "engram_a");
    private static final CarriedItem UNIQUE = CarriedItem.of(RewardKind.UNIQUE, "u_x");

    private static AtlasRules rules() {
        List<AtlasRegion> regions = List.of(new AtlasRegion(AtlasRegion.NORMAL, 5f, 0, "", "", Map.of(), Map.of(), Map.of()));
        return new AtlasRules(regions, Map.of("compact", 1f), Map.of("pirates", 5f, "hegemony", 3f, "tritachyon", 2f),
                new AtlasAnomalyChance(0.5f, 0f, 0.5f), AtlasAnomalyChance.NEVER, List.of(), List.of());
    }

    private record Board(AtlasGenerator generator, AtlasState state, List<AtlasCell> centres) {

        Set<AtlasCell> visible() {
            return AtlasReach.visible(generator, state);
        }
    }

    private static Board revealedBoard() {
        AtlasState state = new AtlasState(SEED);
        AtlasGenerator generator = new AtlasGenerator(SEED, rules(), AtlasUnlocks.NONE, state);
        AtlasReach.refreshReveal(generator, state, TerritoryContext.NONE);
        List<AtlasCell> centres = state.officers().stream().filter(AtlasOfficer::isActive).map(AtlasOfficer::commandCentre)
                .sorted(Comparator.comparingLong(AtlasCell::pack)).toList();
        assertTrue(centres.size() >= 2, "the reveal placed " + centres.size() + " territories");
        return new Board(generator, state, centres);
    }

    private static AtlasOfficer commissionOfficer(Board board, AtlasCell centre, String rankId, List<CarriedItem> carried, boolean identified) {
        AtlasOfficer officer = TestTerritories.establish(board.state(), centre, COMMISSION).seatedAt(rankId, 0).withCarried(carried)
                .withRenown(7);
        officer = identified ? officer.identifiedNow() : officer;
        board.state().putOfficer(officer);
        for (AtlasCell neighbour : board.generator().lanes(centre)) {
            if (!board.generator().isVoid(neighbour) && !board.state().isCompleted(neighbour) && !board.state().isClaimed(neighbour)) {
                board.state().claim(neighbour, centre);
            }
        }
        return officer;
    }

    private static void assertNoCommissionTerritory(AtlasState state) {
        state.claims().keySet().forEach(cell -> assertNotEquals(COMMISSION, state.claimedFaction(cell), cell.toString()));
        assertTrue(state.officers().stream().filter(officer -> COMMISSION.equals(officer.factionId()))
                .noneMatch(officer -> officer.isActive() || officer.hasFled()));
    }

    @Test
    void aHandedOverTerritoryKeepsItsCentreClaimsRankItemsAndIdentification() {
        Board board = revealedBoard();
        AtlasCell centre = board.centres().get(0);
        AtlasOfficer predecessor = commissionOfficer(board, centre, "captain", List.of(KERNELS, ENGRAM), true);
        Set<AtlasCell> claims = Set.copyOf(board.state().claimsOf(centre));

        List<CommissionHandover.Handover> handovers = CommissionHandover.handOver(board.generator(), board.state(), board.visible(), COMMISSIONED);

        AtlasOfficer successor = board.state().officerAt(centre);
        assertNotNull(successor);
        assertTrue(handovers.contains(new CommissionHandover.Handover(predecessor, successor)));
        assertNull(board.state().officer(predecessor.recordId()));
        assertNotEquals(predecessor.recordId(), successor.recordId());
        assertNotEquals(COMMISSION, successor.factionId());
        assertEquals("captain", successor.rankId());
        assertEquals(7, successor.renown());
        assertEquals(List.of(KERNELS, ENGRAM), successor.carried());
        assertTrue(successor.identified());
        assertEquals(claims, board.state().claimsOf(centre));
        assertNoCommissionTerritory(board.state());
    }

    @Test
    void anUnidentifiedPredecessorLeavesAnUnidentifiedSuccessor() {
        Board board = revealedBoard();
        AtlasCell centre = board.centres().get(0);
        commissionOfficer(board, centre, "lieutenant", List.of(), false);

        CommissionHandover.handOver(board.generator(), board.state(), board.visible(), COMMISSIONED);

        assertFalse(board.state().officerAt(centre).identified());
    }

    @Test
    void aFledOfficersItemsGoToTheNextOfficerTakingACommandCentre() {
        Board board = revealedBoard();
        AtlasCell fledFrom = board.centres().get(0);
        AtlasCell heldCentre = board.centres().get(1);
        AtlasOfficer fled = commissionOfficer(board, fledFrom, "commander", List.of(UNIQUE, KERNELS), true);
        commissionOfficer(board, heldCentre, "lieutenant", List.of(ENGRAM), false);
        AtlasProgress.complete(board.generator(), board.state(), fledFrom, 1);
        assertTrue(board.state().officer(fled.recordId()).hasFled());

        List<CommissionHandover.Handover> handovers = CommissionHandover.handOver(board.generator(), board.state(), board.visible(), COMMISSIONED);

        AtlasOfficer waitingSuccessor = handovers.stream().filter(handover -> handover.predecessor().recordId() == fled.recordId())
                .map(CommissionHandover.Handover::successor).findFirst().orElseThrow();
        assertTrue(waitingSuccessor.neverPlaced());
        assertNotEquals(COMMISSION, waitingSuccessor.factionId());
        assertEquals("commander", waitingSuccessor.rankId());
        assertTrue(waitingSuccessor.carried().isEmpty());
        assertEquals(List.of(ENGRAM, UNIQUE, KERNELS), board.state().officerAt(heldCentre).carried());
        assertTrue(board.state().leftBehind().isEmpty());
        assertNoCommissionTerritory(board.state());
    }

    @Test
    void leftBehindItemsWaitForTheNextArrival() {
        Board board = revealedBoard();
        AtlasCell fledFrom = board.centres().get(0);
        commissionOfficer(board, fledFrom, "lieutenant", List.of(KERNELS), false);
        AtlasProgress.complete(board.generator(), board.state(), fledFrom, 1);

        CommissionHandover.handOver(board.generator(), board.state(), board.visible(), COMMISSIONED);
        assertEquals(List.of(KERNELS), board.state().leftBehind());

        AtlasOfficer newcomer = board.state().officers().stream().filter(AtlasOfficer::neverPlaced).findFirst().orElseThrow();
        AtlasOfficer arrived = board.state().withLeftBehindItems(newcomer);

        assertEquals(CarriedItem.stack(newcomer.carried(), List.of(KERNELS)), arrived.carried());
        assertTrue(board.state().leftBehind().isEmpty());
    }

    @Test
    void theTerritoryHoldingTheOpenRunIsHandedOverOnceTheRunCloses() {
        Board board = revealedBoard();
        AtlasCell centre = board.centres().get(0);
        AtlasOfficer predecessor = commissionOfficer(board, centre, "lieutenant", List.of(), false);
        TerritoryContext duringRun = new TerritoryContext(COMMISSION, centre, FlagshipCatalogue.NONE);

        assertTrue(CommissionHandover.handOver(board.generator(), board.state(), board.visible(), duringRun).isEmpty());
        assertEquals(predecessor, board.state().officerAt(centre));

        CommissionHandover.handOver(board.generator(), board.state(), board.visible(), duringRun.withoutOpenRun());
        assertNotEquals(COMMISSION, board.state().officerAt(centre).factionId());
    }

    @Test
    void theOpenRunsOfficerIsHandedOverOnceTheRunClosesEvenWhenTheRunIsOutsideTheirTerritory() {
        Board board = revealedBoard();
        AtlasCell centre = board.centres().get(0);
        AtlasOfficer predecessor = commissionOfficer(board, centre, "lieutenant", List.of(), false);
        TerritoryContext duringRun = COMMISSIONED.withOpenRun(board.centres().get(1), predecessor.recordId());

        assertTrue(CommissionHandover.handOver(board.generator(), board.state(), board.visible(), duringRun).isEmpty());
        assertEquals(predecessor, board.state().officerAt(centre));

        CommissionHandover.handOver(board.generator(), board.state(), board.visible(), duringRun.withoutOpenRun());
        assertNotEquals(COMMISSION, board.state().officerAt(centre).factionId());
    }

    @Test
    void aHandOverOutsideATurnJoinsTheLastTurnResultAndShowsAgain() {
        Board board = revealedBoard();
        AtlasCell centre = board.centres().get(0);
        AtlasOfficer predecessor = commissionOfficer(board, centre, "commander", List.of(), false);
        board.state().markOfficerTurnSeen();

        OfficerTurns.handOverCommission(board.generator(), board.state(), board.visible(), COMMISSIONED);

        AtlasOfficer successor = board.state().officerAt(centre);
        TurnResult result = OfficerTurns.lastTurn(board.state());
        assertTrue(result.replacements().contains(new TurnResult.Replacement(predecessor.recordId(), successor.recordId())));
        assertEquals(predecessor, result.fallen().get(predecessor.recordId()));
        assertEquals(new TurnResult.Standing("commander", 7, 0), result.standingChange(predecessor.recordId()).before());
        assertNull(result.standingChange(predecessor.recordId()).after());
        assertEquals(new TurnResult.Standing("commander", 7, 0), result.standingChange(successor.recordId()).after());
        assertTrue(OfficerTurns.hasUnseenTurn(board.state()));
    }

    @Test
    void nothingIsNeededWithoutACommissionOrItsOfficersOnTheBoard() {
        Board board = revealedBoard();
        assertFalse(CommissionHandover.isNeeded(board.state(), COMMISSION));
        assertTrue(CommissionHandover.handOver(board.generator(), board.state(), board.visible(), COMMISSIONED).isEmpty());

        commissionOfficer(board, board.centres().get(0), "lieutenant", List.of(), false);

        assertFalse(CommissionHandover.isNeeded(board.state(), null));
        assertTrue(CommissionHandover.isNeeded(board.state(), COMMISSION));
    }
}
