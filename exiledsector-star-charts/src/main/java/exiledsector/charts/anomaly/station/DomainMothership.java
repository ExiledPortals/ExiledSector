package exiledsector.charts.anomaly.station;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

public final class DomainMothership extends LaunchingStation {

    public static final String ANOMALY_ID = "domain_mothership";
    public static final DomainMothership INSTANCE = new DomainMothership();

    private DomainMothership() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    String factionId() {
        return Factions.DERELICT;
    }

    @Override
    String variantFor(int tier) {
        return StationRules.MOTHERSHIP_VARIANT;
    }

    @Override
    String nameKey(int tier) {
        return "chart.mothership.name";
    }

    @Override
    boolean damaged(int tier) {
        return false;
    }

    @Override
    EveryFrameScript manager(CampaignFleetAPI station, String runId, int tier, long seed) {
        return new MothershipFleetManager(station, runId, tier, seed);
    }
}
