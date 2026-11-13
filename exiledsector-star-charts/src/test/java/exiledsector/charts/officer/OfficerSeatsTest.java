package exiledsector.charts.officer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerSeatsTest {

    private static final List<OfficerRank> RANKS = OfficerRanks.DEFAULTS;

    private static AtlasOfficer seated(int recordId, String rankId, int seat) {
        return AtlasOfficer.waiting(recordId, "hegemony", rankId, "steady", new AtlasOfficer.OfficerSeeds(1, 2, 3, 4), null).seatedAt(rankId, seat);
    }

    @Test
    void everyTwoSeatsAnswerToOneSeatOfTheRankAbove() {
        assertEquals(0, OfficerSeats.superiorSeat(RANKS, 0, 0));
        assertEquals(0, OfficerSeats.superiorSeat(RANKS, 0, 1));
        assertEquals(7, OfficerSeats.superiorSeat(RANKS, 0, 15));
        assertEquals(3, OfficerSeats.superiorSeat(RANKS, 1, 7));
        assertEquals(0, OfficerSeats.superiorSeat(RANKS, 3, 1));
        assertEquals(AtlasOfficer.UNSEATED, OfficerSeats.superiorSeat(RANKS, 4, 0));
        assertEquals(AtlasOfficer.UNSEATED, OfficerSeats.superiorSeat(RANKS, 0, AtlasOfficer.UNSEATED));
    }

    @Test
    void aSeatsSubordinatesAreTheSeatsThatAnswerToIt() {
        assertEquals(List.of(4, 5), OfficerSeats.subordinateSeats(RANKS, 2, 2));
        assertEquals(List.of(0, 1), OfficerSeats.subordinateSeats(RANKS, 4, 0));
        assertTrue(OfficerSeats.subordinateSeats(RANKS, 0, 3).isEmpty());
    }

    @Test
    void onlyASuperiorAndTheirOwnSubordinatesAreAdjacent() {
        AtlasOfficer commander = seated(1, "commander", 1);
        AtlasOfficer subordinate = seated(2, "lieutenant", 3);
        AtlasOfficer otherLieutenant = seated(3, "lieutenant", 4);
        AtlasOfficer captain = seated(4, "captain", 0);
        List<AtlasOfficer> officers = List.of(commander, subordinate, otherLieutenant, captain);

        assertTrue(OfficerSeats.adjacent(RANKS, officers, commander, subordinate));
        assertTrue(OfficerSeats.adjacent(RANKS, officers, subordinate, commander));
        assertTrue(OfficerSeats.adjacent(RANKS, officers, commander, captain));
        assertFalse(OfficerSeats.adjacent(RANKS, officers, commander, otherLieutenant));
        assertFalse(OfficerSeats.adjacent(RANKS, officers, subordinate, otherLieutenant));
        assertFalse(OfficerSeats.adjacent(RANKS, officers, subordinate, captain));
        assertEquals(List.of(subordinate), OfficerSeats.subordinates(RANKS, officers, commander));
        assertEquals(captain, OfficerSeats.superior(RANKS, officers, commander));
        assertNull(OfficerSeats.superior(RANKS, officers, captain));
    }
}
