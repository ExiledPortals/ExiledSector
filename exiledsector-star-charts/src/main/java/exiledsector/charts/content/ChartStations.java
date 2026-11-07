package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.ChartIds;

import java.util.Set;

public final class ChartStations {

    public static final String STATION_TAG = ChartIds.PREFIX + "station";
    static final Set<String> RAIDED_FROM_FACTIONS = Set.of(Factions.REMNANTS);

    private ChartStations() {
    }

    static void mark(CampaignFleetAPI station) {
        station.addTag(STATION_TAG);
    }

    public static boolean isStation(CampaignFleetAPI fleet) {
        return fleet != null && (fleet.isStationMode() || fleet.hasTag(STATION_TAG));
    }

    public static void matchStationModeTo(LocationAPI chartSystem, boolean playerInside) {
        if (chartSystem == null) {
            return;
        }
        for (CampaignFleetAPI fleet : chartSystem.getFleets()) {
            if (!fleet.hasTag(STATION_TAG)) {
                continue;
            }
            boolean stationMode = playerInside || !raidedFrom(fleet);
            if (fleet.isStationMode() != stationMode) {
                fleet.setStationMode(stationMode);
            }
        }
    }

    private static boolean raidedFrom(CampaignFleetAPI station) {
        return station.getFaction() != null && RAIDED_FROM_FACTIONS.contains(station.getFaction().getId());
    }
}
