package exiledsector.charts.officer;

import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.charts.officer.scar.ScarTable;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerDefeatTest {

    private static final CarriedItem KERNELS = new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 2);

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
    void anOfficerThatLosesAChallengeIsScarredAndStored() {
        AtlasState state = new AtlasState(9L);
        state.putOfficer(officer(4).withCarried(List.of(KERNELS)));

        AtlasOfficer loser = OfficerDefeat.challengeLost(state, 4, "faction_with_no_focus", new Random(1L));

        assertEquals(Map.of(ScarTable.FALLBACK_SCAR_ID, 1), loser.scars());
        assertEquals(List.of(KERNELS), loser.carried());
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
    void ironGripRemovesExactlyOneRewardTypeAndKeepsTheRest() {
        CarriedItem engram = CarriedItem.of(RewardKind.ENGRAM, "engram_a");
        CarriedItem otherEngram = CarriedItem.of(RewardKind.ENGRAM, "engram_b");
        CarriedItem feedstock = new CarriedItem(RewardKind.FEEDSTOCK, "threat_feedstock", null, 30);
        List<CarriedItem> carried = List.of(KERNELS, engram, feedstock, otherEngram);
        Set<List<CarriedItem>> outcomes = new HashSet<>();
        Random random = new Random(5L);
        for (int defeat = 0; defeat < 60; defeat++) {
            outcomes.add(OfficerDefeat.withoutOneRewardType(carried, random));
        }

        assertEquals(Set.of(List.of(engram, feedstock, otherEngram), List.of(KERNELS, feedstock), List.of(KERNELS, engram, otherEngram)), outcomes);
    }

    @Test
    void ironGripOnASingleRewardTypeTakesEverything() {
        List<CarriedItem> carried = List.of(CarriedItem.of(RewardKind.ENGRAM, "engram_a"), CarriedItem.of(RewardKind.ENGRAM, "engram_b"));

        assertEquals(List.of(), OfficerDefeat.withoutOneRewardType(carried, new Random(1L)));
        assertEquals(List.of(), OfficerDefeat.withoutOneRewardType(List.of(), new Random(1L)));
    }

    @Test
    void withoutIronGripADefeatKeepsNothing() {
        List<CarriedItem> carried = List.of(KERNELS, CarriedItem.of(RewardKind.ENGRAM, "engram_a"));

        assertEquals(List.of(), OfficerDefeat.keptAfterDefeat(carried, ChartGenContext.of(List.of()), new Random(1L)));
        assertEquals(List.of(), OfficerDefeat.keptAfterDefeat(carried, null, new Random(1L)));
        ChartGenContext ironGrip = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_OFFICER_LOSES_ONE_REWARD_TYPE, 1f)));
        assertEquals(1, OfficerDefeat.keptAfterDefeat(carried, ironGrip, new Random(1L)).size());
    }

    @Test
    void anOfficerWithoutACustomFlagshipHasNoHullFacts() {
        assertNull(OfficerFleets.flagshipHull(officer(1)));
    }
}
