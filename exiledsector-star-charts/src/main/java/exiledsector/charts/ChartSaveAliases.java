package exiledsector.charts;

import com.thoughtworks.xstream.XStream;
import exiledsector.charts.anomaly.breach.BreachCorePlugin;
import exiledsector.charts.anomaly.breach.BreachSupplyBuff;
import exiledsector.charts.anomaly.breach.BreachTerrainPlugin;
import exiledsector.charts.anomaly.convoy.ConvoyIntel;
import exiledsector.charts.anomaly.convoy.ConvoyRole;
import exiledsector.charts.anomaly.convoy.ConvoyScript;
import exiledsector.charts.anomaly.convoy.ConvoyState;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.wormhole.ChartWormholePlugin;
import exiledsector.charts.wormhole.WormholeGateway;

import java.util.Map;

public final class ChartSaveAliases {

    static final Map<String, Class<?>> ALIASES = Map.ofEntries(
            Map.entry("exiledSector.ChartRun", ChartRun.class),
            Map.entry("exiledSector.ChartRunState", ChartRunState.class),
            Map.entry("exiledSector.ChartWormholePlugin", ChartWormholePlugin.class),
            Map.entry("exiledSector.ChartObjectiveIntel", ChartObjectiveIntel.class),
            Map.entry("exiledSector.WormholeGateway", WormholeGateway.class),
            Map.entry("exiledSector.BreachTerrainPlugin", BreachTerrainPlugin.class),
            Map.entry("exiledSector.BreachCorePlugin", BreachCorePlugin.class),
            Map.entry("exiledSector.BreachSupplyBuff", BreachSupplyBuff.class),
            Map.entry("exiledSector.AtlasState", AtlasState.class),
            Map.entry("exiledSector.ConvoyScript", ConvoyScript.class),
            Map.entry("exiledSector.ConvoyState", ConvoyState.class),
            Map.entry("exiledSector.ConvoyPhase", ConvoyState.Phase.class),
            Map.entry("exiledSector.ConvoyRole", ConvoyRole.class),
            Map.entry("exiledSector.ConvoyIntel", ConvoyIntel.class));

    private ChartSaveAliases() {
    }

    public static void register(XStream xstream) {
        ALIASES.forEach(xstream::alias);
    }
}
