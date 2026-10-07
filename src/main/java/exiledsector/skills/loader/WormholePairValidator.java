package exiledsector.skills.loader;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WormholePairValidator {

    private static final String WORMHOLE_NODE_PREFIX = "Wormhole node \"";
    private static final String PAIRED_WITH = "\" is paired with \"";

    private WormholePairValidator() {
    }

    public static List<String> findIssues(Iterable<SkillNode> nodes) {
        Map<String, SkillNode> nodesById = new HashMap<>();
        for (SkillNode node : nodes) {
            nodesById.put(node.getId(), node);
        }

        List<String> issues = new ArrayList<>();
        for (SkillNode node : nodesById.values()) {
            if (node.getType().getTier() != SkillTier.WORMHOLE) {
                continue;
            }
            String issue = validateWormholeNode(node, nodesById);
            if (issue != null) {
                issues.add(issue);
            }
        }
        return issues;
    }

    private static String validateWormholeNode(SkillNode node, Map<String, SkillNode> nodesById) {
        String pairedId = node.getPairedNodeId();
        if (pairedId == null) {
            return WORMHOLE_NODE_PREFIX + node.getId() + "\" has no pairedWith id set.";
        }
        if (pairedId.equals(node.getId())) {
            return WORMHOLE_NODE_PREFIX + node.getId() + "\" is paired with itself.";
        }

        SkillNode pairedNode = nodesById.get(pairedId);
        if (pairedNode == null) {
            return WORMHOLE_NODE_PREFIX + node.getId() + PAIRED_WITH + pairedId + "\", which does not exist.";
        }
        if (pairedNode.getType().getTier() != SkillTier.WORMHOLE) {
            return WORMHOLE_NODE_PREFIX + node.getId() + PAIRED_WITH + pairedId + "\", which is not a wormhole-tier node.";
        }
        if (!node.getId().equals(pairedNode.getPairedNodeId())) {
            return WORMHOLE_NODE_PREFIX + node.getId() + PAIRED_WITH + pairedId
                    + "\", but that node's pairedWith is \"" + pairedNode.getPairedNodeId() + "\" instead.";
        }
        return null;
    }
}
