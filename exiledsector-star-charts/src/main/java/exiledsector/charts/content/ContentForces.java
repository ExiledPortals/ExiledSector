package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.charts.content.ChartContentPlan.FleetRole;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ContentForces(Map<CampaignFleetAPI, FleetRole> fleetRoles, CampaignFleetAPI boss, ChartBossType bossType, String allyFactionId) {

    public static final ContentForces NONE = new ContentForces(Map.of(), null, ChartBossType.FLEET, null);

    public ContentForces {
        fleetRoles = fleetRoles == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fleetRoles));
        bossType = bossType == null ? ChartBossType.FLEET : bossType;
    }

    public List<CampaignFleetAPI> fleets() {
        return List.copyOf(fleetRoles.keySet());
    }

    public List<CampaignFleetAPI> fleets(FleetRole role) {
        return fleetRoles.entrySet().stream().filter(entry -> entry.getValue() == role).map(Map.Entry::getKey).toList();
    }

    public CampaignFleetAPI bossStation() {
        return bossType == ChartBossType.STATION ? boss : null;
    }
}
