package exiledsector.ui.node;

import exiledsector.skills.ClearedSkillTreeExtension;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClearedSkillTreeExtension.class)
class ConnectorDataTest {

    @Test
    void everyConnectionInTheRealTreeIsDrawnSoAllocationNeverFollowsAnInvisibleLink() throws Exception {
        RealSkillData.load();
        List<String> undrawn = new ArrayList<>();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            for (String connectedId : node.getConnectedNodeIds()) {
                SkillNode other = SkillTree.get(connectedId);
                if (other != null && !SkillTree.topology().drawsEdge(node, other)
                        && !SkillTree.topology().drawsEdge(other, node)) {
                    undrawn.add(node.getId() + " -> " + connectedId);
                }
            }
        }
        assertTrue(undrawn.isEmpty(), "Connections listed on one side only, which are never drawn: " + undrawn);
    }
}
