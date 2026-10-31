package data.kaysaar.aotd.vok.scripts.research;

import data.kaysaar.aotd.vok.scripts.research.models.ResearchOption;

import java.util.ArrayList;

public class ResearchQueueManager {

    public final ArrayList<ResearchOption> queuedResearchOptions = new ArrayList<>();

    public boolean removeFromQueue(String idOfResearchToRemove) {
        return queuedResearchOptions.removeIf(option -> option.Id.equals(idOfResearchToRemove));
    }
}
