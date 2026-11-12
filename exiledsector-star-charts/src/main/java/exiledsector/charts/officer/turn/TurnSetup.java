package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerRank;

import java.util.List;

public record TurnSetup(TurnRules rules, List<OfficerRank> ranks, IntentResolvers resolvers, AtlasCell openRunCell, int openRunOfficerId,
                        FlagshipCatalogue catalogue) {

    public TurnSetup {
        ranks = List.copyOf(ranks);
    }

    public int rankOrder(String rankId) {
        for (int index = 0; index < ranks.size(); index++) {
            if (ranks.get(index).id().equals(rankId)) {
                return index;
            }
        }
        return 0;
    }

    public OfficerRank rank(String rankId) {
        return ranks.get(rankOrder(rankId));
    }
}
