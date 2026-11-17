package exiledsector.charts.tree;

import exiledsector.skills.loader.TreeJson;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AtlasOrbitReader {

    static final String ORBITS_PATH = "data/skilltrees/atlas_orbits.json";
    private static final Logger LOG = Logger.getLogger(AtlasOrbitReader.class);

    private AtlasOrbitReader() {
    }

    public static Map<String, AtlasOrbit> parse(JSONObject orbitsJson) {
        Map<String, AtlasOrbit> orbits = new LinkedHashMap<>();
        if (orbitsJson == null) {
            return orbits;
        }
        TreeJson.forEachEntry(orbitsJson.optJSONArray("orbits"), "atlas orbit", ORBITS_PATH, orbitJson -> {
            AtlasOrbit orbit = parseOrbit(orbitJson);
            orbits.put(orbit.nodeId(), orbit);
        });
        return orbits;
    }

    private static AtlasOrbit parseOrbit(JSONObject orbitJson) throws JSONException {
        return new AtlasOrbit(orbitJson.getString("id"), orbitJson.optString("around", null), (float) orbitJson.optDouble("speed", 0.0),
                orbitJson.optString("colour", null), parseArt(orbitJson.optJSONObject("art")));
    }

    private static AtlasBodyArt parseArt(JSONObject artJson) throws JSONException {
        if (artJson == null) {
            return AtlasBodyArt.PLAIN;
        }
        AtlasBodyArt.Kind kind = enumNamed(AtlasBodyArt.Kind.class, artJson.optString("kind", "plain"), AtlasBodyArt.Kind.PLAIN);
        List<String> paths = new ArrayList<>(TreeJson.stringList(artJson.optJSONArray("paths")));
        String path = artJson.optString("path", null);
        if (path != null) {
            paths.add(0, path);
        }
        AtlasBodyArt.Facing facing = enumNamed(AtlasBodyArt.Facing.class, artJson.optString("facing", "spin"), AtlasBodyArt.Facing.SPIN);
        AtlasBodyArt.Shape shape = new AtlasBodyArt.Shape((float) artJson.optDouble("size", AtlasBodyArt.DEFAULT_SIZE), facing,
                (float) artJson.optDouble("spin", 1.0));
        return new AtlasBodyArt(kind, artJson.optString("planet", null), paths, TreeJson.stringList(artJson.optJSONArray("hulls")), shape,
                artJson.optString("light", null));
    }

    private static <E extends Enum<E>> E enumNamed(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOG.error("[ExiledSector] " + ORBITS_PATH + ": unknown " + type.getSimpleName() + " \"" + name + "\"");
            return fallback;
        }
    }
}
