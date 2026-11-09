package exiledsector.skills;

import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.TreeTopology;

import java.util.Collection;

public final class SkillTreeTopology extends TreeTopology<SkillNode> {

    private static final ConnectorLinks ACTIVE_TREE_LINKS = new ConnectorLinks() {
        @Override
        public ConnectorCurve curve(String firstNodeId, String secondNodeId) {
            return SkillTree.getCurve(firstNodeId, secondNodeId);
        }

        @Override
        public boolean isVisible(String firstNodeId, String secondNodeId) {
            return SkillTree.isConnectorVisible(firstNodeId, secondNodeId);
        }
    };

    private SkillTreeTopology(Collection<SkillNode> nodes) {
        super(nodes, ACTIVE_TREE_LINKS);
    }

    public static SkillTreeTopology of(Collection<SkillNode> nodes) {
        return new SkillTreeTopology(nodes);
    }
}
