package exiledsector.ui.hyperspace;

import exiledsector.ProjectFiles;
import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;
import exiledsector.skills.decoration.Decorations;
import exiledsector.skills.loader.DecorationLoader;
import exiledsector.skills.loader.SkillTreeLoader;
import exiledsector.skills.loader.SkillTypeLoader;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperspaceDataTest {

    private static List<SkillNode> nodes;
    private static List<HyperspaceAnchor> anchors;

    @BeforeAll
    static void loadRealTree() throws Exception {
        JSONObject root = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(RealSkillData.TREE_FILE));
        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(
                ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(RealSkillData.TYPES_FILE)));
        nodes = SkillTreeLoader.parseNodes(root, types);
        Decorations decorations = DecorationLoader.parse(root, RealSkillData.TREE_FILE.toString(), SkillTags::isVolume);
        anchors = HyperspaceAnchor.collect(decorations);
    }

    @Test
    void everyVolumeWithNodesHasAStarOrNebulaOnTheHyperspaceMap() {
        Set<String> missing = new TreeSet<>();
        for (SkillNode node : nodes) {
            String volume = node.getVolume();
            if (volume != null && !ExoshipNodes.isCarried(node) && anchors.stream().noneMatch(anchor -> anchor.volume().equals(volume))) {
                missing.add(volume);
            }
        }

        assertTrue(missing.isEmpty(), "Volumes with no hyperspace anchor: " + missing);
    }

    @Test
    void everyWormholeEndIsTaggedWithTheVolumeOfTheStarItSitsBeside() {
        List<String> problems = new ArrayList<>();
        for (SkillNode node : nodes) {
            if (node.getPairedNodeId() == null || ExoshipNodes.isCarried(node)) {
                continue;
            }
            HyperspaceAnchor nearest = null;
            float best = Float.POSITIVE_INFINITY;
            for (HyperspaceAnchor anchor : anchors) {
                float dx = anchor.x() - node.getOffsetX();
                float dy = anchor.y() - node.getOffsetY();
                if (dx * dx + dy * dy < best) {
                    best = dx * dx + dy * dy;
                    nearest = anchor;
                }
            }
            if (nearest != null && !nearest.volume().equals(node.getVolume())) {
                problems.add(node.getId() + " is tagged " + node.getVolume() + " but sits beside " + nearest.id()
                        + " (" + nearest.volume() + ")");
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
