package exiledsector.skills;

import exiledsector.persistence.ShipSkillDataManager;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

public final class ShipTreeRepairs {

    private ShipTreeRepairs() {
    }

    public static void replaceRemovedNodes(Map<String, SkillNode> nodesById, Map<String, String> replacements) {
        replacements.keySet().forEach(oldId -> {
            String newId = NodeReplacements.current(oldId, replacements, nodesById::containsKey);
            if (newId.equals(oldId)) {
                return;
            }
            boolean renamed = renamedAllTheWay(oldId, newId, replacements);
            ShipSkillDataManager.all().values().forEach(shipData -> {
                if (renamed) {
                    ShipTrees.renameNode(shipData, oldId, newId);
                } else {
                    ShipTrees.replaceNode(shipData, oldId, newId);
                }
            });
        });
    }

    private static boolean renamedAllTheWay(String oldId, String newId, Map<String, String> replacements) {
        String id = oldId;
        for (int hop = 0; hop < replacements.size() && !id.equals(newId); hop++) {
            if (!NodeReplacements.isRename(id)) {
                return false;
            }
            id = replacements.get(id);
        }
        return id.equals(newId);
    }

    public static void forgetUnknownNodes(Map<String, SkillNode> nodesById, Map<String, SkillNode> declaredNodesById,
                                          Map<String, SkillType> typesById, Predicate<String> isOwnedShip,
                                          Consumer<SkillItemCost> refund) {
        forgetUnknownNodes(nodesById, declaredNodesById, typesById, isOwnedShip, refund, storyPoints -> {
        });
    }

    public static void forgetUnknownNodes(Map<String, SkillNode> nodesById, Map<String, SkillNode> declaredNodesById,
                                          Map<String, SkillType> typesById, Predicate<String> isOwnedShip,
                                          Consumer<SkillItemCost> refund, IntConsumer refundStoryPoints) {
        Logger logger = Logger.getLogger(ShipTreeRepairs.class);
        SkillTreeTopology topology = null;
        for (Map.Entry<String, ShipSkillData> entry : ShipSkillDataManager.all().entrySet()) {
            ShipSkillData shipData = entry.getValue();
            BooleanSupplier ownedShip = () -> isOwnedShip.test(entry.getKey());
            int refundedStoryPoints = IntegratedNodes.forgetInapplicable(shipData, declaredNodesById);
            if (refundedStoryPoints > 0 && ownedShip.getAsBoolean()) {
                refundStoryPoints.accept(refundedStoryPoints);
                logger.info("[ExiledSector] Refunded " + refundedStoryPoints + " story points for integrated nodes that no longer apply on ship "
                        + entry.getKey());
            }
            List<String> forgottenNodeIds = ShipTrees.forgetUnknownNodes(shipData, nodesById, declaredNodesById, typesById);
            refundCharges(shipData, forgottenNodeIds, ownedShip, refund);
            if (ShipTrees.hasLostStartingRoot(shipData, nodesById)) {
                List<String> releasedNodeIds = ShipTrees.resetAllocations(shipData);
                refundCharges(shipData, releasedNodeIds, ownedShip, refund);
                logger.info("[ExiledSector] Reset the skill tree of ship " + entry.getKey()
                        + " because its starting root is no longer in the tree; released " + releasedNodeIds + ", removed " + forgottenNodeIds);
            } else if (!forgottenNodeIds.isEmpty()) {
                topology = topology == null ? SkillTreeTopology.of(nodesById.values()) : topology;
                ReleasedNodes stranded = ShipTrees.releaseUnreachable(shipData, topology, ShipTrees.resolveStartingRootId(shipData, nodesById.values()));
                if (ownedShip.getAsBoolean()) {
                    stranded.charges().forEach(refund);
                }
                logger.info("[ExiledSector] Removed nodes that are no longer in the skill tree or whose chosen option is gone from ship " + entry.getKey()
                        + ": " + forgottenNodeIds + (stranded.isEmpty() ? "" : ", and released the nodes they stranded: " + stranded.nodeIds()));
            }
        }
    }

    private static void refundCharges(ShipSkillData shipData, List<String> releasedNodeIds, BooleanSupplier ownedShip,
                                      Consumer<SkillItemCost> refund) {
        List<SkillItemCost> charges = ItemCostLedger.takePaidItemCosts(shipData, releasedNodeIds);
        if (!charges.isEmpty() && ownedShip.getAsBoolean()) {
            charges.forEach(refund);
        }
    }
}
