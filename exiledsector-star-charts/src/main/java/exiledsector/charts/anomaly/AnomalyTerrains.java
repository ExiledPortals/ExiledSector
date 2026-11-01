package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.LocationAPI;

import java.util.ArrayList;
import java.util.List;

public final class AnomalyTerrains {

    private AnomalyTerrains() {
    }

    public static <T> List<T> in(LocationAPI location, Class<T> type) {
        List<T> found = new ArrayList<>();
        if (location == null) {
            return found;
        }
        for (CampaignTerrainAPI terrain : location.getTerrainCopy()) {
            if (type.isInstance(terrain.getPlugin())) {
                found.add(type.cast(terrain.getPlugin()));
            }
        }
        return found;
    }
}
