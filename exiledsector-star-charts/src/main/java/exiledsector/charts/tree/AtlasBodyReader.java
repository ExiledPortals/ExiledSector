package exiledsector.charts.tree;

import exiledsector.skills.loader.TreeJson;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AtlasBodyReader {

    static final String BODIES_PATH = "data/skilltrees/atlas_bodies.json";
    private static final Logger LOG = Logger.getLogger(AtlasBodyReader.class);

    private AtlasBodyReader() {
    }

    public static Map<String, AtlasBody> parse(JSONObject bodiesJson) {
        Map<String, AtlasBody> bodies = new LinkedHashMap<>();
        if (bodiesJson == null) {
            return bodies;
        }
        TreeJson.forEachEntry(bodiesJson.optJSONArray("bodies"), "atlas body", BODIES_PATH, bodyJson -> {
            AtlasBody body = parseBody(bodyJson);
            bodies.put(body.nodeId(), body);
        });
        return bodies;
    }

    private static AtlasBody parseBody(JSONObject bodyJson) throws JSONException {
        return new AtlasBody(bodyJson.getString("id"), bodyJson.optBoolean("highway", false), bodyJson.optString("colour", null),
                bodyJson.optString("wheel", null), Integer.signum(bodyJson.optInt("spiral", 0)), parseArt(bodyJson.optJSONObject("art")));
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
                (float) artJson.optDouble("spin", 1.0), (float) artJson.optDouble("heading", AtlasBodyArt.DEFAULT_HEADING));
        AtlasBodyArt.Colours colours = new AtlasBodyArt.Colours(artJson.optString("light", null), artJson.optString("tint", null));
        AtlasBodyArt.Weather weather = new AtlasBodyArt.Weather(
                enumNamed(AtlasBodyArt.Storm.class, artJson.optString("storm", "none"), AtlasBodyArt.Storm.NONE),
                enumNamed(AtlasBodyArt.Flare.class, artJson.optString("flare", "none"), AtlasBodyArt.Flare.NONE));
        AtlasBodyArt.Population population = new AtlasBodyArt.Population(TreeJson.stringList(artJson.optJSONArray("cityLights")),
                fleets(artJson.optJSONArray("convoys")), fleets(artJson.optJSONArray("haulers")));
        return new AtlasBodyArt(kind, artJson.optString("planet", null), paths, TreeJson.stringList(artJson.optJSONArray("hulls")), shape, colours,
                weather, population);
    }

    private static List<List<String>> fleets(JSONArray fleetsJson) throws JSONException {
        List<List<String>> fleets = new ArrayList<>();
        if (fleetsJson == null) {
            return fleets;
        }
        for (int index = 0; index < fleetsJson.length(); index++) {
            List<String> hulls = TreeJson.stringList(fleetsJson.optJSONArray(index));
            if (!hulls.isEmpty()) {
                fleets.add(hulls);
            }
        }
        return fleets;
    }

    private static <E extends Enum<E>> E enumNamed(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOG.error("[ExiledSector] " + BODIES_PATH + ": unknown " + type.getSimpleName() + " \"" + name + "\"");
            return fallback;
        }
    }
}
