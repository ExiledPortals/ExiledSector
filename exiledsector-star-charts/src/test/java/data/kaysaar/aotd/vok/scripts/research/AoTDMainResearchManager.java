package data.kaysaar.aotd.vok.scripts.research;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.HashSet;
import java.util.Set;

public class AoTDMainResearchManager {

    public static final Set<String> RESEARCHED = new HashSet<>();
    public static AoTDMainResearchManager instance;

    public static AoTDMainResearchManager getInstance() {
        return instance;
    }

    public boolean isResearchedForPlayer(String id) {
        return RESEARCHED.contains(id);
    }

    public boolean isAvailableForThisMarket(String id, MarketAPI market) {
        return RESEARCHED.contains(id);
    }
}
