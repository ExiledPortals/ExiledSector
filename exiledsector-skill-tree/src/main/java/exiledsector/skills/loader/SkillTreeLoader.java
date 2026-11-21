package exiledsector.skills.loader;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.decoration.Decorations;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillTreeLoader {

    private static final String DATA_PATH = "data/skilltrees/ship_skill_tree.json";

    private SkillTreeLoader() {
    }

    public static JSONObject loadRoot() {
        return TreeLayoutReader.load(DATA_PATH);
    }

    public static ParsedTree loadAll(Map<String, SkillType> skillTypes) {
        TreeLayoutReader.Layout<SkillNode> layout = TreeLayoutReader.read(loadRoot(), DATA_PATH, SkillTags::isVolume, nodeFactory(skillTypes));
        return new ParsedTree(layout.nodes(), layout.declaredNodeCount(), layout.connectorCurves(), layout.hiddenConnectors(), layout.decorations());
    }

    public static final class ParsedTree {
        public final List<SkillNode> nodes;
        public final int declaredNodeCount;
        public final Map<String, ConnectorCurve> connectorCurves;
        public final Set<String> hiddenConnectors;
        public final Decorations decorations;

        ParsedTree(List<SkillNode> nodes, int declaredNodeCount, Map<String, ConnectorCurve> connectorCurves, Set<String> hiddenConnectors,
                   Decorations decorations) {
            this.nodes = nodes;
            this.declaredNodeCount = declaredNodeCount;
            this.connectorCurves = connectorCurves;
            this.hiddenConnectors = hiddenConnectors;
            this.decorations = decorations;
        }
    }

    public static Map<String, ConnectorCurve> parseConnectorCurves(JSONObject treeJson) {
        return TreeLayoutReader.parseConnectorCurves(treeJson, DATA_PATH);
    }

    public static Set<String> parseHiddenConnectors(JSONObject treeJson) {
        return TreeLayoutReader.parseHiddenConnectors(treeJson, DATA_PATH);
    }

    public static List<SkillNode> parseNodes(JSONObject treeJson, Map<String, SkillType> skillTypes) throws JSONException {
        return TreeLayoutReader.parseNodes(treeJson, DATA_PATH, SkillTags::isVolume, nodeFactory(skillTypes));
    }

    private static TreeLayoutReader.NodeFactory<SkillNode> nodeFactory(Map<String, SkillType> skillTypes) {
        return new TreeLayoutReader.NodeFactory<>() {
            @Override
            public SkillTier tierOf(String typeId) {
                SkillType skillType = skillTypes.get(typeId);
                return skillType == null ? null : skillType.getTier();
            }

            @Override
            public SkillNode create(TreeLayoutReader.NodeEntry entry) {
                return new SkillNode(entry.id(), skillTypes.get(entry.typeId()), entry.connectedTo(), entry.x(), entry.y(), entry.decoration(),
                        entry.treeTags().tags());
            }
        };
    }
}
