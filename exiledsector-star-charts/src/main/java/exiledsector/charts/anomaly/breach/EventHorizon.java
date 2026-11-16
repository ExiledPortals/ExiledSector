package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.ItemData;

import java.util.List;

public final class EventHorizon {

    public static final String TERRAIN_ID = "exiledSector_chart_event_horizon";
    public static final String WARPED_KEY = "$exiledSector_chartEventHorizonWarped";
    static final float EDGE_MARGIN = 600f;

    private EventHorizon() {
    }

    public static void surround(AnomalyContentSite site, PlanetAPI blackHole, List<SectorEntityToken> derelicts, float farthestReach) {
        ChartGenContext context = site.context();
        if (BreachLines.on(context, BreachLines.WARPED_DERELICTS_KEY)) {
            derelicts.forEach(derelict -> derelict.getMemoryWithoutUpdate().set(WARPED_KEY, true));
        }
        if (blackHole != null && BreachLines.on(context, BreachLines.EVENT_HORIZON_KEY)) {
            site.addTerrain(TERRAIN_ID, new EventHorizonTerrainPlugin.Params(radiusAround(blackHole.getRadius(), farthestReach)), blackHole.getLocation());
        }
    }

    static float radiusAround(float centreRadius, float farthestReach) {
        return Math.max(centreRadius, farthestReach) + EDGE_MARGIN;
    }

    public static ItemData onSiteDrop(ItemData item, MemoryAPI siteMemory) {
        return item == null || siteMemory == null || !siteMemory.getBoolean(WARPED_KEY) ? item : BreachLoot.warped(item, 1f);
    }
}
