package exiledsector.charts.atlas;

import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerBoardGenerator;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerRewardRepairTest {

    private static final List<OfficerRank> RANKS = List.of(
            new OfficerRank("lieutenant", 10, 0, 1, 2),
            new OfficerRank("admiral", 60, 50, 3, 1));
    private static final CarriedItem KERNELS = new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 2);
    private static final CarriedItem WARDS = new CarriedItem(RewardKind.CRAFTING_ITEM, "ward", null, 1);
    private static final CarriedItem ENGRAM = CarriedItem.of(RewardKind.ENGRAM, "engram_a");
    private static final TerritoryContext CONTEXT = new TerritoryContext(null, null, FlagshipCatalogue.NONE, (officer, random) -> WARDS, Set.of());

    private static List<AtlasOfficer> board(AtlasState state) {
        OfficerBoardGenerator.generate(TestBorders.SEED, RANKS, Map.of("hegemony", 1f), FlagshipCatalogue.NONE).forEach(state::putOfficer);
        return state.officers();
    }

    @Test
    void onlyOfficersCarryingNothingAndChoosingNothingAreDealtAReward() {
        AtlasState state = new AtlasState(TestBorders.SEED);
        List<AtlasOfficer> officers = board(state);
        int emptyId = officers.get(0).recordId();
        int carryingId = officers.get(1).recordId();
        int choosingId = officers.get(2).recordId();
        state.putOfficer(state.officer(carryingId).withCarried(List.of(KERNELS)));
        state.putOfficer(state.officer(choosingId).withPendingOffers(List.of(ENGRAM)));

        assertTrue(OfficerRewardRepair.repair(state, CONTEXT));

        assertEquals(List.of(WARDS), state.officer(emptyId).carried());
        assertEquals(List.of(KERNELS), state.officer(carryingId).carried());
        assertTrue(state.officer(choosingId).carried().isEmpty());
        assertFalse(OfficerRewardRepair.repair(state, CONTEXT));
    }
}
