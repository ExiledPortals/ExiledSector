package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OfficerBoardLayoutTest {

    private static final List<String> RANKS = List.of("lieutenant", "commander", "admiral");

    @Test
    void theHighestRankIsTheTopRow() {
        OfficerBoardLayout layout = OfficerBoardLayout.of(RANKS, List.of());

        assertEquals(3, layout.rowCount());
        assertEquals("admiral", layout.rankIdOfRow(0));
        assertEquals("lieutenant", layout.rankIdOfRow(2));
    }

    @Test
    void aRowSortsByRenownThenRecordId() {
        OfficerBoardLayout layout = OfficerBoardLayout.of(RANKS, List.of(new OfficerBoardLayout.Entry(4, "commander", 1),
                new OfficerBoardLayout.Entry(2, "commander", 5), new OfficerBoardLayout.Entry(3, "commander", 1)));

        assertEquals(List.of(2, 3, 4), layout.row(1));
        assertEquals(new OfficerBoardLayout.Seat(1, 2, 3), layout.seat(4));
    }

    @Test
    void anUnknownRankSitsInTheBottomRow() {
        OfficerBoardLayout layout = OfficerBoardLayout.of(RANKS, List.of(new OfficerBoardLayout.Entry(7, "retired_rank", 0)));

        assertEquals(2, layout.seat(7).row());
        assertEquals(2, layout.rowOfRank("retired_rank"));
    }

    @Test
    void theWidestRowCountsItsOfficers() {
        OfficerBoardLayout layout = OfficerBoardLayout.of(RANKS, List.of(new OfficerBoardLayout.Entry(1, "admiral", 0),
                new OfficerBoardLayout.Entry(2, "lieutenant", 0), new OfficerBoardLayout.Entry(3, "lieutenant", 0)));

        assertEquals(2, layout.widestRow());
        assertFalse(layout.contains(9));
    }
}
