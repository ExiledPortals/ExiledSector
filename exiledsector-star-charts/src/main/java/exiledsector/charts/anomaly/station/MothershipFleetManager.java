package exiledsector.charts.anomaly.station;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.SourceBasedFleetManager;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.run.ChartRunBound;

public final class MothershipFleetManager extends SourceBasedFleetManager implements ChartRunBound {

    static final StationFleets.Launch LAUNCH = new StationFleets.Launch(Factions.DERELICT, StationRules.DRONE_MAX_SHIP_SIZE, (random, fleet) -> {
    });

    private final String runId;
    private final int tier;
    private final long seed;
    private int spawned;
    private int fleetsLost;

    MothershipFleetManager(CampaignFleetAPI mothership, String runId, int tier, long seed) {
        super(mothership, StationRules.THRESHOLD_LY, StationRules.MIN_FLEETS, StationRules.MOTHERSHIP_MAX_FLEETS, StationRules.MOTHERSHIP_RESPAWN_DAYS);
        this.runId = runId;
        this.tier = tier;
        this.seed = seed;
    }

    @Override
    protected CampaignFleetAPI spawnFleet() {
        CampaignFleetAPI fleet = StationFleets.spawn(source, LAUNCH, runId, tier, fleetsLost, StationFleets.spawnSeed(seed, spawned));
        if (fleet != null) {
            spawned++;
        }
        return fleet;
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        super.reportFleetDespawnedToListener(fleet, reason, param);
        if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE) {
            fleetsLost++;
        }
    }

    @Override
    public boolean belongsToRun(String wantedRunId) {
        return runId.equals(wantedRunId);
    }

    int fleetsLost() {
        return fleetsLost;
    }

    int maxFleets() {
        return maxFleets;
    }
}
