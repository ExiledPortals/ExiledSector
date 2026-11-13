package exiledsector.ui.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerRank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerBoardLayoutTest {

    static final List<OfficerRank> RANKS = List.of(new OfficerRank("lieutenant", 100, 100, 1, 4), new OfficerRank("commander", 100, 100, 1, 2),
            new OfficerRank("admiral", 100, 100, 1, 1));

    @Test
    void theHighestRankIsTheTopRow() {
        OfficerBoardLayout layout = OfficerBoardLayout.of(RANKS, List.of());

        assertEquals(3, layout.rowCount());
        assertEquals("admiral", layout.rankIdOfRow(0));
        assertEquals("lieutenant", layout.rankIdOfRow(2));
    }

    @Test
    void anOfficerSitsInTheColumnOfItsSeat() {
        OfficerBoardLayout layout = OfficerBoardLayout.of(RANKS, List.of(new OfficerBoardLayout.Entry(4, "lieutenant", 9, 3),
                new OfficerBoardLayout.Entry(2, "lieutenant", 1, 0)));

        assertEquals(new OfficerBoardLayout.Seat(2, 3, 4), layout.seat(4));
        assertEquals(new OfficerBoardLayout.Seat(2, 0, 4), layout.seat(2));
        assertEquals(List.of(2, 4), layout.row(2));
        assertEquals(Integer.valueOf(4), layout.recordIdAt(new OfficerBoardLayout.Seat(2, 3, 4)));
        assertNull(layout.recordIdAt(new OfficerBoardLayout.Seat(2, 1, 4)));
    }

    @Test
    void anUnseatedOfficerTakesTheFirstFreeColumn() {
        OfficerBoardLayout layout = OfficerBoardLayout.of(RANKS, List.of(new OfficerBoardLayout.Entry(1, "commander", 0, 0),
                new OfficerBoardLayout.Entry(2, "commander", 5, AtlasOfficer.UNSEATED), new OfficerBoardLayout.Entry(3, "commander", 1, 0)));

        assertEquals(0, layout.seat(1).column());
        assertEquals(1, layout.seat(2).column());
        assertEquals(new OfficerBoardLayout.Seat(1, 2, 3), layout.seat(3));
    }

    @Test
    void anUnknownRankSitsInTheBottomRow() {
        OfficerBoardLayout layout = OfficerBoardLayout.of(RANKS, List.of(new OfficerBoardLayout.Entry(7, "retired_rank", 0, AtlasOfficer.UNSEATED)));

        assertEquals(2, layout.seat(7).row());
        assertEquals(2, layout.rowOfRank("retired_rank"));
    }

    @Test
    void theWidestRowCountsItsPlaces() {
        OfficerBoardLayout layout = OfficerBoardLayout.of(RANKS, List.of(new OfficerBoardLayout.Entry(1, "admiral", 0, 0)));

        assertEquals(4, layout.widestRow());
        assertFalse(layout.contains(9));
    }

    @Test
    void everySeatLinksToItsSuperior() {
        List<OfficerBoardLayout.Link> links = OfficerBoardLayout.of(RANKS, List.of()).links();

        assertEquals(6, links.size());
        assertTrue(links.contains(new OfficerBoardLayout.Link(new OfficerBoardLayout.Seat(2, 3, 4), new OfficerBoardLayout.Seat(1, 1, 2))));
        assertTrue(links.contains(new OfficerBoardLayout.Link(new OfficerBoardLayout.Seat(1, 1, 2), new OfficerBoardLayout.Seat(0, 0, 1))));
    }
}
