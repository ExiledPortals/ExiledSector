package data.kaysaar.aotd.vok.scripts.research;

import data.kaysaar.aotd.vok.scripts.research.models.ResearchOption;

import java.util.ArrayList;

public class AoTDFactionResearchManager {

    public final ArrayList<ResearchOption> researchRepoOfFaction = new ArrayList<>();
    public final ResearchQueueManager queueManager = new ResearchQueueManager();
    public String currentFocusId;

    public ArrayList<ResearchOption> getResearchRepoOfFaction() {
        return researchRepoOfFaction;
    }

    public ResearchQueueManager getQueueManager() {
        return queueManager;
    }

    public void setCurrentFocus(String currentFocus) {
        currentFocusId = currentFocus;
    }
}
