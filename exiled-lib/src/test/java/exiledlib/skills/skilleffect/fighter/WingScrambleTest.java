package exiledlib.skills.skilleffect.fighter;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WingScrambleTest {

    private record Wing(String name, String group, String type, int wingSize) {
    }

    private static final Wing BROADSWORDS_A = new Wing("broadswords_a", "fighter", "broadsword", 3);
    private static final Wing BROADSWORDS_B = new Wing("broadswords_b", "fighter", "broadsword", 3);
    private static final Wing ELITE = new Wing("elite", "fighter", "elite", 1);

    private static WingScramble.Host<String, Wing, String> host(Wing wing, String... members) {
        return host(wing, 0, members);
    }

    private static WingScramble.Host<String, Wing, String> host(Wing wing, int extraAllowance, String... members) {
        return new WingScramble.Host<>(wing, wing.type(), wing.wingSize(), extraAllowance, List.of(members));
    }

    private static WingScramble.Routing<String, Wing, String> route(List<WingScramble.Host<String, Wing, String>> hosts,
                                                                    Map<String, String> typeByFighter, int copiesPerType) {
        return WingScramble.route(hosts, typeByFighter, fighter -> false, copiesPerType);
    }

    private static Map<Wing, Integer> headcountsAfter(List<WingScramble.Host<String, Wing, String>> hosts,
                                                      WingScramble.Routing<String, Wing, String> routing) {
        Map<String, Wing> wingByFighter = new HashMap<>();
        for (WingScramble.Host<String, Wing, String> host : hosts) {
            host.members().forEach(fighter -> wingByFighter.put(fighter, host.wing()));
        }
        routing.moves().forEach(move -> wingByFighter.put(move.fighter(), move.toWing()));
        routing.conversions().forEach(conversion -> wingByFighter.put(conversion.fighter(), conversion.hostWing()));
        routing.discards().forEach(wingByFighter::remove);
        Map<Wing, Integer> headcounts = new HashMap<>();
        wingByFighter.values().forEach(wing -> headcounts.merge(wing, 1, Integer::sum));
        return headcounts;
    }

    @Test
    void wingsOnlyMixWithinTheirGroupAndALoneOrUngroupedWingIsLeftAlone() {
        Wing daggers = new Wing("daggers", "bomber", "dagger", 3);
        Wing tridents = new Wing("tridents", "bomber", "trident", 2);
        Wing talons = new Wing("talons", "interceptor", "talon", 4);
        Wing supernovas = new Wing("supernovas", null, "supernova", 2);

        List<List<Wing>> groups = WingScramble.groups(List.of(daggers, talons, supernovas, tridents), Wing::group);

        assertEquals(List.of(List.of(daggers, tridents)), groups);
    }

    @Test
    void identicalWingsHaveNothingToTradeSoTheyStayAtFullStrength() {
        List<WingScramble.Host<String, Wing, String>> perditions = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            perditions.add(host(new Wing("perditions_" + index, "bomber", "perdition", 3), "p" + index + "_1", "p" + index + "_2", "p" + index + "_3"));
        }

        WingScramble.Routing<String, Wing, String> routing = route(perditions, Map.of(), 1);

        assertEquals(List.of(), routing.moves());
        assertEquals(List.of(), routing.conversions());
        assertEquals(List.of(), routing.discards(), "trimming identical wings would only make their bays rebuild the scrapped fighters");
    }

    @Test
    void theFirstScrambleRetrainsSpareNativesSoNoWingGainsOrLosesFighters() {
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(BROADSWORDS_A, "a1", "a2", "a3"), host(BROADSWORDS_B, "b1", "b2", "b3"),
                host(ELITE, "e1"));

        WingScramble.Routing<String, Wing, String> routing = route(hosts, Map.of(), 1);

        assertEquals(List.of(), routing.moves(), "nobody has a spare elite to hand over");
        assertEquals(List.of(new WingScramble.Conversion<>("a3", BROADSWORDS_A, "elite"), new WingScramble.Conversion<>("b3", BROADSWORDS_B, "elite")),
                routing.conversions(), "each Broadsword wing retrains its newest spare native into the elite type");
        assertEquals(List.of(), routing.discards());
        assertEquals(Map.of(BROADSWORDS_A, 3, BROADSWORDS_B, 3, ELITE, 1), headcountsAfter(hosts, routing));
    }

    @Test
    void wingsOfDifferentTypesSwapSpareNativesInsteadOfRetrainingThem() {
        Wing broadswords = new Wing("broadswords", "fighter", "broadsword", 3);
        Wing thunders = new Wing("thunders", "fighter", "thunder", 2);
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(broadswords, "b1", "b2", "b3"), host(thunders, "t1", "t2"));

        WingScramble.Routing<String, Wing, String> routing = route(hosts, Map.of(), 1);

        assertEquals(List.of(new WingScramble.Move<>("t2", broadswords), new WingScramble.Move<>("b3", thunders)), routing.moves());
        assertEquals(List.of(), routing.conversions());
        assertEquals(List.of(), routing.discards());
    }

    @Test
    void singleFighterWingsHaveNoRoomForCopiesSoDistinctHeaviesStayAsTheyAre() {
        List<WingScramble.Host<String, Wing, String>> heavies = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            heavies.add(host(new Wing("heavy_" + index, "fighter", "heavy_" + index, 1), "h" + index));
        }

        WingScramble.Routing<String, Wing, String> routing = route(heavies, Map.of(), 1);

        assertEquals(List.of(), routing.moves());
        assertEquals(List.of(), routing.conversions());
        assertEquals(List.of(), routing.discards());
    }

    @Test
    void whenTheWingIsTooSmallForEveryTypeTheCopyPlacesGoRoundRobinStartingAfterTheHostsOwnType() {
        List<String> types = List.of("a", "b", "c", "d");

        assertEquals(Map.of("a", 1, "b", 1), WingScramble.composition("a", types, 0, 2, 1));
        assertEquals(Map.of("c", 1, "d", 1), WingScramble.composition("c", types, 0, 2, 1));
        assertEquals(Map.of("d", 1, "a", 1), WingScramble.composition("d", types, 0, 2, 1));
        assertEquals(Map.of("a", 1, "c", 1), WingScramble.composition("a", types, 1, 2, 1), "a second wing of the same type starts one type later");
    }

    @Test
    void roundRobinNeverGivesAnyTypeMoreThanTheCopiesAndLeftoverPlacesStayNative() {
        Map<String, Integer> wanted = WingScramble.composition("a", List.of("a", "b"), 0, 6, 2);

        assertEquals(Map.of("a", 4, "b", 2), wanted);
        assertEquals(6, wanted.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void moreCopiesThanPlacesAreCappedAtTheNormalWingSize() {
        Map<String, Integer> wanted = WingScramble.composition("a", List.of("a", "b", "c"), 0, 3, 5);

        assertEquals(Map.of("a", 1, "b", 1, "c", 1), wanted);
    }

    @Test
    void fourDistinctTwoFighterWingsEachLendOnePilotSoEveryTypeIsCopiedOnce() {
        List<WingScramble.Host<String, Wing, String>> hosts = new ArrayList<>();
        for (String type : List.of("a", "b", "c", "d")) {
            hosts.add(host(new Wing(type, "fighter", type, 2), type + "1", type + "2"));
        }

        WingScramble.Routing<String, Wing, String> routing = route(hosts, Map.of(), 1);

        assertEquals(4, routing.moves().size(), routing.toString());
        assertEquals(Set.of("a2", "b2", "c2", "d2"), Set.copyOf(routing.moves().stream().map(WingScramble.Move::fighter).toList()));
        assertEquals(List.of(), routing.conversions());
        assertEquals(List.of(), routing.discards());
    }

    @Test
    void extraFightersFromTheBayAreKeptOnTopOfTheCap() {
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(BROADSWORDS_A, 3, "a1", "a2", "eA", "x1", "x2", "x3"), host(ELITE, "e1"));
        Map<String, String> types = Map.of("eA", "elite");

        WingScramble.Routing<String, Wing, String> routing = route(hosts, types, 1);

        assertEquals(List.of(), routing.discards(), "reserve deployment extras are not surplus");
        assertEquals(List.of(), routing.conversions());
    }

    @Test
    void nativesBeyondTheCapAndTheExtraAllowanceAreRemovedNewestLast() {
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(BROADSWORDS_A, 1, "a1", "a2", "eA", "x1", "x2"), host(ELITE, "e1"));
        Map<String, String> types = Map.of("eA", "elite");

        WingScramble.Routing<String, Wing, String> routing = route(hosts, types, 1);

        assertEquals(List.of("x1"), routing.discards());
    }

    @Test
    void aMissingCopyIsRetrainedFromASpareNativeBeforeAnyIsKeptAsAnExtra() {
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(BROADSWORDS_A, 1, "a1", "a2", "a-new"), host(ELITE, "e1"));

        WingScramble.Routing<String, Wing, String> routing = route(hosts, Map.of(), 1);

        assertEquals(List.of(new WingScramble.Conversion<>("a-new", BROADSWORDS_A, "elite")), routing.conversions());
        assertEquals(List.of(), routing.discards());
    }

    @Test
    void aLostCopyIsRetrainedFromTheNativeTheBayRebuilt() {
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(BROADSWORDS_A, "a1", "a2", "a-new"), host(BROADSWORDS_B, "b1", "b2", "eB"),
                host(ELITE, "e1"));
        Map<String, String> types = Map.of("eB", "elite");

        WingScramble.Routing<String, Wing, String> routing = route(hosts, types, 1);

        assertEquals(List.of(), routing.moves());
        assertEquals(List.of(new WingScramble.Conversion<>("a-new", BROADSWORDS_A, "elite")), routing.conversions());
        assertEquals(List.of(), routing.discards());
    }

    @Test
    void aLostCopyWaitsForItsOwnBayWhenNobodyHasASpareSoNothingIsSpawnedForFree() {
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(BROADSWORDS_A, "a1", "a2"), host(BROADSWORDS_B, "b1", "b2", "eB"),
                host(ELITE, "e1"));
        Map<String, String> types = Map.of("eB", "elite");

        WingScramble.Routing<String, Wing, String> routing = route(hosts, types, 1);

        assertEquals(List.of(), routing.moves());
        assertEquals(List.of(), routing.conversions());
        assertEquals(List.of(), routing.discards());
    }

    @Test
    void aSpareOfTheRightTypeElsewhereIsMovedRatherThanRetrained() {
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(BROADSWORDS_A, "a1", "a2"), host(BROADSWORDS_B, "b1", "b2", "eB"),
                host(ELITE, "e1", "e-new"));
        Map<String, String> types = Map.of("eB", "elite");

        WingScramble.Routing<String, Wing, String> routing = route(hosts, types, 1);

        assertEquals(List.of(new WingScramble.Move<>("e-new", BROADSWORDS_A)), routing.moves());
        assertEquals(List.of(), routing.conversions());
    }

    @Test
    void aWingThatLostItsOwnNativeGetsASpareNativeFromAWingOfTheSameType() {
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(BROADSWORDS_A, "a1", "eA"), host(BROADSWORDS_B, "b1", "b2", "eB", "b-new"),
                host(ELITE, "e1"));
        Map<String, String> types = Map.of("eA", "elite", "eB", "elite");

        WingScramble.Routing<String, Wing, String> routing = route(hosts, types, 1);

        assertEquals(List.of(new WingScramble.Move<>("b-new", BROADSWORDS_A)), routing.moves());
        assertEquals(List.of(), routing.discards());
    }

    @Test
    void pinnedFightersAreNeverMovedRetrainedOrRemoved() {
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(BROADSWORDS_A, "a1", "a2", "docked"), host(ELITE, "e1"));

        WingScramble.Routing<String, Wing, String> routing = WingScramble.route(hosts, Map.of(), "docked"::equals, 1);

        assertTrue(routing.moves().stream().noneMatch(move -> move.fighter().equals("docked")));
        assertTrue(routing.conversions().stream().noneMatch(conversion -> conversion.fighter().equals("docked")));
        assertEquals(List.of(new WingScramble.Conversion<>("a2", BROADSWORDS_A, "elite")), routing.conversions());
        assertEquals(List.of(), routing.discards());
    }

    @Test
    void theMagnitudeSetsHowManyOfEachOtherTypeAWingTakesWithinItsSize() {
        Wing first = new Wing("first", "fighter", "first_type", 4);
        Wing second = new Wing("second", "fighter", "second_type", 4);
        List<WingScramble.Host<String, Wing, String>> hosts = List.of(host(first, "f1", "f2", "f3", "f4"), host(second, "s1", "s2", "s3", "s4"));

        WingScramble.Routing<String, Wing, String> routing = route(hosts, Map.of(), 3);

        assertEquals(6, routing.moves().size(), routing.toString());
        assertEquals(Map.of(first, 4, second, 4), headcountsAfter(hosts, routing));
    }
}
