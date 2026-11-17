package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import exiledsector.skills.SkillTier;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShippedAtlasOrbitsTest {

    private static final int MAX_CHANCE_NODES_PER_ANOMALY = 12;
    private static final List<String> ANOMALIES = List.of("spatial_breach", "relay_network", "supply_convoy", "anomalous_debris", "excavation_sites",
            "listening_post");

    private static AtlasTreeData shipped() throws Exception {
        JSONObject types = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TYPES_PATH));
        JSONObject tree = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasTreeData.TREE_PATH));
        JSONObject orbits = ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasOrbitReader.ORBITS_PATH));
        return AtlasTreeData.parse(types, tree, orbits);
    }

    private static boolean isWheelCentre(AtlasTreeNode node) {
        return node.getTier() == SkillTier.NOTABLE || node.getTier() == SkillTier.KEYSTONE;
    }

    @Test
    void everyTreeNodeHasAnOrbitAndEveryOrbitANode() throws Exception {
        AtlasTreeData data = shipped();
        Map<String, AtlasOrbit> orbits = AtlasOrbitReader.parse(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasOrbitReader.ORBITS_PATH)));
        Set<String> nodeIds = new HashSet<>();
        data.topology().nodes().forEach(node -> nodeIds.add(node.getId()));

        assertEquals(nodeIds, orbits.keySet());
        for (AtlasOrbit orbit : orbits.values()) {
            assertTrue(orbit.aroundId() == null || nodeIds.contains(orbit.aroundId()), orbit.nodeId() + " orbits missing " + orbit.aroundId());
        }
    }

    @Test
    void everyNotableAndKeystoneSitsOnceAtTheEndOfExactlyTwoSmallNodesOfItsOwnAnomalies() throws Exception {
        AtlasTreeData data = shipped();
        Map<String, Integer> centresByType = new HashMap<>();
        List<String> problems = new ArrayList<>();

        for (AtlasTreeNode centre : data.topology().nodes()) {
            if (!isWheelCentre(centre)) {
                continue;
            }
            centresByType.merge(centre.type().id(), 1, Integer::sum);
            List<String> path = leadUpOf(data, centre, problems);
            for (String smallId : path) {
                AtlasTreeNode small = data.node(smallId);
                if (small.getTier() != SkillTier.SMALL || !centre.getTags().containsAll(small.getTags()) || small.getTags().isEmpty()) {
                    problems.add(centre.getId() + " leads up through " + smallId + ", which is not a small node of its anomalies");
                }
            }
        }

        centresByType.forEach((type, count) -> {
            if (count != 1) {
                problems.add(type + " appears " + count + " times");
            }
        });
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static List<String> leadUpOf(AtlasTreeData data, AtlasTreeNode centre, List<String> problems) {
        List<String> path = new ArrayList<>();
        String previous = centre.getId();
        AtlasTreeNode current = centre;
        for (int step = 0; step < 2; step++) {
            String from = previous;
            List<String> onward = current.getConnectedNodeIds().stream().filter(id -> !id.equals(from)).toList();
            if (current == centre && current.getConnectedNodeIds().size() != 1 || current != centre && onward.size() != 1) {
                problems.add(current.getId() + " should have one way onward towards the highway, has " + onward);
                return path;
            }
            previous = current.getId();
            current = data.node(onward.get(0));
            path.add(current.getId());
        }
        String from = previous;
        List<String> highway = current.getConnectedNodeIds().stream().filter(id -> !id.equals(from)).toList();
        if (highway.size() != 1 || !data.node(highway.get(0)).getTags().isEmpty()) {
            problems.add(centre.getId() + "'s lead-up does not start on a highway node: " + highway);
        }
        return path;
    }

    @Test
    void noAnomalyHasMoreThanTwelveChanceNodes() throws Exception {
        AtlasTreeData data = shipped();

        for (String anomalyId : ANOMALIES) {
            long chanceNodes = data.topology().nodes().stream().filter(node -> node.type().id().equals(anomalyId + "_chance")).count();
            assertTrue(chanceNodes <= MAX_CHANCE_NODES_PER_ANOMALY, anomalyId + " has " + chanceNodes);
            assertTrue(chanceNodes > 0, anomalyId);
        }
    }

    @Test
    void everyNodeCanBeReachedFromTheStar() throws Exception {
        AtlasTreeData data = shipped();
        AtlasTreeNode root = data.topology().roots().get(0);
        Set<String> reached = new HashSet<>(Set.of(root.getId()));
        Deque<String> frontier = new ArrayDeque<>(reached);
        while (!frontier.isEmpty()) {
            for (AtlasTreeNode neighbour : data.topology().dependents(frontier.pop())) {
                if (reached.add(neighbour.getId())) {
                    frontier.add(neighbour.getId());
                }
            }
        }

        assertEquals(data.topology().nodes().size(), reached.size());
    }

    @Test
    void theHighwayRingsTurnSlowerFurtherFromTheStar() throws Exception {
        AtlasOrbitSystem system = shipped().orbits();
        List<AtlasOrbitSystem.Track> starTracks = system.tracks().stream()
                .filter(track -> !system.bodies().get(track.centreIndex()).orbits() && track.memberIndexes().size() > 3)
                .sorted((first, second) -> Float.compare(first.radius(), second.radius()))
                .toList();

        assertEquals(5, starTracks.size());
        for (int ring = 1; ring < starTracks.size(); ring++) {
            assertTrue(starTracks.get(ring).degreesPerSecond() < starTracks.get(ring - 1).degreesPerSecond(), "ring " + ring);
        }
    }
}
