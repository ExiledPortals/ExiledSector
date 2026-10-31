package data.kaysaar.aotd.vok.scripts.research;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.kaysaar.aotd.vok.scripts.research.models.ResearchOption;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AoTDMainResearchManager {

    public static final Set<String> RESEARCHED = new HashSet<>();
    public static final List<String> SPEC_IDS = new ArrayList<>();
    public static AoTDMainResearchManager instance;

    public final ArrayList<AoTDFactionResearchManager> factionResearchManagers = new ArrayList<>();
    public int specUpdates;

    public static AoTDMainResearchManager getInstance() {
        return instance;
    }

    public ArrayList<AoTDFactionResearchManager> getFactionResearchManagers() {
        return factionResearchManagers;
    }

    public void updateResearchOptionsFromSpec() {
        specUpdates++;
        for (AoTDFactionResearchManager factionManager : factionResearchManagers) {
            for (String id : SPEC_IDS) {
                if (factionManager.getResearchRepoOfFaction().stream().noneMatch(option -> option.Id.equals(id))) {
                    factionManager.getResearchRepoOfFaction().add(new ResearchOption(id));
                }
            }
        }
    }

    public boolean isResearchedForPlayer(String id) {
        return RESEARCHED.contains(id);
    }

    public boolean isAvailableForThisMarket(String id, MarketAPI market) {
        return RESEARCHED.contains(id);
    }
}
