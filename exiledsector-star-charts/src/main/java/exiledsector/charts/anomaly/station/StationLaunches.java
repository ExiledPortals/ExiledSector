package exiledsector.charts.anomaly.station;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public final class StationLaunches {

    static final long SPAWN_SALT = 0x4c41554e4348L;

    private final String runId;
    private final int tier;
    private final long seed;
    private int spawned;

    StationLaunches(String runId, int tier, long seed) {
        this.runId = runId;
        this.tier = tier;
        this.seed = seed;
    }

    CampaignFleetAPI next(SectorEntityToken source, StationFleets.Launch launch, int fleetsLost) {
        CampaignFleetAPI fleet = StationFleets.spawn(source, launch, runId, tier, fleetsLost, seed ^ (SPAWN_SALT * (spawned + 1L)));
        if (fleet != null) {
            spawned++;
        }
        return fleet;
    }

    boolean belongsToRun(String wantedRunId) {
        return runId.equals(wantedRunId);
    }
}
