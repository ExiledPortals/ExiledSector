package exiledsector.charts;

import com.thoughtworks.xstream.XStream;
import exiledsector.charts.anomaly.breach.BreachCorePlugin;
import exiledsector.charts.anomaly.breach.BreachSupplyBuff;
import exiledsector.charts.anomaly.breach.BreachTerrainPlugin;
import exiledsector.charts.anomaly.convoy.ConvoyIntel;
import exiledsector.charts.anomaly.convoy.ConvoyRole;
import exiledsector.charts.anomaly.convoy.ConvoyScript;
import exiledsector.charts.anomaly.convoy.ConvoyState;
import exiledsector.charts.anomaly.convoy.ConvoyStep;
import exiledsector.charts.anomaly.debris.AnomalousDebrisTerrainPlugin;
import exiledsector.charts.anomaly.heist.ListeningPostPlugin;
import exiledsector.charts.anomaly.heist.PicketStation;
import exiledsector.charts.anomaly.heist.PostScript;
import exiledsector.charts.anomaly.station.MothershipFleetManager;
import exiledsector.charts.anomaly.station.NexusFleetManager;
import exiledsector.charts.anomaly.station.StationLaunches;
import exiledsector.charts.anomaly.storm.StormFrontTerrainPlugin;
import exiledsector.charts.layout.ChartCoronaTerrainPlugin;
import exiledsector.charts.layout.ChartPulsarBeamTerrainPlugin;
import exiledsector.charts.layout.abyss.AbyssalDarknessTerrainPlugin;
import exiledsector.charts.layout.abyss.AbyssalFogTerrainPlugin;
import exiledsector.charts.layout.abyss.ChartPhotospherePlugin;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.wormhole.ChartWormholePlugin;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.ui.atlas.AtlasAbility;

import java.util.Map;

public final class ChartSaveAliases {

    static final Map<String, Class<?>> ALIASES = Map.ofEntries(
            Map.entry("exiledSector.ChartRun", ChartRun.class),
            Map.entry("exiledSector.ChartRunState", ChartRunState.class),
            Map.entry("exiledSector.ChartWormholePlugin", ChartWormholePlugin.class),
            Map.entry("exiledSector.ChartObjectiveIntel", ChartObjectiveIntel.class),
            Map.entry("exiledSector.WormholeGateway", WormholeGateway.class),
            Map.entry("exiledSector.AtlasAbility", AtlasAbility.class),
            Map.entry("exiledSector.BreachTerrainPlugin", BreachTerrainPlugin.class),
            Map.entry("exiledSector.BreachCorePlugin", BreachCorePlugin.class),
            Map.entry("exiledSector.BreachSupplyBuff", BreachSupplyBuff.class),
            Map.entry("exiledSector.AtlasState", AtlasState.class),
            Map.entry("exiledSector.ConvoyScript", ConvoyScript.class),
            Map.entry("exiledSector.ConvoyState", ConvoyState.class),
            Map.entry("exiledSector.ConvoyPhase", ConvoyState.Phase.class),
            Map.entry("exiledSector.ConvoyRole", ConvoyRole.class),
            Map.entry("exiledSector.ConvoyStep", ConvoyStep.class),
            Map.entry("exiledSector.ConvoyStepKind", ConvoyStep.Kind.class),
            Map.entry("exiledSector.ConvoyIntel", ConvoyIntel.class),
            Map.entry("exiledSector.AnomalousDebrisTerrainPlugin", AnomalousDebrisTerrainPlugin.class),
            Map.entry("exiledSector.StormFrontTerrainPlugin", StormFrontTerrainPlugin.class),
            Map.entry("exiledSector.ChartPhotospherePlugin", ChartPhotospherePlugin.class),
            Map.entry("exiledSector.AbyssalFogTerrainPlugin", AbyssalFogTerrainPlugin.class),
            Map.entry("exiledSector.AbyssalDarknessTerrainPlugin", AbyssalDarknessTerrainPlugin.class),
            Map.entry("exiledSector.ChartCoronaTerrainPlugin", ChartCoronaTerrainPlugin.class),
            Map.entry("exiledSector.ChartPulsarBeamTerrainPlugin", ChartPulsarBeamTerrainPlugin.class),
            Map.entry("exiledSector.ListeningPostPlugin", ListeningPostPlugin.class),
            Map.entry("exiledSector.PostScript", PostScript.class),
            Map.entry("exiledSector.PicketStation", PicketStation.class),
            Map.entry("exiledSector.MothershipFleetManager", MothershipFleetManager.class),
            Map.entry("exiledSector.NexusFleetManager", NexusFleetManager.class),
            Map.entry("exiledSector.StationLaunches", StationLaunches.class));

    static final Map<String, Class<?>> MOVED = Map.of(
            "exiledsector.charts.anomaly.ChartCoronaTerrainPlugin", ChartCoronaTerrainPlugin.class,
            "exiledsector.charts.anomaly.ChartPulsarBeamTerrainPlugin", ChartPulsarBeamTerrainPlugin.class);

    private ChartSaveAliases() {
    }

    public static void register(XStream xstream) {
        MOVED.forEach(xstream::alias);
        ALIASES.forEach(xstream::alias);
    }
}
