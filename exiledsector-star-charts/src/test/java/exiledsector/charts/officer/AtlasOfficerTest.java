package exiledsector.charts.officer;

import exiledsector.charts.atlas.AtlasCell;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasOfficerTest {

    private static final AtlasCell CENTRE = new AtlasCell(4, -7);

    private static AtlasOfficer waiting() {
        return AtlasOfficer.waiting(7, "hegemony", "commander", "steady", new AtlasOfficer.OfficerSeeds(-11L, 22L, 33L, Long.MIN_VALUE),
                new FlagshipCatalogue.Flagship("onslaught", "onslaught_Elite"));
    }

    @Test
    void aFullyFilledOfficerSurvivesEncodingAndDecoding() {
        AtlasOfficer officer = new AtlasOfficer(12, "tritachyon", 1L, -2L, "admiral", CENTRE, "reckless", "paragon", "paragon_Elite", 3L, -4L,
                List.of("degraded_engines", "faulty_grid"), Map.of("scar_a", 2, "scar_b", 1), Map.of("item_x", 5), "rank_swap", 3, true,
                AtlasOfficer.Status.ACTIVE, true, List.of("arrived:1", "moved:1:2"), 40, true);

        assertEquals(officer, AtlasOfficer.decode(12, officer.encode()));
    }

    @Test
    void aWaitingOfficerSurvivesEncodingAndDecoding() {
        AtlasOfficer officer = waiting();

        assertEquals(officer, AtlasOfficer.decode(officer.recordId(), officer.encode()));
    }

    @Test
    void textThatIsNotAnOfficerRecordDecodesToNothing() {
        String encoded = waiting().encode();

        assertNull(AtlasOfficer.decode(1, null));
        assertNull(AtlasOfficer.decode(1, ""));
        assertNull(AtlasOfficer.decode(1, encoded.replaceFirst("o1", "o9")));
        assertNull(AtlasOfficer.decode(1, encoded + "|extra"));
        assertNull(AtlasOfficer.decode(1, encoded.replace("WAITING", "LOST")));
    }

    @Test
    void anOfficerKeepsOnlyItsLatestHistoryEntries() {
        AtlasOfficer officer = waiting();
        for (int entry = 0; entry < AtlasOfficer.HISTORY_LENGTH + 2; entry++) {
            officer = officer.takingCommand(CENTRE, "arrived:" + entry);
        }

        assertEquals(List.of("arrived:2", "arrived:3", "arrived:4"), officer.history());
    }

    @Test
    void anOfficerMovesFromWaitingToCommandingToFled() {
        AtlasOfficer officer = waiting();
        assertTrue(officer.neverPlaced());
        assertFalse(officer.isActive());
        assertTrue(officer.hasFlagship());

        AtlasOfficer commanding = officer.takingCommand(CENTRE, "arrived:" + CENTRE.pack());
        assertTrue(commanding.isActive());
        assertEquals(CENTRE, commanding.commandCentre());
        assertFalse(commanding.neverPlaced());

        AtlasOfficer fled = commanding.losingCommand("fled:" + CENTRE.pack());
        assertTrue(fled.hasFled());
        assertFalse(fled.isActive());
        assertNull(fled.commandCentre());
        assertEquals(2, fled.history().size());
    }

    @Test
    void rankAndIdentificationChangeOnlyThemselves() {
        AtlasOfficer officer = waiting().takingCommand(CENTRE, "arrived:1");

        AtlasOfficer promoted = officer.withRank("captain");
        assertEquals("captain", promoted.rankId());
        assertEquals(officer.history(), promoted.history());
        assertEquals(CENTRE, promoted.commandCentre());

        AtlasOfficer identified = officer.identifiedNow();
        assertTrue(identified.identified());
        assertEquals(officer.rankId(), identified.rankId());
    }
}
