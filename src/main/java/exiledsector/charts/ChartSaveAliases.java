package exiledsector.charts;

import com.thoughtworks.xstream.XStream;

import java.util.Map;

public final class ChartSaveAliases {

    static final Map<String, Class<?>> ALIASES = Map.of(
            "exiledSector.ChartRun", ChartRun.class,
            "exiledSector.ChartRunState", ChartRunState.class,
            "exiledSector.ChartWormholePlugin", ChartWormholePlugin.class);

    private ChartSaveAliases() {
    }

    public static void register(XStream xstream) {
        ALIASES.forEach(xstream::alias);
    }
}
