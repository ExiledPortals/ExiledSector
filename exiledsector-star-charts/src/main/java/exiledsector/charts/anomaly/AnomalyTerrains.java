package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AnomalyTerrains {

    private AnomalyTerrains() {
    }

    public static <T> List<T> in(LocationAPI location, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (CampaignTerrainPlugin plugin : in(location, type::isInstance)) {
            found.add(type.cast(plugin));
        }
        return found;
    }

    public static List<CampaignTerrainPlugin> in(LocationAPI location, Predicate<CampaignTerrainPlugin> wanted) {
        List<CampaignTerrainPlugin> found = new ArrayList<>();
        if (location == null) {
            return found;
        }
        for (CampaignTerrainAPI terrain : location.getTerrainCopy()) {
            if (wanted.test(terrain.getPlugin())) {
                found.add(terrain.getPlugin());
            }
        }
        return found;
    }

    public static <T extends CampaignTerrainPlugin> T holding(CampaignFleetAPI fleet, List<T> terrains) {
        for (T terrain : terrains) {
            if (terrain.containsEntity(fleet)) {
                return terrain;
            }
        }
        return null;
    }
}
