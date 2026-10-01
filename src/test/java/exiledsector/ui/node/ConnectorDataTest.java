package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorDataTest {

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    @Test
    void everyConnectionInTheRealTreeIsDrawnSoAllocationNeverFollowsAnInvisibleLink() throws Exception {
        RealSkillData.load();
        List<String> undrawn = new ArrayList<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            for (String connectedId : node.getConnectedNodeIds()) {
                SkillNode other = SkillTree.get(connectedId);
                if (other != null && !SkillTreeTopology.drawsEdge(node, other)
                        && !SkillTreeTopology.drawsEdge(other, node)) {
                    undrawn.add(node.getId() + " -> " + connectedId);
                }
            }
        }
        assertTrue(undrawn.isEmpty(), "Connections listed on one side only, which are never drawn: " + undrawn);
    }
}
