package exiledsector.charts;

import com.thoughtworks.xstream.XStream;
import exiledsector.charts.anomaly.breach.BreachCorePlugin;
import exiledsector.charts.anomaly.breach.BreachSupplyBuff;
import exiledsector.charts.anomaly.breach.BreachTerrainPlugin;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.wormhole.ChartWormholePlugin;
import exiledsector.charts.wormhole.WormholeGateway;

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
            "exiledSector.BreachSupplyBuff", BreachSupplyBuff.class,
            "exiledSector.AtlasState", AtlasState.class);

    private ChartSaveAliases() {
    }

    public static void register(XStream xstream) {
        ALIASES.forEach(xstream::alias);
    }
}
