package exiledsector.charts.officer;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
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
                List.of("degraded_engines", "faulty_grid"), Map.of("scar_a", 2, "scar_b", 1),
                List.of("hull_item:1:bridge_relay:abyssal_rift", "crafting_item:4:kernel"), List.of("engram:1:engram_a", "unique:1:u_x"), 3, true,
                AtlasOfficer.Status.ACTIVE, true, List.of("arrived:1", "moved:1:2"), 40, true, 5,
                "graphics/portraits/portrait_mercenary01.png", Map.of("faulty_grid", "scar_a"));

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
        assertNull(AtlasOfficer.decode(1, encoded.replaceFirst(AtlasOfficer.FORMAT, "o9")));
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

        AtlasOfficer promoted = officer.seatedAt("captain", 3);
        assertEquals("captain", promoted.rankId());
        assertEquals(3, promoted.seat());
        assertEquals(officer.history(), promoted.history());
        assertEquals(CENTRE, promoted.commandCentre());

        AtlasOfficer identified = officer.identifiedNow();
        assertTrue(identified.identified());
        assertEquals(officer.rankId(), identified.rankId());
    }

    @Test
    void aRecordWrittenBeforeRewardsAndScarsStillDecodes() {
        String oldRecord = "o1|hegemony|1|2|commander||steady|onslaught|onslaught_Elite|3|4|||||0|0|WAITING|0||0|0";

        AtlasOfficer officer = AtlasOfficer.decode(5, oldRecord);

        assertEquals(List.of(), officer.carried());
        assertEquals(List.of(), officer.pendingOffers());
        assertEquals(Map.of(), officer.scars());
        assertEquals(List.of(), officer.dmodIds());
        assertFalse(officer.hasPendingChoice());
        assertEquals(AtlasOfficer.UNSEATED, officer.seat());
        assertFalse(officer.isSeated());
        assertFalse(officer.hasPortrait());
    }

    @Test
    void carriedRewardsOfAnUnknownKindAreSkippedWhenDecoding() {
        AtlasOfficer officer = new AtlasOfficer(12, "tritachyon", 1L, -2L, "admiral", CENTRE, "reckless", "paragon", "paragon_Elite", 3L, -4L,
                List.of(), Map.of(), List.of("crafting_item:7:kernel", "subroutine:1:old_subroutine", "hull_item:2:bridge"),
                List.of("relic:1:gone", "engram:1:engram_a"), 0, false, AtlasOfficer.Status.ACTIVE, true, List.of(), 40, true, 5,
                "graphics/portraits/portrait_mercenary01.png", Map.of());

        AtlasOfficer decoded = AtlasOfficer.decode(12, officer.encode());

        assertEquals(List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 7), new CarriedItem(RewardKind.HULL_ITEM, "bridge", null, 2)),
                decoded.carried());
        assertEquals(List.of(CarriedItem.of(RewardKind.ENGRAM, "engram_a")), decoded.pendingOffers());
    }

    @Test
    void aRecordWrittenBeforeStoredPortraitsStillDecodes() {
        String oldRecord = "o2|hegemony|1|2|commander||steady|onslaught|onslaught_Elite|3|4|||||0|0|WAITING|0||0|0|4";

        AtlasOfficer officer = AtlasOfficer.decode(5, oldRecord);

        assertEquals(4, officer.seat());
        assertFalse(officer.hasPortrait());
    }

    @Test
    void aRecordWrittenBeforeDModOriginsStillDecodes() {
        String oldRecord = "o3|hegemony|1|2|commander||steady|onslaught|onslaught_Elite|3|4|faulty_grid|humbled=1|||0|0|WAITING|0||0|0|4|portrait.png";

        AtlasOfficer officer = AtlasOfficer.decode(5, oldRecord);

        assertEquals(List.of("faulty_grid"), officer.dmodIds());
        assertEquals("portrait.png", officer.portraitPath());
        assertEquals(Map.of(), officer.dmodScars());
    }

    @Test
    void eachScarDModRemembersItsScarUntilTheDModIsLost() {
        AtlasOfficer scarred = waiting().withScarDMod("faulty_grid", "humbled").withScarDMod("comp_armor", "rammed");

        assertEquals(List.of("faulty_grid", "comp_armor"), scarred.dmodIds());
        assertEquals(Map.of("faulty_grid", "humbled", "comp_armor", "rammed"), scarred.dmodScars());
        assertEquals(scarred, AtlasOfficer.decode(scarred.recordId(), scarred.encode()));
        assertEquals(scarred, scarred.withScarDMod("faulty_grid", "rammed"));
        assertEquals(Map.of("comp_armor", "rammed"), scarred.withDModIds(List.of("comp_armor")).dmodScars());
        assertEquals(Map.of("faulty_grid", "humbled"),
                scarred.flying(new FlagshipCatalogue.Flagship("eagle", "eagle_Assault"), List.of("faulty_grid")).dmodScars());
    }

    @Test
    void aPortraitStaysWithTheOfficerThroughCommandChanges() {
        AtlasOfficer portrayed = waiting().withPortrait("graphics/portraits/portrait_hegemony01.png");

        AtlasOfficer moved = portrayed.takingCommand(CENTRE, "arrived:1").losingCommand("fled:1").seatedAt("admiral", 2);

        assertEquals("graphics/portraits/portrait_hegemony01.png", moved.portraitPath());
        assertEquals(moved, AtlasOfficer.decode(moved.recordId(), moved.encode()));
        assertFalse(waiting().withPortrait("bad|path").hasPortrait());
    }

    @Test
    void unreadableItemTokensAreSkipped() {
        AtlasOfficer officer = new AtlasOfficer(3, "hegemony", 1L, 2L, "commander", null, "steady", "", "", 3L, 4L, List.of(), Map.of(),
                List.of("item_x=5", "crafting_item:2:kernel"), List.of("rank_swap"), 0, false, AtlasOfficer.Status.WAITING, false,
                List.of(), 0, false, AtlasOfficer.UNSEATED, "", Map.of());

        assertEquals(List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 2)), officer.carried());
        assertFalse(officer.hasPendingChoice());
    }

    @Test
    void scarsDModsCarriedItemsAndOffersChangeOnlyThemselves() {
        AtlasOfficer officer = waiting().takingCommand(CENTRE, "arrived:1");
        List<CarriedItem> carried = List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 3));
        List<CarriedItem> offers = List.of(CarriedItem.of(RewardKind.ENGRAM, "engram_a"), CarriedItem.of(RewardKind.UNIQUE, "u_x"));

        AtlasOfficer changed = officer.withScars(Map.of("veteran", 2)).withDModIds(List.of("faulty_grid")).withCarried(carried)
                .withPendingOffers(offers);

        assertEquals(Map.of("veteran", 2), changed.scars());
        assertEquals(List.of("faulty_grid"), changed.dmodIds());
        assertEquals(carried, changed.carried());
        assertEquals(offers, changed.pendingOffers());
        assertTrue(changed.hasPendingChoice());
        assertEquals(officer.history(), changed.history());
        assertEquals(CENTRE, changed.commandCentre());
        assertEquals(changed, AtlasOfficer.decode(changed.recordId(), changed.encode()));
        assertFalse(changed.withPendingOffers(List.of()).hasPendingChoice());
    }
}
