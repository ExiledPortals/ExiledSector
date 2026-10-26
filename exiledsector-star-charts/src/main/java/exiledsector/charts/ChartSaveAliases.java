package exiledsector.charts;

import com.thoughtworks.xstream.XStream;
import exiledsector.charts.anomaly.BreachCorePlugin;
import exiledsector.charts.anomaly.BreachSupplyBuff;
import exiledsector.charts.anomaly.BreachTerrainPlugin;

import java.util.Map;

public final class ChartSaveAliases {

    static final Map<String, Class<?>> ALIASES = Map.of(
            "exiledSector.ChartRun", ChartRun.class,
            "exiledSector.ChartRunState", ChartRunState.class,
            "exiledSector.ChartWormholePlugin", ChartWormholePlugin.class,
            "exiledSector.ChartObjectiveIntel", ChartObjectiveIntel.class,
            "exiledSector.WormholeGateway", WormholeGateway.class,
            "exiledSector.BreachTerrainPlugin", BreachTerrainPlugin.class,
            "exiledSector.BreachCorePlugin", BreachCorePlugin.class,
            "exiledSector.BreachSupplyBuff", BreachSupplyBuff.class);

    private ChartSaveAliases() {
    }

    public static void register(XStream xstream) {
        ALIASES.forEach(xstream::alias);
    }
}
