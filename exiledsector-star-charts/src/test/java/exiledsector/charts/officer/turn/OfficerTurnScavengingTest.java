package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.ScavengeRoll;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static exiledsector.charts.officer.turn.TurnBoard.SEED;
import static exiledsector.charts.officer.turn.TurnBoard.crowdedBoard;
import static exiledsector.charts.officer.turn.TurnBoard.freeSeat;
import static exiledsector.charts.officer.turn.TurnBoard.place;
import static exiledsector.charts.officer.turn.TurnBoard.scavengingSetup;
import static exiledsector.charts.officer.turn.TurnBoard.take;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerTurnScavengingTest {

    private static final int PUSHING_OFFICER = 1;
    private static final int PUSHED_OFFICER = 2;
    private static final int IDLE_OFFICER = 3;
    private static final int WAITING_OFFICER = 99;
    private static final CarriedItem LOOT = CarriedItem.of(RewardKind.ENGRAM, "engram_a");
    private static final ScavengeRoll LOOT_ROLL = (officer, random) -> LOOT;
    private static final Map<String, IntentWeights> ONLY_HOLD = Map.of("steady", new IntentWeights(0f, 0f, 0f, 0f, 1f));

    private static TurnRules rules(int fewest, int most, Map<String, IntentWeights> weights) {
        return new TurnRules(4, 6f, 50f, 6.25f, 10f, 1, 3, 2, 0, 50, 6, fewest, most, weights);
    }

    private static TurnResult scavengingTurn(AtlasState state, TurnRules rules, ScavengeRoll roll) {
        return take(state, TurnCause.forced(), scavengingSetup(rules, null, Set.of(), roll));
    }

    private static AtlasState pushAndBystander() {
        AtlasState state = new AtlasState(SEED);
        place(state, PUSHING_OFFICER, "hegemony", "commander", new AtlasCell(0, 0), new AtlasCell(1, 0));
        place(state, PUSHED_OFFICER, "pirates", "lieutenant", new AtlasCell(3, 0), new AtlasCell(2, 0));
        place(state, IDLE_OFFICER, "tritachyon", "lieutenant", new AtlasCell(-5, -5));
        return state;
    }

    @Test
    void onlyAnOfficerWhoHoldsAndIsNotTargetedScavenges() {
        AtlasState state = pushAndBystander();
        TurnRules rules = rules(3, 3, Map.of("commander", new IntentWeights(0f, 1f, 0f, 0f, 0f), "lieutenant", new IntentWeights(0f, 0f, 0f, 0f, 1f)));

        TurnResult result = scavengingTurn(state, rules, LOOT_ROLL);

        assertTrue(OfficerTurn.decodeIntents(state.declaredIntents()).stream()
                .anyMatch(intent -> intent.kind() == IntentKind.PUSH && intent.targets(PUSHED_OFFICER)));
        assertEquals(List.of(IDLE_OFFICER), result.scavengerRecordIds());
        assertEquals(List.of(LOOT), state.officer(IDLE_OFFICER).carried());
        assertFalse(state.officer(PUSHING_OFFICER).carried().contains(LOOT));
        assertFalse(state.officer(PUSHED_OFFICER).carried().contains(LOOT));
        assertEquals(result, TurnResult.decode(state.lastTurnResult()));
    }

    @Test
    void anOfficerWhoCannotActNeverScavenges() {
        AtlasState state = crowdedBoard(SEED);
        state.putOfficer(AtlasOfficer.waiting(WAITING_OFFICER, "pirates", "lieutenant", "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L), null)
                .seatedAt("lieutenant", freeSeat(state, "lieutenant")));
        int openRunOfficer = 1;

        TurnResult result = take(state, TurnCause.forced(), scavengingSetup(rules(100, 100, ONLY_HOLD), null, Set.of(openRunOfficer), LOOT_ROLL));

        Set<Integer> expected = new TreeSet<>();
        state.officers().stream().filter(AtlasOfficer::isActive).forEach(officer -> expected.add(officer.recordId()));
        expected.remove(openRunOfficer);
        assertEquals(expected, new TreeSet<>(result.scavengerRecordIds()));
        assertFalse(result.scavengerRecordIds().contains(WAITING_OFFICER));
    }

    @Test
    void noScavengerHasADeclaredIntentOrIsTargeted() {
        AtlasState state = crowdedBoard(SEED);
        int scavenges = 0;
        for (int turn = 0; turn < 20; turn++) {
            TurnResult result = scavengingTurn(state, OfficerTurnTables.DEFAULTS, LOOT_ROLL);
            List<Intent> declared = OfficerTurn.decodeIntents(state.declaredIntents());
            for (int recordId : result.scavengerRecordIds()) {
                assertTrue(declared.stream().noneMatch(intent -> intent.officerRecordId() == recordId || intent.targets(recordId)));
                scavenges++;
            }
        }
        assertTrue(scavenges > 0);
    }

    @Test
    void theCountIsRolledBetweenTheFewestAndTheMost() {
        AtlasState state = crowdedBoard(SEED);
        Set<Integer> counts = new HashSet<>();
        for (int turn = 0; turn < 30; turn++) {
            List<Integer> scavengers = scavengingTurn(state, rules(2, 3, ONLY_HOLD), LOOT_ROLL).scavengerRecordIds();
            assertEquals(scavengers.size(), new HashSet<>(scavengers).size());
            counts.add(scavengers.size());
        }
        assertEquals(Set.of(2, 3), counts);
    }

    @Test
    void everyIdleOfficerScavengesWhenFewerThanTheCountCanAct() {
        AtlasState state = new AtlasState(SEED);
        place(state, PUSHING_OFFICER, "hegemony", "commander", new AtlasCell(0, 0));
        place(state, PUSHED_OFFICER, "pirates", "lieutenant", new AtlasCell(4, 0));

        TurnResult result = scavengingTurn(state, rules(3, 3, ONLY_HOLD), LOOT_ROLL);

        assertEquals(Set.of(PUSHING_OFFICER, PUSHED_OFFICER), Set.copyOf(result.scavengerRecordIds()));
    }

    @Test
    void scavengingAddsTheRolledItemAndChangesNothingElse() {
        AtlasState looting = crowdedBoard(SEED);
        AtlasState quiet = crowdedBoard(SEED);
        for (int turn = 0; turn < 6; turn++) {
            TurnResult result = scavengingTurn(looting, OfficerTurnTables.DEFAULTS, LOOT_ROLL);
            scavengingTurn(quiet, OfficerTurnTables.DEFAULTS, ScavengeRoll.NONE);
            assertEquals(quiet.declaredIntents(), looting.declaredIntents());
            assertEquals(quiet.claims(), looting.claims());
            for (AtlasOfficer officer : quiet.officers()) {
                assertEquals(officer.renown(), looting.officer(officer.recordId()).renown());
                assertEquals(officer.rankId(), looting.officer(officer.recordId()).rankId());
            }
            if (turn == 0) {
                for (AtlasOfficer officer : quiet.officers()) {
                    List<CarriedItem> expected = new ArrayList<>(officer.carried());
                    if (result.scavengerRecordIds().contains(officer.recordId())) {
                        expected.add(LOOT);
                    }
                    assertEquals(expected, looting.officer(officer.recordId()).carried());
                }
            }
        }
    }

    @Test
    void theSameStateSeedAndCounterScavengeTheSame() {
        ScavengeRoll countedRoll = (officer, random) -> new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 1 + random.nextInt(5));
        AtlasState first = crowdedBoard(SEED);
        AtlasState second = crowdedBoard(SEED);
        int scavenges = 0;
        for (int turn = 0; turn < 8; turn++) {
            TurnResult firstResult = scavengingTurn(first, OfficerTurnTables.DEFAULTS, countedRoll);
            TurnResult secondResult = scavengingTurn(second, OfficerTurnTables.DEFAULTS, countedRoll);
            assertEquals(firstResult.encode(), secondResult.encode());
            scavenges += firstResult.scavengerRecordIds().size();
        }
        assertEquals(first.officers(), second.officers());
        assertTrue(scavenges > 0);
    }
}
