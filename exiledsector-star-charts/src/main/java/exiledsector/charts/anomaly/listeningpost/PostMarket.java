package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

final class PostMarket {

    static final String MARKET_SUFFIX = "_market";

    private PostMarket() {
    }

    static MarketAPI ensure(SectorEntityToken post) {
        if (post.getMarket() != null) {
            return post.getMarket();
        }
        MarketAPI market = Global.getFactory().createMarket(post.getId() + MARKET_SUFFIX, post.getName(), 0);
        market.setPlanetConditionMarketOnly(true);
        market.setHidden(true);
        market.setPrimaryEntity(post);
        market.setFactionId(post.getFaction() == null ? Factions.NEUTRAL : post.getFaction().getId());
        post.setMarket(market);
        return market;
    }
}
