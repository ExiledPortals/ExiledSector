package exiledsector.ui.hyperspace;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;
import exiledsector.skills.loader.SkillTreeLoader;
import exiledsector.skills.loader.SkillTypeLoader;
import exiledsector.skills.npc.RealSkillData;
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
        JSONObject root = RealSkillData.readJson(RealSkillData.projectRoot().resolve(RealSkillData.TREE_FILE));
        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(
                RealSkillData.readJson(RealSkillData.projectRoot().resolve(RealSkillData.TYPES_FILE)));
        nodes = SkillTreeLoader.parseNodes(root, types);
        anchors = HyperspaceAnchor.collect(SkillTreeLoader.parseStars(root), SkillTreeLoader.parseStaticImages(root));
    }

    @Test
    void everyRegionWithNodesHasAStarOrNebulaOnTheHyperspaceMap() {
        Set<String> missing = new TreeSet<>();
        for (SkillNode node : nodes) {
            String region = node.getRegion();
            if (region != null && anchors.stream().noneMatch(anchor -> anchor.region().equals(region))) {
                missing.add(region);
            }
        }

        assertTrue(missing.isEmpty(), "Regions with no hyperspace anchor: " + missing);
    }

    @Test
    void everyWormholeEndIsTaggedWithTheRegionOfTheStarItSitsBeside() {
        List<String> problems = new ArrayList<>();
        for (SkillNode node : nodes) {
            if (node.getPairedNodeId() == null) {
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
            if (nearest != null && !nearest.region().equals(node.getRegion())) {
                problems.add(node.getId() + " is tagged " + node.getRegion() + " but sits beside " + nearest.id()
                        + " (" + nearest.region() + ")");
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
