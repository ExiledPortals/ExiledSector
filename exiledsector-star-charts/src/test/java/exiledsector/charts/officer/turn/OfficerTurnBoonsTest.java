package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.ScavengeRoll;
import exiledsector.charts.officer.StartingItemRoll;
import exiledsector.charts.officer.TrophyRefit;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static exiledsector.charts.officer.turn.TurnBoard.ATTACKER_WINS;
import static exiledsector.charts.officer.turn.TurnBoard.SEED;
import static exiledsector.charts.officer.turn.TurnBoard.declare;
import static exiledsector.charts.officer.turn.TurnBoard.place;
import static exiledsector.charts.officer.turn.TurnBoard.take;
import static exiledsector.charts.officer.turn.TurnBoard.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerTurnBoonsTest {

    private static final int HEGEMONY_OFFICER = 1;
    private static final int PIRATE_OFFICER = 2;
    private static final CarriedItem KERNEL = CarriedItem.of(RewardKind.CRAFTING_ITEM, "kernel");
    private static final CarriedItem WARDS = new CarriedItem(RewardKind.CRAFTING_ITEM, "ward", null, 2);
    private static final CarriedItem REFITTED = CarriedItem.of(RewardKind.ENGRAM, "engram_a");
    private static final OfficerBoons CERTAIN_TROPHIES = new OfficerBoons(100f, 0f, false, 0f, 0f, false);

    private static AtlasState challengedBoard() {
        AtlasState state = new AtlasState(SEED);
        place(state, HEGEMONY_OFFICER, "hegemony", "commander", new AtlasCell(0, 0), new AtlasCell(1, 0));
        place(state, PIRATE_OFFICER, "pirates", "lieutenant", new AtlasCell(3, 0), new AtlasCell(2, 0));
        update(state, HEGEMONY_OFFICER, officer -> officer.withCarried(List.of(KERNEL)));
        update(state, PIRATE_OFFICER, officer -> officer.withCarried(List.of(WARDS)));
        declare(state, Intent.challenge(HEGEMONY_OFFICER, PIRATE_OFFICER));
        return state;
    }

    private static TurnSetup boosted(TurnRules rules, ScavengeRoll scavenges, OfficerBoons boons, TrophyRefit refits) {
        return new TurnSetup(rules, OfficerRanks.all(), IntentResolvers.ABSTRACT, null, Set.of(), FlagshipCatalogue.NONE, StartingItemRoll.NONE,
                scavenges, boons, refits);
    }

    @Test
    void trophyDuplicationDuplicatesOnlyTheItemsTaken() {
        AtlasState state = challengedBoard();

        take(state, TurnCause.forced(), boosted(ATTACKER_WINS, ScavengeRoll.NONE, CERTAIN_TROPHIES, TrophyRefit.NONE));

        assertEquals(List.of(KERNEL, WARDS.withCount(4)), state.officer(HEGEMONY_OFFICER).carried());
    }

    @Test
    void trophyRefitRerollsTheWinnersWholeJoinedStack() {
        AtlasState state = challengedBoard();
        List<List<CarriedItem>> refitInputs = new ArrayList<>();
        TrophyRefit refits = (officer, random) -> {
            refitInputs.add(officer.carried());
            return List.of(REFITTED);
        };
        OfficerBoons boons = new OfficerBoons(0f, 0f, true, 0f, 0f, false);

        take(state, TurnCause.forced(), boosted(ATTACKER_WINS, ScavengeRoll.NONE, boons, refits));

        assertEquals(List.of(List.of(KERNEL, WARDS)), refitInputs);
        assertEquals(List.of(REFITTED), state.officer(HEGEMONY_OFFICER).carried());
        assertTrue(state.officer(PIRATE_OFFICER).carried().isEmpty());
    }

    @Test
    void aSkirmishWinnerTakesNothingFromALoserThePlayerBeatAndTheLoserKeepsWhatItHeld() {
        AtlasState state = challengedBoard();
        state.decideContest(Intent.challenge(HEGEMONY_OFFICER, PIRATE_OFFICER).encode(), true, true);

        take(state, TurnCause.forced(), boosted(ATTACKER_WINS, ScavengeRoll.NONE, CERTAIN_TROPHIES, TrophyRefit.NONE));

        assertEquals(List.of(KERNEL), state.officer(HEGEMONY_OFFICER).carried());
        assertEquals(List.of(WARDS), state.officer(PIRATE_OFFICER).carried());
    }

    @Test
    void aSkirmishLoserThePlayerBeatWithNothingLeftIsDealtAFreshItem() {
        AtlasState state = challengedBoard();
        update(state, PIRATE_OFFICER, officer -> officer.withCarried(List.of()));
        state.decideContest(Intent.challenge(HEGEMONY_OFFICER, PIRATE_OFFICER).encode(), true, true);
        TurnSetup setup = new TurnSetup(ATTACKER_WINS, OfficerRanks.all(), IntentResolvers.ABSTRACT, null, Set.of(), FlagshipCatalogue.NONE,
                (officer, random) -> REFITTED, ScavengeRoll.NONE, OfficerBoons.NONE, TrophyRefit.NONE);

        take(state, TurnCause.forced(), setup);

        assertEquals(List.of(KERNEL), state.officer(HEGEMONY_OFFICER).carried());
        assertEquals(List.of(REFITTED), state.officer(PIRATE_OFFICER).carried());
    }

    @Test
    void aSkirmishWinnerStillTakesFromALoserItsRivalBeat() {
        AtlasState state = challengedBoard();
        state.decideContest(Intent.challenge(HEGEMONY_OFFICER, PIRATE_OFFICER).encode(), true, false);

        take(state, TurnCause.forced(), boosted(ATTACKER_WINS, ScavengeRoll.NONE, OfficerBoons.NONE, TrophyRefit.NONE));

        assertEquals(List.of(KERNEL, WARDS), state.officer(HEGEMONY_OFFICER).carried());
        assertTrue(state.officer(PIRATE_OFFICER).carried().isEmpty());
    }

    @Test
    void scavengeDuplicationDuplicatesTheScavengedItem() {
        AtlasState state = new AtlasState(SEED);
        place(state, HEGEMONY_OFFICER, "hegemony", "commander", new AtlasCell(-5, -5));
        TurnRules onlyHold = new TurnRules(4, 6f, 50f, 6.25f, 10f, 1, 3, 2, 0, 50, 6, 1, 1,
                Map.of("commander", new IntentWeights(0f, 0f, 0f, 0f, 1f)));
        OfficerBoons boons = new OfficerBoons(0f, 100f, false, 0f, 0f, false);

        take(state, TurnCause.forced(), boosted(onlyHold, (officer, random) -> WARDS, boons, TrophyRefit.NONE));

        assertEquals(List.of(WARDS.withCount(4)), state.officer(HEGEMONY_OFFICER).carried());
    }
}
