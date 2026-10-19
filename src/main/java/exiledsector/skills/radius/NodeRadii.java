package exiledsector.skills.radius;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class NodeRadii {

    public static final String DATA_PATH = "data/config/exiledSector/node_radii.json";
    private static final Logger LOG = Logger.getLogger(NodeRadii.class);
    private static final Map<RadiusSize, Float> UNCONFIGURED = unconfigured();
    private static final AtomicReference<Map<RadiusSize, Float>> DISTANCES_BY_SIZE = new AtomicReference<>(UNCONFIGURED);

    private NodeRadii() {
    }

    public static void load() {
        try {
            register(Global.getSettings().loadJSON(DATA_PATH));
        } catch (IOException | JSONException | RuntimeException e) {
            LOG.error("Couldn't load node radii from " + DATA_PATH + "; radius effects cover no nodes", e);
            DISTANCES_BY_SIZE.set(UNCONFIGURED);
        }
    }

    public static void register(JSONObject radiiJson) throws JSONException {
        if (radiiJson == null) {
            throw new JSONException("No node radii were found");
        }
        Map<RadiusSize, Float> distancesBySize = new EnumMap<>(RadiusSize.class);
        float previousDistance = 0f;
        for (RadiusSize size : RadiusSize.values()) {
            float distance = (float) radiiJson.getDouble(size.dataKey());
            if (!(distance >= previousDistance)) {
                throw new JSONException("Node radius \"" + size.dataKey() + "\" is " + distance
                        + "; sizes must be non-negative and grow from small to large");
            }
            distancesBySize.put(size, distance);
            previousDistance = distance;
        }
        DISTANCES_BY_SIZE.set(Map.copyOf(distancesBySize));
    }

    public static void reset() {
        DISTANCES_BY_SIZE.set(UNCONFIGURED);
    }

    public static Map<RadiusSize, Float> distances() {
        return DISTANCES_BY_SIZE.get();
    }

    public static float distance(RadiusSize size) {
        return DISTANCES_BY_SIZE.get().get(size);
    }

    private static Map<RadiusSize, Float> unconfigured() {
        Map<RadiusSize, Float> distancesBySize = new EnumMap<>(RadiusSize.class);
        for (RadiusSize size : RadiusSize.values()) {
            distancesBySize.put(size, 0f);
        }
        return Map.copyOf(distancesBySize);
    }
}
