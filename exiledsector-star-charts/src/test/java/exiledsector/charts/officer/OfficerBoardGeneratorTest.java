package exiledsector.charts.officer;

import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerBoardGeneratorTest {

    private static final long SEED = 0x0ff1ce5L;
    private static final Map<String, Float> FACTIONS = Map.of("hegemony", 3f, "pirates", 5f, "tritachyon", 2f);

    private static List<AtlasOfficer> board(long seed) {
        return OfficerBoardGenerator.generate(seed, OfficerRanks.DEFAULTS, FACTIONS, FlagshipCatalogue.NONE);
    }

    @Test
    void theDefaultBoardIsAPyramidOfFortyFiveWaitingOfficers() {
        List<AtlasOfficer> board = board(SEED);

        assertEquals(45, board.size());
        Map<String, Long> perRank = board.stream().collect(Collectors.groupingBy(AtlasOfficer::rankId, LinkedHashMap::new, Collectors.counting()));
        assertEquals(Map.of("lieutenant", 16L, "commander", 12L, "captain", 8L, "commodore", 6L, "admiral", 3L), perRank);
        assertEquals(List.of("lieutenant", "commander", "captain", "commodore", "admiral"), new ArrayList<>(perRank.keySet()));
        assertTrue(board.stream().allMatch(AtlasOfficer::neverPlaced));
        assertEquals(IntStream.rangeClosed(1, 45).boxed().toList(), board.stream().map(AtlasOfficer::recordId).toList());
    }

    @Test
    void officersComeOnlyFromTheWeightedFactionsWithAKnownTrait() {
        for (AtlasOfficer officer : board(SEED)) {
            assertTrue(FACTIONS.containsKey(officer.factionId()), officer.factionId());
            assertTrue(OfficerBoardGenerator.TRAIT_IDS.contains(officer.traitId()), officer.traitId());
        }
    }

    @Test
    void theSameSeedDealsTheSameBoardAndAnotherSeedDealsAnother() {
        assertEquals(board(SEED), board(SEED));
        assertNotEquals(board(SEED), board(SEED + 1));
    }

    @Test
    void theCatalogueChoosesEachFlagshipForTheOfficersFactionAndRank() {
        FlagshipCatalogue catalogue = (factionId, rankOrder, rankCount, random) ->
                new FlagshipCatalogue.Flagship(factionId + "_hull", factionId + "_" + rankOrder + "_of_" + rankCount);

        for (AtlasOfficer officer : OfficerBoardGenerator.generate(SEED, OfficerRanks.DEFAULTS, FACTIONS, catalogue)) {
            assertEquals(officer.factionId() + "_hull", officer.flagshipHullId());
            assertEquals(officer.factionId() + "_" + rankOrder(officer.rankId()) + "_of_5", officer.flagshipVariantId());
        }
    }

    @Test
    void theStartingItemRollAddsItsItemWithoutChangingTheRestOfTheBoard() {
        CarriedItem kernel = CarriedItem.of(RewardKind.CRAFTING_ITEM, "kernel");
        StartingItemRoll admiralsOnly = (officer, random) -> "admiral".equals(officer.rankId()) ? kernel : null;

        List<AtlasOfficer> plain = board(SEED);
        List<AtlasOfficer> dealt = OfficerBoardGenerator.generate(SEED, OfficerRanks.DEFAULTS, FACTIONS, FlagshipCatalogue.NONE, admiralsOnly);

        for (int index = 0; index < plain.size(); index++) {
            AtlasOfficer officer = dealt.get(index);
            List<CarriedItem> expected = "admiral".equals(officer.rankId()) ? List.of(kernel) : List.of();
            assertEquals(expected, officer.carried());
            assertEquals(plain.get(index), officer.withCarried(List.of()));
        }
    }

    @Test
    void withoutFactionsTheBoardStaysEmpty() {
        assertEquals(List.of(), OfficerBoardGenerator.generate(SEED, OfficerRanks.DEFAULTS, Map.of(), FlagshipCatalogue.NONE));
    }

    private static int rankOrder(String rankId) {
        for (int index = 0; index < OfficerRanks.DEFAULTS.size(); index++) {
            if (OfficerRanks.DEFAULTS.get(index).id().equals(rankId)) {
                return index;
            }
        }
        return -1;
    }
}
