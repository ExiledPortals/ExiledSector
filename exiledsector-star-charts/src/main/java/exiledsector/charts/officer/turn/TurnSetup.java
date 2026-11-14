package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.StartingItemRoll;

import java.util.List;

public record TurnSetup(TurnRules rules, List<OfficerRank> ranks, IntentResolvers resolvers, AtlasCell openRunCell, int openRunOfficerId,
                        FlagshipCatalogue catalogue, StartingItemRoll startingItems) {

    public TurnSetup {
        ranks = List.copyOf(ranks);
    }

    public TurnSetup(TurnRules rules, List<OfficerRank> ranks, IntentResolvers resolvers, AtlasCell openRunCell, int openRunOfficerId,
                     FlagshipCatalogue catalogue) {
        this(rules, ranks, resolvers, openRunCell, openRunOfficerId, catalogue, StartingItemRoll.NONE);
    }

    public int rankOrder(String rankId) {
        return OfficerRanks.order(ranks, rankId);
    }

    public OfficerRank rank(String rankId) {
        return ranks.get(rankOrder(rankId));
    }

    public boolean canAct(AtlasOfficer officer) {
        return officer.isActive() && officer.recordId() != openRunOfficerId && !officer.commandCentre().equals(openRunCell);
    }
}
