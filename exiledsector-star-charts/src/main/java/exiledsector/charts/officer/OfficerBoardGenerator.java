package exiledsector.charts.officer;

import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import exiledsector.Seeds;
import exiledsector.charts.atlas.AtlasHash;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class OfficerBoardGenerator {

    public static final List<String> TRAIT_IDS = List.of(Personalities.CAUTIOUS, Personalities.STEADY, Personalities.AGGRESSIVE,
            Personalities.RECKLESS);
    private static final long FACTION_SALT = 0x4f4646464143L;
    private static final long TRAIT_SALT = 0x4f4646545254L;
    private static final long SEEDS_SALT = 0x4f4646534544L;

    private OfficerBoardGenerator() {
    }

    public static List<AtlasOfficer> generate(long seed, List<OfficerRank> ranks, Map<String, Float> factionWeights, FlagshipCatalogue catalogue) {
        return generate(seed, ranks, factionWeights, catalogue, StartingItemRoll.NONE);
    }

    public static List<AtlasOfficer> generate(long seed, List<OfficerRank> ranks, Map<String, Float> factionWeights, FlagshipCatalogue catalogue,
                                              StartingItemRoll startingItems) {
        List<AtlasOfficer> board = new ArrayList<>();
        int recordId = 0;
        for (int rankOrder = 0; rankOrder < ranks.size(); rankOrder++) {
            for (int place = 0; place < ranks.get(rankOrder).places(); place++) {
                recordId++;
                String factionId = AtlasHash.race(seed, FACTION_SALT, recordId, rankOrder, factionWeights);
                if (factionId == null) {
                    return board;
                }
                board.add(officer(seed, recordId, factionId, rankOrder, place, ranks, catalogue, startingItems));
            }
        }
        return board;
    }

    public static AtlasOfficer replacement(long seed, int recordId, String factionId, int seat, List<OfficerRank> ranks, FlagshipCatalogue catalogue,
                                           StartingItemRoll startingItems) {
        return officer(seed, recordId, factionId, 0, seat, ranks, catalogue, startingItems);
    }

    public static AtlasOfficer atRank(long seed, int recordId, String factionId, String rankId, int seat, List<OfficerRank> ranks,
                                      FlagshipCatalogue catalogue, StartingItemRoll startingItems) {
        return officer(seed, recordId, factionId, OfficerSeats.order(ranks, rankId), seat, ranks, catalogue, startingItems);
    }

    private static AtlasOfficer officer(long seed, int recordId, String factionId, int rankOrder, int seat, List<OfficerRank> ranks,
                                        FlagshipCatalogue catalogue, StartingItemRoll startingItems) {
        Map<String, Float> traitWeights = new LinkedHashMap<>();
        TRAIT_IDS.forEach(traitId -> traitWeights.put(traitId, 1f));
        String traitId = AtlasHash.race(seed, TRAIT_SALT, recordId, rankOrder, traitWeights);
        Random random = Seeds.derived(seed, SEEDS_SALT ^ recordId);
        AtlasOfficer.OfficerSeeds seeds = new AtlasOfficer.OfficerSeeds(random.nextLong(), random.nextLong(), random.nextLong(),
                random.nextLong());
        FlagshipCatalogue.Flagship flagship = catalogue.pick(factionId, rankOrder, ranks.size(), random);
        AtlasOfficer officer = AtlasOfficer.waiting(recordId, factionId, ranks.get(rankOrder).id(), traitId, seeds, flagship)
                .seatedAt(ranks.get(rankOrder).id(), seat);
        return OfficerStartingItems.dealt(officer, startingItems, random);
    }
}
