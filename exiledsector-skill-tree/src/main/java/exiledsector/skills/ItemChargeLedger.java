package exiledsector.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ItemChargeLedger {

    private ItemChargeLedger() {
    }

    public static SkillItemCost itemCharge(ShipSkillData data, String nodeId) {
        String itemId = chargedItemIds(data).get(nodeId);
        return itemId == null ? null : new SkillItemCost(itemId, data.chargedItemQuantities.getOrDefault(nodeId, 0f));
    }

    public static void recordItemCharge(ShipSkillData data, String nodeId, SkillItemCost itemCost) {
        chargedItemIds(data).put(nodeId, itemCost.itemId());
        data.chargedItemQuantities.put(nodeId, itemCost.quantity());
    }

    public static SkillItemCost takeItemCharge(ShipSkillData data, String nodeId) {
        SkillItemCost charged = itemCharge(data, nodeId);
        if (charged != null) {
            discard(data, nodeId);
        }
        return charged;
    }

    public static List<SkillItemCost> takeItemCharges(ShipSkillData data, Collection<String> nodeIds) {
        List<SkillItemCost> charges = new ArrayList<>();
        for (String nodeId : nodeIds) {
            SkillItemCost charged = takeItemCharge(data, nodeId);
            if (charged != null) {
                charges.add(charged);
            }
        }
        return charges;
    }

    static void discard(ShipSkillData data, String nodeId) {
        chargedItemIds(data).remove(nodeId);
        data.chargedItemQuantities.remove(nodeId);
    }

    private static Map<String, String> chargedItemIds(ShipSkillData data) {
        ensure(data);
        return data.chargedItemIds;
    }

    static void ensure(ShipSkillData data) {
        if (data.chargedItemIds != null && data.chargedItemQuantities != null) {
            return;
        }
        data.chargedItemIds = new LinkedHashMap<>();
        data.chargedItemQuantities = new LinkedHashMap<>();
        if (data.npcBuild) {
            return;
        }
        for (String nodeId : data.allocatedNodeIds) {
            SkillNode node = SkillTree.getDeclared(nodeId);
            SkillItemCost legacyCharge = node == null ? null : node.getType().getItemCost();
            if (legacyCharge != null) {
                data.chargedItemIds.put(nodeId, legacyCharge.itemId());
                data.chargedItemQuantities.put(nodeId, legacyCharge.quantity());
            }
        }
    }
}
