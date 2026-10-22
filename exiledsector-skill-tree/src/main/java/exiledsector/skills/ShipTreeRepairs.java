package exiledsector.skills;

import exiledsector.persistence.ShipSkillDataManager;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ShipTreeRepairs {

    private ShipTreeRepairs() {
    }

    public static void replaceRemovedNodes(Map<String, SkillNode> nodesById, Map<String, String> replacements) {
        replacements.forEach((oldId, newId) -> {
            if (!nodesById.containsKey(oldId) && nodesById.containsKey(newId)) {
                ShipSkillDataManager.all().values().forEach(shipData -> ShipTrees.replaceNode(shipData, oldId, newId));
            }
        });
    }

    public static void forgetUnknownNodes(Map<String, SkillNode> nodesById, Map<String, SkillNode> declaredNodesById,
                                          Map<String, SkillType> typesById, Predicate<String> isOwnedShip,
                                          Consumer<SkillItemCost> refund) {
        Logger logger = Logger.getLogger(ShipTreeRepairs.class);
        for (Map.Entry<String, ShipSkillData> entry : ShipSkillDataManager.all().entrySet()) {
            ShipSkillData shipData = entry.getValue();
            List<String> forgottenNodeIds = ShipTrees.forgetUnknownNodes(shipData, nodesById, declaredNodesById, typesById);
            refundCharges(shipData, forgottenNodeIds, () -> isOwnedShip.test(entry.getKey()), refund);
            if (ShipTrees.hasLostStartingRoot(shipData, nodesById)) {
                List<String> releasedNodeIds = ShipTrees.resetAllocations(shipData);
                refundCharges(shipData, releasedNodeIds, () -> isOwnedShip.test(entry.getKey()), refund);
                logger.info("[ExiledSector] Reset the skill tree of ship " + entry.getKey()
                        + " because its starting root is no longer in the tree; released " + releasedNodeIds + ", removed " + forgottenNodeIds);
            } else if (!forgottenNodeIds.isEmpty()) {
                logger.info("[ExiledSector] Removed nodes that are no longer in the skill tree or whose chosen option is gone from ship " + entry.getKey() + ": " + forgottenNodeIds);
            }
        }
    }

    private static void refundCharges(ShipSkillData shipData, List<String> releasedNodeIds, BooleanSupplier ownedShip,
                                      Consumer<SkillItemCost> refund) {
        List<SkillItemCost> charges = ShipTrees.takeItemCharges(shipData, releasedNodeIds);
        if (!charges.isEmpty() && ownedShip.getAsBoolean()) {
            charges.forEach(refund);
        }
    }
}
