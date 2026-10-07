package exiledsector.skills.loader;

import com.fs.starfarer.api.Global;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.RingBelt;
import exiledsector.skills.layout.Rotation;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.layout.Star;
import exiledsector.skills.layout.StaticImage;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillTreeLoader {

    private static final String DATA_PATH = "data/skilltrees/ship_skill_tree.json";

    private SkillTreeLoader() {
    }

    public static JSONObject loadRoot() {
        try {
            return Global.getSettings().loadJSON(DATA_PATH);
        } catch (IOException | JSONException e) {
            Logger.getLogger(SkillTreeLoader.class).error("Failed to load " + DATA_PATH, e);
            return null;
        }
    }

    public static ParsedTree loadAll(Map<String, SkillType> skillTypes) {
        JSONObject treeJson = loadRoot();
        if (treeJson == null) {
            return new ParsedTree(new ArrayList<>(), 0, new LinkedHashMap<>(), new HashSet<>(),
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        JSONArray declaredNodes = treeJson.optJSONArray("nodes");
        return new ParsedTree(
                safeParse("nodes", () -> parseNodes(treeJson, skillTypes), new ArrayList<>()),
                declaredNodes == null ? 0 : declaredNodes.length(),
                safeParse("connector curves", () -> parseConnectorCurves(treeJson), new LinkedHashMap<>()),
                safeParse("hidden connectors", () -> parseHiddenConnectors(treeJson), new HashSet<>()),
                safeParse("static images", () -> parseStaticImages(treeJson), new ArrayList<>()),
                safeParse("ring belts", () -> parseRingBelts(treeJson), new ArrayList<>()),
                safeParse("stars", () -> parseStars(treeJson), new ArrayList<>()));
    }

    private static <T> T safeParse(String sectionName, JsonParser<T> parser, T fallback) {
        try {
            return parser.parse();
        } catch (JSONException e) {
            Logger.getLogger(SkillTreeLoader.class).error("Failed to parse " + sectionName + " from " + DATA_PATH, e);
            return fallback;
        }
    }

    private interface JsonParser<T> {
        T parse() throws JSONException;
    }

    public static final class ParsedTree {
        public final List<SkillNode> nodes;
        public final int declaredNodeCount;
        public final Map<String, ConnectorCurve> connectorCurves;
        public final Set<String> hiddenConnectors;
        public final List<StaticImage> staticImages;
        public final List<RingBelt> ringBelts;
        public final List<Star> stars;

        ParsedTree(List<SkillNode> nodes, int declaredNodeCount, Map<String, ConnectorCurve> connectorCurves, Set<String> hiddenConnectors,
                   List<StaticImage> staticImages, List<RingBelt> ringBelts, List<Star> stars) {
            this.nodes = nodes;
            this.declaredNodeCount = declaredNodeCount;
            this.connectorCurves = connectorCurves;
            this.hiddenConnectors = hiddenConnectors;
            this.staticImages = staticImages;
            this.ringBelts = ringBelts;
            this.stars = stars;
        }
    }

    public static List<RingBelt> parseRingBelts(JSONObject treeJson) throws JSONException {
        List<RingBelt> ringBelts = new ArrayList<>();
        JSONArray beltArray = treeJson.optJSONArray("ringBelts");
        if (beltArray == null) return ringBelts;
        for (int i = 0; i < beltArray.length(); i++) {
            JSONObject beltJson = beltArray.getJSONObject(i);
            ringBelts.add(new RingBelt(
                    beltJson.getString("id"),
                    (float) beltJson.getDouble("x"),
                    (float) beltJson.getDouble("y"),
                    new RingBelt.Shape((float) beltJson.getDouble("innerRadius"), (float) beltJson.getDouble("outerRadius"),
                            beltJson.getString("ringArtPath"),
                            new Rotation((float) beltJson.optDouble("rotation", 0.0), (float) beltJson.optDouble("rotationSpeed", 0.0))),
                    SkillTypeLoader.parseStringArray(beltJson.optJSONArray("tags"))));
        }
        return ringBelts;
    }

    public static List<StaticImage> parseStaticImages(JSONObject treeJson) throws JSONException {
        List<StaticImage> images = new ArrayList<>();
        JSONArray imageArray = treeJson.optJSONArray("staticImages");
        if (imageArray == null) return images;
        for (int i = 0; i < imageArray.length(); i++) {
            JSONObject imageJson = imageArray.getJSONObject(i);
            images.add(new StaticImage(
                    imageJson.getString("id"),
                    (float) imageJson.getDouble("x"),
                    (float) imageJson.getDouble("y"),
                    new StaticImage.Shape((float) imageJson.getDouble("width"), (float) imageJson.getDouble("height"),
                            imageJson.getString("imagePath"),
                            new Rotation((float) imageJson.optDouble("rotation", 0.0), (float) imageJson.optDouble("rotationSpeed", 0.0))),
                    SkillTypeLoader.parseStringArray(imageJson.optJSONArray("tags"))));
        }
        return images;
    }

    public static List<Star> parseStars(JSONObject treeJson) throws JSONException {
        List<Star> stars = new ArrayList<>();
        JSONArray starArray = treeJson.optJSONArray("stars");
        if (starArray == null) return stars;
        for (int i = 0; i < starArray.length(); i++) {
            JSONObject starJson = starArray.getJSONObject(i);
            stars.add(new Star(
                    starJson.getString("id"),
                    (float) starJson.getDouble("x"),
                    (float) starJson.getDouble("y"),
                    (float) starJson.getDouble("radius"),
                    starJson.optString("starType", "star_yellow"),
                    starJson.optString("color", null),
                    SkillTypeLoader.parseStringArray(starJson.optJSONArray("tags"))));
        }
        return stars;
    }

    public static Map<String, ConnectorCurve> parseConnectorCurves(JSONObject treeJson) throws JSONException {
        Map<String, ConnectorCurve> curves = new LinkedHashMap<>();
        JSONArray curveArray = treeJson.optJSONArray("connectorCurves");
        if (curveArray == null) return curves;
        for (int i = 0; i < curveArray.length(); i++) {
            JSONObject curveJson = curveArray.getJSONObject(i);
            String firstNodeId = curveJson.getString("a");
            String secondNodeId = curveJson.getString("b");
            float controlX = (float) curveJson.getDouble("controlX");
            float controlY = (float) curveJson.getDouble("controlY");
            curves.put(SkillTree.curveKey(firstNodeId, secondNodeId), new ConnectorCurve(controlX, controlY));
        }
        return curves;
    }

    public static Set<String> parseHiddenConnectors(JSONObject treeJson) throws JSONException {
        Set<String> hiddenKeys = new HashSet<>();
        JSONArray hiddenArray = treeJson.optJSONArray("hiddenConnectors");
        if (hiddenArray == null) return hiddenKeys;
        for (int i = 0; i < hiddenArray.length(); i++) {
            JSONObject hiddenJson = hiddenArray.getJSONObject(i);
            hiddenKeys.add(SkillTree.curveKey(hiddenJson.getString("a"), hiddenJson.getString("b")));
        }
        return hiddenKeys;
    }

    public static List<SkillNode> parseNodes(JSONObject treeJson, Map<String, SkillType> skillTypes) throws JSONException {
        List<SkillNode> nodes = new ArrayList<>();
        JSONArray nodeArray = treeJson.getJSONArray("nodes");
        for (int i = 0; i < nodeArray.length(); i++) {
            try {
                nodes.add(parseNode(nodeArray.getJSONObject(i), skillTypes));
            } catch (JSONException e) {
                Logger.getLogger(SkillTreeLoader.class).error("Skipping skill node " + SkillTypeLoader.entryLabel(nodeArray, i)
                        + " in " + DATA_PATH + ": " + e.getMessage());
            }
        }
        return nodes;
    }

    private static SkillNode parseNode(JSONObject nodeJson, Map<String, SkillType> skillTypes) throws JSONException {
        List<String> connectedTo = SkillTypeLoader.parseStringArray(nodeJson.optJSONArray("connectedTo"));
        List<String> tags = SkillTypeLoader.parseStringArray(nodeJson.optJSONArray("tags"));

        String typeId = nodeJson.getString("type");
        SkillType skillType = skillTypes.get(typeId);
        if (skillType == null) {
            throw new JSONException("Unknown skill type \"" + typeId + "\"");
        }

        String pairedWith = nodeJson.optString("pairedWith", null);
        if (pairedWith != null && skillType.getTier() != SkillTier.WORMHOLE) {
            Logger.getLogger(SkillTreeLoader.class).error("Ignoring pairedWith on skill node \"" + nodeJson.getString("id")
                    + "\" in " + DATA_PATH + ": only wormhole-tier nodes can be paired");
            pairedWith = null;
        }
        return new SkillNode(
                nodeJson.getString("id"),
                skillType,
                connectedTo,
                (float) nodeJson.optDouble("x", 0),
                (float) nodeJson.optDouble("y", 0),
                new SkillNodeDecoration(
                        nodeJson.optString("ringBeltPath", null),
                        nodeJson.optString("ringBeltColor", null),
                        nodeJson.has("ringBeltWidth") ? (float) nodeJson.getDouble("ringBeltWidth") : null,
                        nodeJson.optString("wormholeColor", null),
                        pairedWith),
                tags);
    }
}
