package exiledsector.effects;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.items.DoctrineOverrides;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ReleasedNodes;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        released.charges().forEach(charged -> charged.refundTo(cargo));
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
        Set<String> allocatedBefore = new LinkedHashSet<>(shipData.getAllocatedNodeIds());
        Set<String> reachableBefore = ShipTrees.reachableAllocatedNodeIds(shipData, topology, rootId);
        List<SkillItemCost> charges = new ArrayList<>();
        for (SkillNode node : reverted) {
            NodeEligibility.Context eligibility = NodeEligibility.Context.of(shipData, shipFacts, SkillTypeUnlockStatus.lockedFor(shipData),
                    itemId -> 0d);
            SkillType option = NodeEligibility.validOption(node.getType(), shipData.getOptionalSelection(node.getId()));
            if (NodeEligibility.check(node, option, eligibility.excluding(node.getId(), shipData)) == null) {
                shipData.keepAsOriginal(node.getId());
            } else {
                charges.addAll(ShipTrees.deallocateReleasingCharges(shipData, node));
            }
        }
        if (allocatedBefore.equals(shipData.getAllocatedNodeIds())) {
            return ReleasedNodes.NONE;
        }
        charges.addAll(ShipTrees.releaseNewlyStranded(shipData, topology, rootId, reachableBefore).charges());
        allocatedBefore.removeAll(shipData.getAllocatedNodeIds());
        return new ReleasedNodes(List.copyOf(allocatedBefore), charges);
    }
}
