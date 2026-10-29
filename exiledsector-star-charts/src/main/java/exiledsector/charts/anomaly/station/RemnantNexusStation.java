package exiledsector.charts.anomaly.station;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

public final class RemnantNexusStation extends LaunchingStation {

    public static final String ID = "remnant_nexus_station";
    public static final RemnantNexusStation INSTANCE = new RemnantNexusStation();

    private RemnantNexusStation() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    String factionId() {
        return Factions.REMNANTS;
    }

    @Override
    String variantFor(int tier) {
        return StationRules.damagedNexus(tier) ? StationRules.NEXUS_DAMAGED_VARIANT : StationRules.NEXUS_VARIANT;
    }

    @Override
    String nameKey(int tier) {
        return StationRules.damagedNexus(tier) ? "chart.nexus.damagedName" : "chart.nexus.name";
    }

    @Override
    boolean damaged(int tier) {
        return StationRules.damagedNexus(tier);
    }

    @Override
    EveryFrameScript manager(CampaignFleetAPI station, String runId, int tier, long seed) {
        return new NexusFleetManager(station, runId, tier, seed);
    }
}
