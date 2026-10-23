package exerelin.campaign;

import java.util.HashSet;
import java.util.Set;

public class SectorManager {

    public static final Set<String> LIVE_FACTION_IDS = new HashSet<>();

    public static boolean isFactionAlive(String factionId) {
        return LIVE_FACTION_IDS.contains(factionId);
    }
}
