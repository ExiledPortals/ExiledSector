package exiledsector.skills.skilleffect;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WingScrambleTest {

    private record Wing(String name, String role, String type) {
    }

    private static final Wing BROADSWORDS_A = new Wing("broadswords_a", "fighter", "broadsword");
    private static final Wing BROADSWORDS_B = new Wing("broadswords_b", "fighter", "broadsword");
    private static final Wing ELITE = new Wing("elite", "fighter", "elite");
    private static final List<Wing> FIGHTER_GROUP = List.of(BROADSWORDS_A, BROADSWORDS_B, ELITE);

    private static Map<Wing, List<String>> freshLaunch() {
        return Map.of(
                BROADSWORDS_A, List.of("a1", "a2", "a3"),
                BROADSWORDS_B, List.of("b1", "b2", "b3"),
                ELITE, List.of("e1"));
    }

    private static WingScramble.Routing<String, Wing, String> route(List<Wing> group, Map<Wing, List<String>> members,
                                                                    Map<String, String> typeByFighter, int copiesPerType, boolean spawnMissing) {
        return WingScramble.route(group, members, typeByFighter, Wing::type, copiesPerType, spawnMissing);
    }

    @Test
    void wingsOnlyMixWithinTheirRoleAndALoneWingIsLeftAlone() {
        Wing daggers = new Wing("daggers", "bomber", "dagger");
        Wing tridents = new Wing("tridents", "bomber", "trident");
        Wing talons = new Wing("talons", "interceptor", "talon");

        List<List<Wing>> groups = WingScramble.roleGroups(List.of(daggers, talons, tridents), Wing::role);

        assertEquals(List.of(List.of(daggers, tridents)), groups);
    }

    @Test
    void identicalWingsShareOneTypeSoEachKeepsOneNativeAndNothingIsSpawned() {
        List<Wing> perditions = new ArrayList<>();
        Map<Wing, List<String>> members = new LinkedHashMap<>();
        for (int index = 0; index < 6; index++) {
            Wing wing = new Wing("perditions_" + index, "bomber", "perdition");
            perditions.add(wing);
            members.put(wing, List.of("p" + index + "_1", "p" + index + "_2"));
        }

        WingScramble.Routing<String, Wing, String> routing = route(perditions, members, Map.of(), 1, true);

        assertEquals(List.of(), routing.moves());
        assertEquals(List.of(), routing.spawns(), "a lone fighter type has nothing to copy");
        assertEquals(6, routing.discards().size(), routing.toString());
        assertTrue(routing.discards().stream().allMatch(fighter -> fighter.endsWith("_2")), "each wing keeps exactly one Perdition");
    }

    @Test
    void theFirstScrambleGivesEveryWingOneOfEachTypeAndSpawnsOnlyTheMissingType() {
        WingScramble.Routing<String, Wing, String> routing = route(FIGHTER_GROUP, freshLaunch(), Map.of(), 1, true);

        assertEquals(List.of(new WingScramble.Move<>("a2", ELITE)), routing.moves(), "a spare Broadsword fills the elite wing's copy slot");
        assertEquals(List.of(new WingScramble.Spawn<>(BROADSWORDS_A, "elite"), new WingScramble.Spawn<>(BROADSWORDS_B, "elite")),
                routing.spawns(), "only the elite type has to be copied, once per Broadsword wing");
        assertEquals(List.of("a3", "b2", "b3"), routing.discards(), "Broadswords beyond one per wing are scrapped");
    }

    @Test
    void aNativeTheBayLaunchesLaterRefillsACopySlotThatWasLostElsewhere() {
        Map<Wing, List<String>> members = new HashMap<>();
        members.put(BROADSWORDS_A, List.of("a1"));
        members.put(BROADSWORDS_B, List.of("b1", "eB", "b-new"));
        members.put(ELITE, List.of("e1"));
        Map<String, String> types = Map.of("eB", "elite");

        WingScramble.Routing<String, Wing, String> routing = route(FIGHTER_GROUP, members, types, 1, false);

        assertEquals(List.of(new WingScramble.Move<>("b-new", ELITE)), routing.moves(), "the elite wing lost its Broadsword copy");
        assertEquals(List.of(), routing.discards());
        assertEquals(List.of(), routing.spawns(), "after the first scramble the node never spawns, even for the lost elite copy");
    }

    @Test
    void aSurplusOfATypeFromAnotherWingRefillsAWingThatLostItsOwnNative() {
        Map<Wing, List<String>> members = Map.of(
                BROADSWORDS_A, List.of("eA"),
                BROADSWORDS_B, List.of("b1", "eB", "b-new"),
                ELITE, List.of("e1", "a2"));
        Map<String, String> types = Map.of("eA", "elite", "eB", "elite", "a2", "broadsword");

        WingScramble.Routing<String, Wing, String> routing = route(FIGHTER_GROUP, members, types, 1, false);

        assertEquals(List.of(new WingScramble.Move<>("b-new", BROADSWORDS_A)), routing.moves());
        assertEquals(List.of(), routing.discards());
    }

    @Test
    void aSurplusNativeNobodyNeedsIsRemovedSoEachWingKeepsOneOfEachType() {
        Map<Wing, List<String>> members = Map.of(
                BROADSWORDS_A, List.of("a1", "eA", "a-new"),
                BROADSWORDS_B, List.of("b1", "eB"),
                ELITE, List.of("e1", "a2"));
        Map<String, String> types = Map.of("eA", "elite", "eB", "elite", "a2", "broadsword");

        WingScramble.Routing<String, Wing, String> routing = route(FIGHTER_GROUP, members, types, 1, false);

        assertEquals(List.of(), routing.moves());
        assertEquals(List.of("a-new"), routing.discards());
    }

    @Test
    void theMagnitudeSetsHowManyCopiesOfEachOtherTypeAWingWants() {
        Wing first = new Wing("first", "fighter", "first_type");
        Wing second = new Wing("second", "fighter", "second_type");

        WingScramble.Routing<String, Wing, String> routing = route(List.of(first, second), Map.of(first, List.of("f1"), second,
                List.of("s1")), Map.of(), 3, true);

        assertEquals(6, routing.spawns().size());
    }

    @Test
    void theMagnitudeCountsTypesNotWings() {
        WingScramble.Routing<String, Wing, String> routing = route(FIGHTER_GROUP, freshLaunch(), Map.of(), 2, true);

        assertEquals(List.of(new WingScramble.Move<>("a2", ELITE), new WingScramble.Move<>("a3", ELITE)), routing.moves());
        assertEquals(List.of(new WingScramble.Spawn<>(BROADSWORDS_A, "elite"), new WingScramble.Spawn<>(BROADSWORDS_A, "elite"),
                new WingScramble.Spawn<>(BROADSWORDS_B, "elite"), new WingScramble.Spawn<>(BROADSWORDS_B, "elite")), routing.spawns());
        assertEquals(List.of("b2", "b3"), routing.discards());
    }
}
