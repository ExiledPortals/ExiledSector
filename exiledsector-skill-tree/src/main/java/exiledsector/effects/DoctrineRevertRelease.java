package exiledsector.effects;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.items.DoctrineOverrides;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ReleasedNodes;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class DoctrineRevertRelease {

    private static final Logger LOG = Logger.getLogger(DoctrineRevertRelease.class);

    private DoctrineRevertRelease() {
    }

    static boolean release(FleetMemberAPI member) {
        ShipSkillData shipData = ShipSkillDataManager.find(member.getId());
        if (shipData == null || shipData.getReplacementAllocatedNodeIds().isEmpty()) {
            return false;
        }
        ReleasedNodes released = release(shipData, PlayerShipFacts.of(member, member.getVariant()), SkillTree.topology(),
                ShipTrees.resolveStartingRootId(shipData));
        if (released.nodeIds().isEmpty()) {
            return false;
        }
        released.refundTo(PlayerFleet.cargoOrNull());
        LOG.info("[ExiledSector] Released nodes of ship " + member.getId() + " that were allocated as doctrine replacements and whose originals"
                + " are not allowed on it: " + released.nodeIds());
        return true;
    }

    public static ReleasedNodes release(ShipSkillData shipData, ShipFacts shipFacts, SkillTreeTopology topology, String rootId) {
        if (shipData.getReplacementAllocatedNodeIds().isEmpty()) {
            return ReleasedNodes.NONE;
        }
        DoctrineOverrides.bindShipFacts(shipData, () -> shipFacts);
        List<SkillNode> reverted = new ArrayList<>();
        for (String nodeId : List.copyOf(shipData.getReplacementAllocatedNodeIds())) {
            SkillNode node = SkillTree.get(nodeId);
            if (node == null || !shipData.isAllocated(nodeId) && !shipData.getDormantNodeIds().contains(nodeId)) {
                shipData.keepAsOriginal(nodeId);
            } else if (shipData.isAllocated(nodeId) && DoctrineOverrides.replacementType(shipData, node) == null) {
                reverted.add(node);
            }
        }
        if (reverted.isEmpty()) {
            return ReleasedNodes.NONE;
        }
        return ShipTrees.releaseWithStranded(shipData, topology, rootId, () -> {
            for (SkillNode node : reverted) {
                if (allowedAsOriginal(shipData, shipFacts, node)) {
                    shipData.keepAsOriginal(node.getId());
                } else {
                    ShipTrees.deallocate(shipData, node);
                }
            }
        });
    }

    private static boolean allowedAsOriginal(ShipSkillData shipData, ShipFacts shipFacts, SkillNode node) {
        NodeEligibility.Context eligibility = NodeEligibility.Context.forShip(shipData, shipFacts, itemId -> 0d);
        SkillType option = NodeEligibility.validOption(node.getType(), shipData.getOptionalSelection(node.getId()));
        return NodeEligibility.check(node, option, eligibility.excluding(node.getId(), shipData)) == null;
    }
}
