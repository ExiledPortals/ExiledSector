package exiledsector.skills.loader;

import exiledsector.ProjectFiles;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;
import exiledsector.skills.decoration.Decoration;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.VolumeToggles;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VolumeToggleDataTest {

    private static final List<Set<String>> TOGGLED_VOLUMES = VolumeToggles.TOGGLED_VOLUMES;

    private static JSONObject treeJson() throws Exception {
        return ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(RealSkillData.TREE_FILE));
    }

    private static SkillTreeLoader.ParsedTree realTree() throws Exception {
        JSONObject root = treeJson();
        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(
                ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(RealSkillData.TYPES_FILE)));
        List<SkillNode> nodes = SkillTreeLoader.parseNodes(root, types);
        return new SkillTreeLoader.ParsedTree(nodes, nodes.size(), SkillTreeLoader.parseConnectorCurves(root),
                SkillTreeLoader.parseHiddenConnectors(root), DecorationLoader.parse(root, RealSkillData.TREE_FILE.toString(), SkillTags::isVolume));
    }

    @Test
    void everyDecorationCarriesExactlyOneVolumeTag() throws Exception {
        SkillTreeLoader.ParsedTree tree = realTree();
        List<String> problems = new ArrayList<>();
        for (Decoration decoration : tree.decorations.all()) {
            List<String> volumes = decoration.getTags().stream().filter(SkillTags::isVolume).toList();
            if (volumes.size() != 1) {
                problems.add(decoration.kind().dataKey() + " " + decoration.getId() + " has volume tags " + volumes);
            }
        }
        assertTrue(!tree.decorations.all().isEmpty(), "the tree has no decorations");

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void switchingAToggledVolumeOffStrandsNothingAndLeavesNoHalfWormholes() throws Exception {
        SkillTreeLoader.ParsedTree tree = realTree();
        List<String> problems = new ArrayList<>();
        for (Set<String> volume : TOGGLED_VOLUMES) {
            SkillTreeLoader.ParsedTree filtered = TreeVolumeFilter.apply(tree, volume);
            Map<String, SkillNode> byId = new HashMap<>();
            filtered.nodes.forEach(node -> byId.put(node.getId(), node));
            for (SkillNode node : filtered.nodes) {
                for (String other : node.getConnectedNodeIds()) {
                    if (!byId.containsKey(other)) {
                        problems.add(volume + " off: " + node.getId() + " still links to missing " + other);
                    }
                }
                if (node.getPairedNodeId() != null && !byId.containsKey(node.getPairedNodeId())) {
                    problems.add(volume + " off: wormhole " + node.getId() + " lost its pair " + node.getPairedNodeId());
                }
            }
            Set<String> reached = TreeReachability.fromRootsAndExits(filtered.nodes, node -> true);
            for (SkillNode node : filtered.nodes) {
                if (!reached.contains(node.getId())) {
                    problems.add(volume + " off: " + node.getId() + " can no longer be reached from any root or wormhole exit");
                }
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
