package exiledsector.skills.loader;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillTreeObject;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.tags.AreaToggles;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaToggleDataTest {

    private static final List<String> TOGGLED_REGIONS = List.of(AreaToggles.LOST_SECTOR_REGION);

    private static JSONObject treeJson() throws Exception {
        return RealSkillData.readJson(RealSkillData.projectRoot().resolve(RealSkillData.TREE_FILE));
    }

    private static SkillTreeLoader.ParsedTree realTree() throws Exception {
        JSONObject root = treeJson();
        Map<String, SkillType> types = SkillTypeLoader.parseSkillTypes(
                RealSkillData.readJson(RealSkillData.projectRoot().resolve(RealSkillData.TYPES_FILE)));
        List<SkillNode> nodes = SkillTreeLoader.parseNodes(root, types);
        return new SkillTreeLoader.ParsedTree(nodes, nodes.size(), SkillTreeLoader.parseConnectorCurves(root),
                SkillTreeLoader.parseHiddenConnectors(root), SkillTreeLoader.parseStaticImages(root),
                SkillTreeLoader.parseRingBelts(root), SkillTreeLoader.parseStars(root));
    }

    private static void requireOneRegion(String kind, List<? extends SkillTreeObject> objects, List<String> problems) {
        for (SkillTreeObject object : objects) {
            List<String> regions = object.getTags().stream().filter(SkillTags::isRegion).toList();
            if (regions.size() != 1) {
                problems.add(kind + " " + object.getId() + " has region tags " + regions);
            }
        }
    }

    @Test
    void everyStarRingBeltAndStaticImageCarriesExactlyOneRegionTag() throws Exception {
        SkillTreeLoader.ParsedTree tree = realTree();
        List<String> problems = new ArrayList<>();
        requireOneRegion("star", tree.stars, problems);
        requireOneRegion("ring belt", tree.ringBelts, problems);
        requireOneRegion("static image", tree.staticImages, problems);
        JSONArray anchors = treeJson().optJSONArray("orbitAnchors");
        for (int i = 0; anchors != null && i < anchors.length(); i++) {
            JSONObject anchor = anchors.getJSONObject(i);
            if ("asteroidBelt".equals(anchor.optString("shape")) && anchor.optJSONArray("tags") == null) {
                problems.add("ring belt anchor " + anchor.optString("id") + " has no region tag");
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void switchingAToggledAreaOffStrandsNothingAndLeavesNoHalfWormholes() throws Exception {
        SkillTreeLoader.ParsedTree tree = realTree();
        List<String> problems = new ArrayList<>();
        for (String region : TOGGLED_REGIONS) {
            SkillTreeLoader.ParsedTree filtered = TreeRegionFilter.apply(tree, Set.of(region));
            Map<String, SkillNode> byId = new HashMap<>();
            filtered.nodes.forEach(node -> byId.put(node.getId(), node));
            for (SkillNode node : filtered.nodes) {
                for (String other : node.getConnectedNodeIds()) {
                    if (!byId.containsKey(other)) {
                        problems.add(region + " off: " + node.getId() + " still links to missing " + other);
                    }
                }
                if (node.getPairedNodeId() != null && !byId.containsKey(node.getPairedNodeId())) {
                    problems.add(region + " off: wormhole " + node.getId() + " lost its pair " + node.getPairedNodeId());
                }
            }
            Set<String> reached = reachableFromRoots(filtered.nodes, byId);
            for (SkillNode node : filtered.nodes) {
                if (!reached.contains(node.getId())) {
                    problems.add(region + " off: " + node.getId() + " can no longer be reached from any root");
                }
            }
        }

        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static Set<String> reachableFromRoots(List<SkillNode> nodes, Map<String, SkillNode> byId) {
        Map<String, Set<String>> links = new HashMap<>();
        for (SkillNode node : nodes) {
            for (String other : node.getConnectedNodeIds()) {
                links.computeIfAbsent(node.getId(), key -> new HashSet<>()).add(other);
                links.computeIfAbsent(other, key -> new HashSet<>()).add(node.getId());
            }
        }
        Set<String> reached = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        for (SkillNode node : nodes) {
            if (node.getType().getTier() == SkillTier.ROOT && reached.add(node.getId())) {
                queue.add(node.getId());
            }
        }
        while (!queue.isEmpty()) {
            for (String next : links.getOrDefault(queue.poll(), Set.of())) {
                if (byId.containsKey(next) && reached.add(next)) {
                    queue.add(next);
                }
            }
        }
        return reached;
    }
}
