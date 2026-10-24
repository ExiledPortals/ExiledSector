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
import exiledsector.skills.layout.ThreatUnit;
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
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        JSONArray declaredNodes = treeJson.optJSONArray("nodes");
        List<SkillNode> nodes;
        try {
            nodes = parseNodes(treeJson, skillTypes);
        } catch (JSONException e) {
            Logger.getLogger(SkillTreeLoader.class).error("Failed to parse nodes from " + DATA_PATH, e);
            nodes = new ArrayList<>();
        }
        return new ParsedTree(nodes, declaredNodes == null ? 0 : declaredNodes.length(), parseConnectorCurves(treeJson),
                parseHiddenConnectors(treeJson), parseStaticImages(treeJson), parseRingBelts(treeJson), parseStars(treeJson),
                parseThreatUnits(treeJson));
    }

    private static void forEachEntry(JSONArray entries, String sectionName, EntryParser parser) {
        if (entries == null) {
            return;
        }
        for (int i = 0; i < entries.length(); i++) {
            try {
                parser.parse(entries.getJSONObject(i));
            } catch (JSONException e) {
                Logger.getLogger(SkillTreeLoader.class).error("Skipping " + sectionName + " " + SkillTypeLoader.entryLabel(entries, i)
                        + " in " + DATA_PATH + ": " + e.getMessage());
            }
        }
    }

    private interface EntryParser {
        void parse(JSONObject entryJson) throws JSONException;
    }

    public static final class ParsedTree {
        public final List<SkillNode> nodes;
        public final int declaredNodeCount;
        public final Map<String, ConnectorCurve> connectorCurves;
        public final Set<String> hiddenConnectors;
        public final List<StaticImage> staticImages;
        public final List<RingBelt> ringBelts;
        public final List<Star> stars;
        public final List<ThreatUnit> threatUnits;

        ParsedTree(List<SkillNode> nodes, int declaredNodeCount, Map<String, ConnectorCurve> connectorCurves, Set<String> hiddenConnectors,
                   List<StaticImage> staticImages, List<RingBelt> ringBelts, List<Star> stars,
                   List<ThreatUnit> threatUnits) {
            this.nodes = nodes;
            this.declaredNodeCount = declaredNodeCount;
            this.connectorCurves = connectorCurves;
            this.hiddenConnectors = hiddenConnectors;
            this.staticImages = staticImages;
            this.ringBelts = ringBelts;
            this.stars = stars;
            this.threatUnits = threatUnits;
        }
    }

    public static List<RingBelt> parseRingBelts(JSONObject treeJson) {
        List<RingBelt> ringBelts = new ArrayList<>();
        forEachEntry(treeJson.optJSONArray("ringBelts"), "ring belt", beltJson -> ringBelts.add(new RingBelt(
                beltJson.getString("id"),
                (float) beltJson.getDouble("x"),
                (float) beltJson.getDouble("y"),
                new RingBelt.Shape((float) beltJson.getDouble("innerRadius"), (float) beltJson.getDouble("outerRadius"),
                        beltJson.getString("ringArtPath"),
                        new Rotation((float) beltJson.optDouble("rotation", 0.0), (float) beltJson.optDouble("rotationSpeed", 0.0))),
                SkillTypeLoader.parseStringArray(beltJson.optJSONArray("tags")))));
        return ringBelts;
    }

    public static List<StaticImage> parseStaticImages(JSONObject treeJson) {
        List<StaticImage> images = new ArrayList<>();
        forEachEntry(treeJson.optJSONArray("staticImages"), "static image", imageJson -> images.add(new StaticImage(
                imageJson.getString("id"),
                (float) imageJson.getDouble("x"),
                (float) imageJson.getDouble("y"),
                new StaticImage.Shape((float) imageJson.getDouble("width"), (float) imageJson.getDouble("height"),
                        imageJson.getString("imagePath"),
                        new Rotation((float) imageJson.optDouble("rotation", 0.0), (float) imageJson.optDouble("rotationSpeed", 0.0))),
                imageJson.optBoolean("behindStars", false),
                SkillTypeLoader.parseStringArray(imageJson.optJSONArray("tags")))));
        return images;
    }

    public static List<Star> parseStars(JSONObject treeJson) {
        List<Star> stars = new ArrayList<>();
        forEachEntry(treeJson.optJSONArray("stars"), "star", starJson -> {
            String overlayPath = starJson.optString("overlayPath", null);
            Star.Overlay overlay = overlayPath == null || overlayPath.isEmpty() ? null
                    : new Star.Overlay(overlayPath, (float) starJson.optDouble("overlaySize", 1.0), (float) starJson.optDouble("overlayAlpha", 1.0));
            stars.add(new Star(
                    starJson.getString("id"),
                    (float) starJson.getDouble("x"),
                    (float) starJson.getDouble("y"),
                    (float) starJson.getDouble("radius"),
                    starJson.optString("starType", "star_yellow"),
                    starJson.optString("color", null),
                    overlay,
                    SkillTypeLoader.parseStringArray(starJson.optJSONArray("tags"))));
        });
        return stars;
    }

    public static List<ThreatUnit> parseThreatUnits(JSONObject treeJson) {
        List<ThreatUnit> threatUnits = new ArrayList<>();
        forEachEntry(treeJson.optJSONArray("threatUnits"), "threat unit", unitJson -> threatUnits.add(new ThreatUnit(
                unitJson.getString("id"),
                (float) unitJson.getDouble("x"),
                (float) unitJson.getDouble("y"),
                unitJson.optString("unitType", ThreatUnit.FABRICATOR),
                (float) unitJson.getDouble("length"),
                (float) unitJson.optDouble("rotation", 0.0),
                SkillTypeLoader.parseStringArray(unitJson.optJSONArray("tags")))));
        return threatUnits;
    }

    public static Map<String, ConnectorCurve> parseConnectorCurves(JSONObject treeJson) {
        Map<String, ConnectorCurve> curves = new LinkedHashMap<>();
        forEachEntry(treeJson.optJSONArray("connectorCurves"), "connector curve", curveJson -> {
            String firstNodeId = curveJson.getString("a");
            String secondNodeId = curveJson.getString("b");
            float controlX = (float) curveJson.getDouble("controlX");
            float controlY = (float) curveJson.getDouble("controlY");
            curves.put(SkillTree.curveKey(firstNodeId, secondNodeId), new ConnectorCurve(controlX, controlY));
        });
        return curves;
    }

    public static Set<String> parseHiddenConnectors(JSONObject treeJson) {
        Set<String> hiddenKeys = new HashSet<>();
        forEachEntry(treeJson.optJSONArray("hiddenConnectors"), "hidden connector",
                hiddenJson -> hiddenKeys.add(SkillTree.curveKey(hiddenJson.getString("a"), hiddenJson.getString("b"))));
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
