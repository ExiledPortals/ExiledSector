package exiledsector.charts.officer;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

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
                List.of("hull_item:1:bridge_relay:abyssal_rift", "crafting_item:4:kernel"), 3, true,
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
    void carriedRewardsOfAnUnknownKindAreSkippedWhenDecoding() {
        AtlasOfficer officer = new AtlasOfficer(12, "tritachyon", 1L, -2L, "admiral", CENTRE, "reckless", "paragon", "paragon_Elite", 3L, -4L,
                List.of(), Map.of(), List.of("crafting_item:7:kernel", "subroutine:1:old_subroutine", "hull_item:2:bridge"), 0, false, AtlasOfficer.Status.ACTIVE, true, List.of(), 40, true, 5,
                "graphics/portraits/portrait_mercenary01.png", Map.of());

        AtlasOfficer decoded = AtlasOfficer.decode(12, officer.encode());

        assertEquals(List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 7), new CarriedItem(RewardKind.HULL_ITEM, "bridge", null, 2)),
                decoded.carried());
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
                List.of("item_x=5", "crafting_item:2:kernel"), 0, false, AtlasOfficer.Status.WAITING, false,
                List.of(), 0, false, AtlasOfficer.UNSEATED, "", Map.of());

        assertEquals(List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 2)), officer.carried());
    }

    @Test
    void scarsDModsAndCarriedItemsChangeOnlyThemselves() {
        AtlasOfficer officer = waiting().takingCommand(CENTRE, "arrived:1");
        List<CarriedItem> carried = List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 3));

        AtlasOfficer changed = officer.withScars(Map.of("veteran", 2)).withDModIds(List.of("faulty_grid")).withCarried(carried);

        assertEquals(Map.of("veteran", 2), changed.scars());
        assertEquals(List.of("faulty_grid"), changed.dmodIds());
        assertEquals(carried, changed.carried());
        assertEquals(officer.history(), changed.history());
        assertEquals(CENTRE, changed.commandCentre());
        assertEquals(changed, AtlasOfficer.decode(changed.recordId(), changed.encode()));
    }

    private static AtlasOfficer fullyFilled() {
        return new AtlasOfficer(12, "tritachyon", 1L, -2L, "admiral", CENTRE, "reckless", "paragon", "paragon_Elite", 3L, -4L,
                List.of("degraded_engines", "faulty_grid"), Map.of("scar_a", 2), List.of("crafting_item:4:kernel"), 3, false, AtlasOfficer.Status.ACTIVE, true, List.of("arrived:1"), 40, false, 5, "portrait.png", Map.of("faulty_grid", "scar_a"));
    }

    static Stream<Arguments> copyMethods() {
        List<CarriedItem> carried = List.of(new CarriedItem(RewardKind.HULL_ITEM, "bridge", null, 2));
        return Stream.of(
                copy("withRenown", officer -> officer.withRenown(7), "renown"),
                copy("withLastChance", officer -> officer.withLastChance(true), "lastChance"),
                copy("withDefeats", officer -> officer.withDefeats(9), "defeats"),
                copy("withHistory", officer -> officer.withHistory("moved:1:2"), "history"),
                copy("withScars", officer -> officer.withScars(Map.of("scar_b", 1)), "scars"),
                copy("withDModIds", officer -> officer.withDModIds(List.of("degraded_engines", "comp_armor")), "dmodIds", "dmodScars"),
                copy("withScarDMod", officer -> officer.withScarDMod("comp_armor", "rammed"), "dmodIds", "dmodScars"),
                copy("withCarried", officer -> officer.withCarried(carried), "carriedItems"),
                copy("withPortrait", officer -> officer.withPortrait("other.png"), "portraitPath"),
                copy("seatedAt", officer -> officer.seatedAt("captain", 2), "rankId", "seat"),
                copy("identifiedNow", AtlasOfficer::identifiedNow, "identified"));
    }

    private static Arguments copy(String name, UnaryOperator<AtlasOfficer> method, String... changedComponents) {
        return Arguments.of(name, method, Set.of(changedComponents));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("copyMethods")
    void eachCopyMethodChangesOnlyItsOwnFields(String name, UnaryOperator<AtlasOfficer> method, Set<String> changedComponents) throws Exception {
        AtlasOfficer before = fullyFilled();
        AtlasOfficer after = method.apply(before);

        Set<String> actuallyChanged = new TreeSet<>();
        for (RecordComponent component : AtlasOfficer.class.getRecordComponents()) {
            if (!Objects.equals(component.getAccessor().invoke(before), component.getAccessor().invoke(after))) {
                actuallyChanged.add(component.getName());
            }
        }

        assertEquals(new TreeSet<>(changedComponents), actuallyChanged);
    }
}
