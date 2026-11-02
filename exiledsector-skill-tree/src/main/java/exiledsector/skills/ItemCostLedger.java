package exiledsector.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemCostLedger {

    private ItemCostLedger() {
    }

    public static SkillItemCost paidItemCost(ShipSkillData data, String nodeId) {
        String itemId = paidItemCostIds(data).get(nodeId);
        return itemId == null ? null : new SkillItemCost(itemId, data.paidItemCostQuantities.getOrDefault(nodeId, 0f));
    }

    public static void recordPaidItemCost(ShipSkillData data, String nodeId, SkillItemCost itemCost) {
        paidItemCostIds(data).put(nodeId, itemCost.itemId());
        data.paidItemCostQuantities.put(nodeId, itemCost.quantity());
    }

    public static SkillItemCost takePaidItemCost(ShipSkillData data, String nodeId) {
        SkillItemCost charged = paidItemCost(data, nodeId);
        if (charged != null) {
            discard(data, nodeId);
        }
        return charged;
    }

    public static List<SkillItemCost> takePaidItemCosts(ShipSkillData data, Collection<String> nodeIds) {
        List<SkillItemCost> charges = new ArrayList<>();
        for (String nodeId : nodeIds) {
            SkillItemCost charged = takePaidItemCost(data, nodeId);
            if (charged != null) {
                charges.add(charged);
            }
        }
        return charges;
    }

    static void discard(ShipSkillData data, String nodeId) {
        paidItemCostIds(data).remove(nodeId);
        data.paidItemCostQuantities.remove(nodeId);
    }

    private static Map<String, String> paidItemCostIds(ShipSkillData data) {
        ensure(data);
        return data.paidItemCostIds;
    }

    static void ensure(ShipSkillData data) {
        if (data.paidItemCostIds != null && data.paidItemCostQuantities != null) {
            return;
        }
        data.paidItemCostIds = new LinkedHashMap<>();
        data.paidItemCostQuantities = new LinkedHashMap<>();
        if (data.npcBuild) {
            return;
        }
        for (String nodeId : data.allocatedNodeIds) {
            SkillNode node = SkillTree.getDeclared(nodeId);
            SkillItemCost legacyCharge = node == null ? null : node.getType().getItemCost();
            if (legacyCharge != null) {
                data.paidItemCostIds.put(nodeId, legacyCharge.itemId());
                data.paidItemCostQuantities.put(nodeId, legacyCharge.quantity());
            }
        }
    }
}
