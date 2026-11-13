package exiledsector.charts.officer;

import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.charts.officer.scar.ScarTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerDefeatTest {

    private static final CarriedItem KERNELS = new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 2);
    private static final CarriedItem ENGRAM = CarriedItem.of(RewardKind.ENGRAM, "engram_a");
    private static final CarriedItem UNIQUE = CarriedItem.of(RewardKind.UNIQUE, "u_x");

    private static AtlasOfficer officer(int recordId) {
        return AtlasOfficer.waiting(recordId, "hegemony", "commander", "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L), null);
    }

    @Test
    void aDefeatAddsOneScarStackAndItsDMod() {
        AtlasOfficer officer = officer(1).withScars(Map.of("veteran", 1)).withDModIds(List.of("faulty_grid"));

        AtlasOfficer scarred = OfficerDefeat.scarred(officer, "veteran", "degraded_engines");

        assertEquals(Map.of("veteran", 2), scarred.scars());
        assertEquals(List.of("faulty_grid", "degraded_engines"), scarred.dmodIds());
        assertEquals(Map.of("degraded_engines", "veteran"), scarred.dmodScars());
    }

    @Test
    void aDefeatNeverRepeatsAHeldDModOrAddsAMissingOne() {
        AtlasOfficer officer = officer(1).withDModIds(List.of("faulty_grid"));

        assertEquals(List.of("faulty_grid"), OfficerDefeat.scarred(officer, "veteran", "faulty_grid").dmodIds());
        assertEquals(List.of("faulty_grid"), OfficerDefeat.scarred(officer, "veteran", null).dmodIds());
        assertEquals(Map.of("veteran", 1), OfficerDefeat.scarred(officer, "veteran", null).scars());
    }

    @Test
    void aRewardedOfficerDropsWhatItCarriedAndHoldsTheOffers() {
        AtlasOfficer officer = officer(1).withCarried(List.of(KERNELS));

        AtlasOfficer rewarded = OfficerDefeat.rewarded(officer, List.of(ENGRAM, UNIQUE));

        assertEquals(List.of(), rewarded.carried());
        assertEquals(List.of(ENGRAM, UNIQUE), rewarded.pendingOffers());
    }

    @Test
    void thePickedOfferIsCarriedAndTheChoiceIsCleared() {
        AtlasOfficer waitingForPick = officer(1).withCarried(List.of(KERNELS)).withPendingOffers(List.of(ENGRAM, UNIQUE));

        AtlasOfficer picked = OfficerDefeat.picked(waitingForPick, UNIQUE);

        assertEquals(List.of(KERNELS, UNIQUE), picked.carried());
        assertFalse(picked.hasPendingChoice());
    }

    @Test
    void aPickThatWasNotOfferedChangesNothing() {
        AtlasOfficer waitingForPick = officer(1).withPendingOffers(List.of(ENGRAM));

        assertSame(waitingForPick, OfficerDefeat.picked(waitingForPick, UNIQUE));
        assertSame(waitingForPick, OfficerDefeat.picked(waitingForPick, null));
    }

    @Test
    void anOfficerThatLosesAChallengeIsScarredAndStored() {
        AtlasState state = new AtlasState(9L);
        state.putOfficer(officer(4).withCarried(List.of(KERNELS)));

        AtlasOfficer loser = OfficerDefeat.challengeLost(state, 4, "faction_with_no_focus", new Random(1L));

        assertEquals(Map.of(ScarTable.FALLBACK_SCAR_ID, 1), loser.scars());
        assertEquals(List.of(KERNELS), loser.carried());
        assertFalse(loser.hasPendingChoice());
        assertEquals(loser, state.officer(4));
    }

    @Test
    void aChallengeLostByAnUnknownOfficerChangesNothing() {
        AtlasState state = new AtlasState(9L);

        assertNull(OfficerDefeat.challengeLost(state, 4, "hegemony", new Random(1L)));
        assertNull(OfficerDefeat.challengeLost(null, 4, "hegemony", new Random(1L)));
        assertTrue(state.officers().isEmpty());
    }

    @Test
    void aChoiceFollowsTheOfficerWhoInheritedItUntilNobodyHoldsIt() {
        AtlasState state = new AtlasState(9L);
        AtlasOfficer beaten = officer(4).withPendingOffers(List.of(ENGRAM, UNIQUE));
        state.putOfficer(beaten);
        List<String> choice = beaten.pendingChoice();

        assertEquals(4, OfficerDefeat.holderOf(state, choice).recordId());

        state.removeOfficer(4);
        state.putOfficer(officer(7).waitingInPlaceOf(beaten));
        assertEquals(7, OfficerDefeat.holderOf(state, choice).recordId());

        state.removeOfficer(7);
        assertNull(OfficerDefeat.holderOf(state, choice));
        assertNull(OfficerDefeat.holderOf(null, choice));
    }

    @Test
    void anUndecidedChoiceIsPickedAtRandomAndTheSameWayEveryTime() {
        AtlasState state = undecidedBoard();

        List<OfficerDefeat.RandomPick> randomPicks = OfficerDefeat.pickUnchosenAtRandom(state);

        assertEquals(1, randomPicks.size());
        assertEquals(4, randomPicks.get(0).officer().recordId());
        CarriedItem pick = randomPicks.get(0).pick();
        assertTrue(List.of(ENGRAM, UNIQUE).contains(pick));
        assertEquals(List.of(KERNELS, pick), state.officer(4).carried());
        assertFalse(state.officer(4).hasPendingChoice());
        assertEquals(List.of(KERNELS), state.officer(1).carried());
        assertEquals(pick, OfficerDefeat.pickUnchosenAtRandom(undecidedBoard()).get(0).pick());
        assertTrue(OfficerDefeat.pickUnchosenAtRandom(state).isEmpty());
        assertTrue(OfficerDefeat.pickUnchosenAtRandom((AtlasState) null).isEmpty());
    }

    @Test
    void theNextUndecidedChoiceBelongsToTheOfficerHoldingIt() {
        AtlasState state = undecidedBoard();

        assertEquals(4, OfficerDefeat.nextUnchosen(state).recordId());

        OfficerDefeat.pickUnchosenAtRandom(state);
        assertNull(OfficerDefeat.nextUnchosen(state));
        assertNull(OfficerDefeat.nextUnchosen(null));
    }

    private static AtlasState undecidedBoard() {
        AtlasState state = new AtlasState(9L);
        state.putOfficer(officer(1).withCarried(List.of(KERNELS)));
        state.putOfficer(officer(4).withCarried(List.of(KERNELS)).withPendingOffers(List.of(ENGRAM, UNIQUE)));
        return state;
    }

    @Test
    void anOfficerWithoutACustomFlagshipHasNoHullFacts() {
        assertNull(OfficerFleets.flagshipHull(officer(1)));
    }
}
