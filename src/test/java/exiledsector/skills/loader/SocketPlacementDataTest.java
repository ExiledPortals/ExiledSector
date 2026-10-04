package exiledsector.skills.loader;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketPlacementDataTest {

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    @Test
    void everyOtherNodeCanBeReachedWithoutPassingThroughASocketBecauseNpcsNeverTakeOne() throws Exception {
        RealSkillData.load();
        List<SkillNode> nodes = List.copyOf(SkillTree.getAllNodes().values());

        Set<String> reached = TreeReachability.fromRoots(nodes, node -> node.getType().getTier() != SkillTier.SOCKET);
        List<String> stranded = nodes.stream()
                .filter(node -> node.getType().getTier() != SkillTier.SOCKET && !reached.contains(node.getId()))
                .map(SkillNode::getId)
                .sorted()
                .toList();

        assertTrue(stranded.isEmpty(), "Only reachable through a Modular Hull Socket: " + stranded);
    }
}
