package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerBoardGenerator;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerSeats;
import exiledsector.charts.officer.turn.Intent;
import exiledsector.charts.officer.turn.IntentKind;
import exiledsector.charts.officer.turn.OfficerTurn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerSeatRepairTest {

    private static final List<OfficerRank> SEATED_RANKS = List.of(
            new OfficerRank("lieutenant", 10, 0, 1, 16),
            new OfficerRank("commander", 20, 10, 1, 8),
            new OfficerRank("captain", 30, 20, 2, 4),
            new OfficerRank("commodore", 45, 35, 2, 2),
            new OfficerRank("admiral", 60, 50, 3, 1));
    private static final List<OfficerRank> SEATLESS_RANKS = List.of(
            new OfficerRank("lieutenant", 10, 0, 1, 16),
            new OfficerRank("commander", 20, 10, 1, 12),
            new OfficerRank("captain", 30, 20, 2, 8),
            new OfficerRank("commodore", 45, 35, 2, 6),
            new OfficerRank("admiral", 60, 50, 3, 3));
    private static final Map<String, Float> FACTIONS = Map.of("hegemony", 1f, "tritachyon", 1f);

    private static AtlasState seatlessBoard(AtlasState state) {
        for (AtlasOfficer officer : OfficerBoardGenerator.generate(TestBorders.SEED, SEATLESS_RANKS, FACTIONS, FlagshipCatalogue.NONE)) {
            state.putOfficer(officer.seatedAt(officer.rankId(), AtlasOfficer.UNSEATED).withRenown(officer.recordId()));
        }
        return state;
    }

    @Test
    void aBoardFromBeforeSeatsIsSeatedFromTheTopAndTheRestDismissed() {
        AtlasState state = seatlessBoard(new AtlasState(TestBorders.SEED));
        AtlasGenerator generator = TestBorders.generator(state);
        state.setLastTurnResult(List.of("turn,1"));
        state.setDeclaredIntents(List.of(Intent.challenge(1, 45).encode(), Intent.expand(2, new AtlasCell(4, 4)).encode()));

        assertTrue(OfficerSeatRepair.repair(generator, state, SEATED_RANKS, TerritoryContext.NONE));

        assertEquals(31, state.officers().size());
        assertTrue(OfficerSeatRepair.everySeatHeldOnce(SEATED_RANKS, state.officers()));
        assertEquals(45, OfficerSeats.seated(state.officers(), "admiral", 0).recordId());
        assertEquals(List.of(44, 43), List.of(OfficerSeats.seated(state.officers(), "commodore", 0).recordId(),
                OfficerSeats.seated(state.officers(), "commodore", 1).recordId()));
        assertTrue(state.lastTurnResult().isEmpty());
        assertEquals(List.of(IntentKind.EXPAND), OfficerTurn.decodeIntents(state.declaredIntents()).stream().map(Intent::kind).toList());
        assertFalse(OfficerSeatRepair.repair(generator, state, SEATED_RANKS, TerritoryContext.NONE));
    }

    @Test
    void anOfficerHoldingACommandKeepsASeatOverMoreRenownedWaitingOnes() {
        AtlasState state = seatlessBoard(new AtlasState(TestBorders.SEED));
        AtlasGenerator generator = TestBorders.generator(state);
        AtlasCell centre = new AtlasCell(3, 3);
        state.putOfficer(state.officer(1).takingCommand(centre, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, centre)));
        state.claim(centre, centre);

        OfficerSeatRepair.repair(generator, state, SEATED_RANKS, TerritoryContext.NONE);

        assertNotNull(state.officer(1));
        assertEquals("lieutenant", state.officer(1).rankId());
        assertTrue(state.officer(1).isSeated());
        assertEquals(centre, state.officer(1).commandCentre());
    }

    @Test
    void emptySeatsAreFilledWithNewOfficers() {
        AtlasState state = new AtlasState(TestBorders.SEED);
        AtlasGenerator generator = TestBorders.generator(state);
        state.putOfficer(OfficerBoardGenerator.generate(TestBorders.SEED, SEATED_RANKS, FACTIONS, FlagshipCatalogue.NONE).get(0));

        assertTrue(OfficerSeatRepair.repair(generator, state, SEATED_RANKS, TerritoryContext.NONE));

        assertEquals(31, state.officers().size());
        assertTrue(OfficerSeatRepair.everySeatHeldOnce(SEATED_RANKS, state.officers()));
    }
}
