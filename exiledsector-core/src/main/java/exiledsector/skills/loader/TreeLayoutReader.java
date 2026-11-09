package exiledsector.skills.loader;

import com.fs.starfarer.api.Global;
import exiledsector.skills.SkillTier;
import exiledsector.skills.decoration.Decorations;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.layout.TreeNode;
import exiledsector.skills.layout.TreeTags;
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
import java.util.function.Predicate;

public final class TreeLayoutReader {

    public interface NodeFactory<N extends TreeNode> {

        SkillTier tierOf(String typeId);

        N create(NodeEntry entry);
    }

    public record NodeEntry(String id, String typeId, List<String> connectedTo, float x, float y, SkillNodeDecoration decoration,
                            TreeTags treeTags) {
    }

    public record Layout<N extends TreeNode>(List<N> nodes, int declaredNodeCount, Map<String, ConnectorCurve> connectorCurves,
                                             Set<String> hiddenConnectors, Decorations decorations, List<String> volumes) {

        public static <N extends TreeNode> Layout<N> empty() {
            return new Layout<>(new ArrayList<>(), 0, new LinkedHashMap<>(), new HashSet<>(), Decorations.NONE, List.of());
        }
    }

    private TreeLayoutReader() {
    }

    public static JSONObject load(String path) {
        try {
            return Global.getSettings().loadJSON(path);
        } catch (IOException | JSONException e) {
            Logger.getLogger(TreeLayoutReader.class).error("Failed to load " + path, e);
            return null;
        }
    }

    public static <N extends TreeNode> Layout<N> read(JSONObject treeJson, String sourceName, Predicate<String> isVolume, NodeFactory<N> factory) {
        if (treeJson == null) {
            return Layout.empty();
        }
        JSONArray declaredNodes = treeJson.optJSONArray("nodes");
        List<N> nodes;
        try {
            nodes = parseNodes(treeJson, sourceName, isVolume, factory);
        } catch (JSONException e) {
            Logger.getLogger(TreeLayoutReader.class).error("Failed to parse nodes from " + sourceName, e);
            nodes = new ArrayList<>();
        }
        return new Layout<>(nodes, declaredNodes == null ? 0 : declaredNodes.length(), parseConnectorCurves(treeJson, sourceName),
                parseHiddenConnectors(treeJson, sourceName), DecorationLoader.parse(treeJson, sourceName, isVolume), parseVolumes(treeJson, sourceName));
    }

    public static List<String> parseVolumes(JSONObject treeJson, String sourceName) {
        try {
            return List.copyOf(TreeJson.stringList(treeJson.optJSONArray("volumes")));
        } catch (JSONException e) {
            Logger.getLogger(TreeLayoutReader.class).error("Skipping the volumes list in " + sourceName + ": " + e.getMessage());
            return List.of();
        }
    }

    public static Map<String, ConnectorCurve> parseConnectorCurves(JSONObject treeJson, String sourceName) {
        Map<String, ConnectorCurve> curves = new LinkedHashMap<>();
        TreeJson.forEachEntry(treeJson.optJSONArray("connectorCurves"), "connector curve", sourceName, curveJson -> {
            String firstNodeId = curveJson.getString("a");
            String secondNodeId = curveJson.getString("b");
            float controlX = (float) curveJson.getDouble("controlX");
            float controlY = (float) curveJson.getDouble("controlY");
            curves.put(ConnectorCurve.key(firstNodeId, secondNodeId), new ConnectorCurve(controlX, controlY));
        });
        return curves;
    }

    public static Set<String> parseHiddenConnectors(JSONObject treeJson, String sourceName) {
        Set<String> hiddenKeys = new HashSet<>();
        TreeJson.forEachEntry(treeJson.optJSONArray("hiddenConnectors"), "hidden connector", sourceName,
                hiddenJson -> hiddenKeys.add(ConnectorCurve.key(hiddenJson.getString("a"), hiddenJson.getString("b"))));
        return hiddenKeys;
    }

    public static <N extends TreeNode> List<N> parseNodes(JSONObject treeJson, String sourceName, Predicate<String> isVolume,
                                                          NodeFactory<N> factory) throws JSONException {
        List<N> nodes = new ArrayList<>();
        TreeJson.forEachEntry(treeJson.getJSONArray("nodes"), "tree node", sourceName,
                nodeJson -> nodes.add(parseNode(nodeJson, sourceName, isVolume, factory)));
        return nodes;
    }

    private static <N extends TreeNode> N parseNode(JSONObject nodeJson, String sourceName, Predicate<String> isVolume,
                                                    NodeFactory<N> factory) throws JSONException {
        List<String> connectedTo = TreeJson.stringList(nodeJson.optJSONArray("connectedTo"));
        List<String> tags = TreeJson.stringList(nodeJson.optJSONArray("tags"));

        String typeId = nodeJson.getString("type");
        SkillTier tier = factory.tierOf(typeId);
        if (tier == null) {
            throw new JSONException("Unknown node type \"" + typeId + "\"");
        }

        String pairedWith = nodeJson.optString("pairedWith", null);
        if (pairedWith != null && tier != SkillTier.WORMHOLE) {
            Logger.getLogger(TreeLayoutReader.class).error("Ignoring pairedWith on tree node \"" + nodeJson.getString("id")
                    + "\" in " + sourceName + ": only wormhole-tier nodes can be paired");
            pairedWith = null;
        }
        return factory.create(new NodeEntry(
                nodeJson.getString("id"),
                typeId,
                connectedTo,
                (float) nodeJson.optDouble("x", 0),
                (float) nodeJson.optDouble("y", 0),
                new SkillNodeDecoration(
                        nodeJson.optString("ringBeltPath", null),
                        nodeJson.optString("ringBeltColor", null),
                        nodeJson.has("ringBeltWidth") ? (float) nodeJson.getDouble("ringBeltWidth") : null,
                        nodeJson.optString("wormholeColor", null),
                        pairedWith),
                TreeTags.of(tags, isVolume)));
    }
}
