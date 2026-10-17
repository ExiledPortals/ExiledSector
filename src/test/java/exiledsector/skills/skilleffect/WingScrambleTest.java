package exiledsector.skills.skilleffect;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WingScrambleTest {

    private record Wing(String name, String role) {
    }

    private static final Wing BROADSWORDS_A = new Wing("broadswords_a", "fighter");
    private static final Wing BROADSWORDS_B = new Wing("broadswords_b", "fighter");
    private static final Wing ELITE = new Wing("elite", "fighter");
    private static final List<Wing> FIGHTER_GROUP = List.of(BROADSWORDS_A, BROADSWORDS_B, ELITE);

    private static Map<Wing, List<String>> freshLaunch() {
        return Map.of(
                BROADSWORDS_A, List.of("a1", "a2", "a3"),
                BROADSWORDS_B, List.of("b1", "b2", "b3"),
                ELITE, List.of("e1"));
    }

    @Test
    void wingsOnlyMixWithinTheirRoleAndALoneWingIsLeftAlone() {
        Wing daggers = new Wing("daggers", "bomber");
        Wing tridents = new Wing("tridents", "bomber");
        Wing talons = new Wing("talons", "interceptor");

        List<List<Wing>> groups = WingScramble.roleGroups(List.of(daggers, talons, tridents), Wing::role);

        assertEquals(List.of(List.of(daggers, tridents)), groups);
    }

    @Test
    void theFirstScrambleKeepsOneNativePerWingAndUsesTheRestAsCopiesBeforeSpawningAny() {
        WingScramble.Routing<String, Wing> routing = WingScramble.route(FIGHTER_GROUP, freshLaunch(), Map.of(), 1, true);

        assertEquals(4, routing.moves().size(), routing.toString());
        assertTrue(routing.moves().stream().allMatch(move -> move.fighter().startsWith("a") || move.fighter().startsWith("b")));
        assertEquals(List.of(), routing.discards(), "every spare Broadsword fills a copy slot");
        assertEquals(List.of(new WingScramble.Spawn<>(BROADSWORDS_A, ELITE), new WingScramble.Spawn<>(BROADSWORDS_B, ELITE)), routing.spawns(),
                "only the one-fighter elite has to be copied");
    }

    @Test
    void aNativeTheBayLaunchesLaterRefillsACopySlotThatWasLostElsewhere() {
        Map<Wing, List<String>> members = new HashMap<>();
        members.put(BROADSWORDS_A, List.of("a1", "b2", "e2"));
        members.put(BROADSWORDS_B, List.of("b1", "a2", "e3", "b-new"));
        members.put(ELITE, List.of("e1", "a3"));
        Map<String, Wing> donors = Map.of("b2", BROADSWORDS_B, "e2", ELITE, "a2", BROADSWORDS_A, "e3", ELITE, "a3", BROADSWORDS_A);

        WingScramble.Routing<String, Wing> routing = WingScramble.route(FIGHTER_GROUP, members, donors, 1, false);

        assertEquals(List.of(new WingScramble.Move<>("b-new", ELITE)), routing.moves(), "the elite wing lost its Broadsword copy");
        assertEquals(List.of(), routing.discards());
        assertEquals(List.of(), routing.spawns(), "after the first scramble the node never spawns");
    }

    @Test
    void aSurplusNativeNobodyNeedsIsRemovedSoEachWingKeepsOneOfItsOwnType() {
        Map<Wing, List<String>> members = Map.of(
                BROADSWORDS_A, List.of("a1", "b2", "e2", "a-new"),
                BROADSWORDS_B, List.of("b1", "a2", "e3"),
                ELITE, List.of("e1", "a3", "b3"));
        Map<String, Wing> donors = Map.of("b2", BROADSWORDS_B, "e2", ELITE, "a2", BROADSWORDS_A, "e3", ELITE, "a3", BROADSWORDS_A, "b3",
                BROADSWORDS_B);

        WingScramble.Routing<String, Wing> routing = WingScramble.route(FIGHTER_GROUP, members, donors, 1, false);

        assertEquals(List.of(), routing.moves());
        assertEquals(List.of("a-new"), routing.discards());
    }

    @Test
    void theMagnitudeSetsHowManyCopiesOfEachOtherWingAHostWants() {
        Wing first = new Wing("first", "fighter");
        Wing second = new Wing("second", "fighter");

        WingScramble.Routing<String, Wing> routing = WingScramble.route(List.of(first, second), Map.of(first, List.of("f1"), second,
                List.of("s1")), Map.of(), 3, true);

        assertEquals(6, routing.spawns().size());
    }
}
