package exiledsector.persistence;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.fs.starfarer.api.Global;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;
import org.apache.log4j.Logger;

public class ShipSkillDataManager {

    private static final String DATA_KEY = "exiledSector_shipSkillData";

    private ShipSkillDataManager() {
    }

    // persistentData is a raw Object map; this key is only ever written as Map<String, ShipSkillData>
    @SuppressWarnings("unchecked")
    private static Map<String, ShipSkillData> getStore() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        return (Map<String, ShipSkillData>) persistentData.computeIfAbsent(DATA_KEY, key -> new HashMap<String, ShipSkillData>());
    }

    public static ShipSkillData get(String shipId) {
        return getStore().computeIfAbsent(shipId, key -> new ShipSkillData());
    }

    public static Map<String, ShipSkillData> all() {
        return Collections.unmodifiableMap(getStore());
    }

    public static ShipSkillData find(String shipId) {
        return getStore().get(shipId);
    }

    public static boolean hasProgress(String shipId) {
        ShipSkillData shipData = find(shipId);
        return shipData != null && !shipData.isBlank();
    }

    public static void remove(String shipId) {
        getStore().remove(shipId);
    }

    public static void removeBlankRecords() {
        getStore().values().removeIf(ShipSkillData::isBlank);
    }

    public static void replaceRemovedNodes(Map<String, SkillNode> nodesById, Map<String, String> replacements) {
        replacements.forEach((oldId, newId) -> {
            if (!nodesById.containsKey(oldId) && nodesById.containsKey(newId)) {
                getStore().values().forEach(shipData -> shipData.replaceNode(oldId, newId));
            }
        });
    }

    public static void forgetUnknownNodes(Map<String, SkillNode> nodesById, Map<String, SkillNode> declaredNodesById,
                                          Map<String, SkillType> typesById, int maxAllocatedNodes, Predicate<String> isOwnedShip,
                                          Consumer<SkillItemCost> refund) {
        Logger logger = Logger.getLogger(ShipSkillDataManager.class);
        for (Map.Entry<String, ShipSkillData> entry : getStore().entrySet()) {
            ShipSkillData shipData = entry.getValue();
            List<String> forgottenNodeIds = shipData.forgetUnknownNodes(nodesById, declaredNodesById, typesById);
            refundCharges(shipData, forgottenNodeIds, () -> isOwnedShip.test(entry.getKey()), refund);
            List<String> strandedNodeIds = shipData.wakeDormantNodes(nodesById, maxAllocatedNodes);
            refundCharges(shipData, strandedNodeIds, () -> isOwnedShip.test(entry.getKey()), refund);
            if (!strandedNodeIds.isEmpty()) {
                logger.info("[ExiledSector] Released hidden-area nodes of ship " + entry.getKey()
                        + " that no longer connect to its tree or no longer fit under the node cap: " + strandedNodeIds);
            }
            if (shipData.hasLostStartingRoot(nodesById)) {
                List<String> releasedNodeIds = shipData.resetAllocations();
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
        for (String nodeId : releasedNodeIds) {
            SkillItemCost charged = shipData.takeItemCharge(nodeId);
            if (charged != null && ownedShip.getAsBoolean()) {
                refund.accept(charged);
            }
        }
    }

    public static void put(String shipId, ShipSkillData shipData) {
        getStore().put(shipId, shipData);
    }
}
