package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.SectorAPI;

public final class ThreatEncounter {

    static final String ENCOUNTERED_THREAT_KEY = "$encounteredThreat";

    private ThreatEncounter() {
    }

    public static boolean isFought(SectorAPI sector) {
        return sector != null && sector.getPlayerMemoryWithoutUpdate().getBoolean(ENCOUNTERED_THREAT_KEY);
    }
}
