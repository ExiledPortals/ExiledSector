package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.StartingItemRoll;

import java.util.List;
import java.util.Set;

public record TurnSetup(TurnRules rules, List<OfficerRank> ranks, IntentResolvers resolvers, AtlasCell openRunCell, Set<Integer> openRunOfficerIds,
                        FlagshipCatalogue catalogue, StartingItemRoll startingItems) {

    public TurnSetup {
        ranks = List.copyOf(ranks);
        openRunOfficerIds = openRunOfficerIds == null ? Set.of() : Set.copyOf(openRunOfficerIds);
    }

    public TurnSetup(TurnRules rules, List<OfficerRank> ranks, IntentResolvers resolvers, AtlasCell openRunCell, Set<Integer> openRunOfficerIds,
                     FlagshipCatalogue catalogue) {
        this(rules, ranks, resolvers, openRunCell, openRunOfficerIds, catalogue, StartingItemRoll.NONE);
    }

    public int rankOrder(String rankId) {
        return OfficerRanks.order(ranks, rankId);
    }

    public OfficerRank rank(String rankId) {
        return ranks.get(rankOrder(rankId));
    }

    public boolean canAct(AtlasOfficer officer) {
        return officer.isActive() && !fightsInOpenRun(officer.recordId()) && !officer.commandCentre().equals(openRunCell);
    }

    public boolean fightsInOpenRun(int recordId) {
        return openRunOfficerIds.contains(recordId);
    }
}
