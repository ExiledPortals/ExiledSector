package exiledsector.charts.tree;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasModifier;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.skills.SkillTier;
import exiledsector.skills.decoration.Decorations;
import exiledsector.skills.layout.TreeTopology;
import exiledsector.skills.loader.TreeJson;
import exiledsector.skills.loader.TreeLayoutReader;
import exiledsector.skills.loader.WormholePairValidator;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AtlasTreeData {

    static final String TYPES_PATH = "data/skilltrees/atlas_tree_types.json";
    static final String TREE_PATH = "data/skilltrees/atlas_tree.json";
    private static final Logger LOG = Logger.getLogger(AtlasTreeData.class);

    private static AtlasTreeData loaded = empty();

    private final Map<String, AtlasTreeType> types;
    private final TreeTopology<AtlasTreeNode> topology;
    private final Decorations decorations;
    private final List<String> volumes;

    private AtlasTreeData(Map<String, AtlasTreeType> types, TreeLayoutReader.Layout<AtlasTreeNode> layout) {
        this.types = Map.copyOf(types);
        this.topology = TreeTopology.of(layout.nodes(), TreeTopology.ConnectorLinks.of(layout.connectorCurves(), layout.hiddenConnectors()));
        this.decorations = layout.decorations();
        this.volumes = List.copyOf(layout.volumes());
    }

    public static void load() {
        loaded = parse(TreeLayoutReader.load(TYPES_PATH), TreeLayoutReader.load(TREE_PATH));
    }

    public static AtlasTreeData get() {
        return loaded;
    }

    static void use(AtlasTreeData data) {
        loaded = data;
    }

    static AtlasTreeData empty() {
        return new AtlasTreeData(Map.of(), TreeLayoutReader.Layout.empty());
    }

    static AtlasTreeData parse(JSONObject typesJson, JSONObject treeJson) {
        Map<String, AtlasTreeType> types = parseTypes(typesJson);
        List<String> volumes = treeJson == null ? List.of() : TreeLayoutReader.parseVolumes(treeJson, TREE_PATH);
        Set<String> volumeIds = Set.copyOf(volumes);
        TreeLayoutReader.Layout<AtlasTreeNode> layout = TreeLayoutReader.read(treeJson, TREE_PATH, volumeIds::contains, nodeFactory(types));
        AtlasTreeData data = new AtlasTreeData(types, layout);
        for (String issue : WormholePairValidator.findIssues(layout.nodes())) {
            LOG.error("[ExiledSector] " + TREE_PATH + ": " + issue);
        }
        if (layout.declaredNodeCount() > 0 && data.topology.roots().isEmpty()) {
            LOG.error("[ExiledSector] " + TREE_PATH + " has no root node, so nothing on the Atlas Tree can be allocated");
        }
        return data;
    }

    private static TreeLayoutReader.NodeFactory<AtlasTreeNode> nodeFactory(Map<String, AtlasTreeType> types) {
        return new TreeLayoutReader.NodeFactory<>() {
            @Override
            public SkillTier tierOf(String typeId) {
                AtlasTreeType type = types.get(typeId);
                return type == null ? null : type.tier();
            }

            @Override
            public AtlasTreeNode create(TreeLayoutReader.NodeEntry entry) {
                return new AtlasTreeNode(entry.id(), types.get(entry.typeId()), entry.connectedTo(), entry.x(), entry.y(), entry.decoration(),
                        entry.treeTags());
            }
        };
    }

    static Map<String, AtlasTreeType> parseTypes(JSONObject typesJson) {
        Map<String, AtlasTreeType> types = new LinkedHashMap<>();
        if (typesJson == null) {
            return types;
        }
        TreeJson.forEachEntry(typesJson.optJSONArray("skillTypes"), "atlas tree type", TYPES_PATH, typeJson -> {
            AtlasTreeType type = parseType(typeJson);
            types.put(type.id(), type);
        });
        return types;
    }

    private static AtlasTreeType parseType(JSONObject typeJson) throws JSONException {
        String id = typeJson.getString("id");
        String tierName = typeJson.optString("tier", null);
        SkillTier tier = tierName == null ? SkillTier.SMALL : tierNamed(tierName);
        ParsedEffects effects = parseEffects(id, typeJson.optJSONArray("effects"));
        return new AtlasTreeType(id, typeJson.optString("name", id), typeJson.optString("icon", null), tier, effects.chartLines(), effects.atlasLines(),
                typeJson.optString("flavour", null), TreeJson.stringList(typeJson.optJSONArray("tags")));
    }

    private static SkillTier tierNamed(String tierName) throws JSONException {
        try {
            return SkillTier.valueOf(tierName);
        } catch (IllegalArgumentException e) {
            throw new JSONException("unknown tier \"" + tierName + "\"");
        }
    }

    private record ParsedEffects(List<ChartGenContext.AppliedEffect> chartLines, List<AtlasEffects.Line> atlasLines) {
    }

    private static ParsedEffects parseEffects(String typeId, JSONArray effectsJson) throws JSONException {
        ParsedEffects effects = new ParsedEffects(new ArrayList<>(), new ArrayList<>());
        if (effectsJson == null) {
            return effects;
        }
        for (int i = 0; i < effectsJson.length(); i++) {
            JSONObject effectJson = effectsJson.getJSONObject(i);
            String effectName = effectJson.getString("effect");
            float magnitude = (float) effectJson.getDouble("magnitude");
            ChartModifier chartEffect = ChartModifier.byNameOrNull(effectName);
            AtlasModifier atlasEffect = AtlasModifier.byNameOrNull(effectName);
            if (chartEffect != null) {
                effects.chartLines().add(new ChartGenContext.AppliedEffect(chartEffect, magnitude));
            } else if (atlasEffect != null) {
                effects.atlasLines().add(new AtlasEffects.Line(atlasEffect, magnitude));
            } else {
                LOG.error("[ExiledSector] Skipping effect " + effectName + " on atlas tree type \"" + typeId + "\" in " + TYPES_PATH
                        + ": not a chart or atlas effect");
            }
        }
        return effects;
    }

    public TreeTopology<AtlasTreeNode> topology() {
        return topology;
    }

    public AtlasTreeNode node(String nodeId) {
        return topology.node(nodeId);
    }

    public AtlasTreeType type(String typeId) {
        return types.get(typeId);
    }

    public Decorations decorations() {
        return decorations;
    }

    public List<String> volumes() {
        return volumes;
    }
}
