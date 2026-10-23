package exiledsector.ui.node;

import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.NodeGrantStatus;
import exiledsector.skills.NodeGrants;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class GrantMarkers {

    private final Function<String, Color> frameColorByItemId;
    private ShipSkillData markedSkillData;
    private int markedDataRevision;
    private NodeGrants.GrantLookup markedLookup;
    private int markedLookupRevision;
    private Map<String, Color> colorByNodeId = Map.of();
    private Map<String, List<String>> nodeIdsBySocketId = Map.of();
    private Map<String, Color> colorBySocketId = Map.of();
    private List<String> grantingSocketIds = List.of();

    GrantMarkers(Function<String, Color> frameColorByItemId) {
        this.frameColorByItemId = frameColorByItemId;
    }

    static GrantMarkers fromItemStore() {
        return new GrantMarkers(GrantMarkers::itemFrameColor);
    }

    private static Color itemFrameColor(String itemId) {
        Item item = ItemStore.lookup(itemId);
        return item == null ? null : item.frameColor();
    }

    void refresh(NodeAllocator.Snapshot allocation) {
        ShipSkillData skillData = allocation.skillData();
        NodeGrants.GrantLookup lookup = NodeGrants.lookup();
        int lookupRevision = lookup.revision();
        if (skillData == markedSkillData && skillData.revision() == markedDataRevision && lookup == markedLookup
                && lookupRevision == markedLookupRevision) {
            return;
        }
        markedSkillData = skillData;
        markedDataRevision = skillData.revision();
        markedLookup = lookup;
        markedLookupRevision = lookupRevision;
        rebuild(allocation, skillData);
    }

    private void rebuild(NodeAllocator.Snapshot allocation, ShipSkillData skillData) {
        Map<String, Color> colors = new HashMap<>();
        Map<String, List<String>> activeNodeIdsBySocketId = new LinkedHashMap<>();
        Map<String, Color> socketColors = new HashMap<>();
        ShipTrees.grantedNodesBySocket(skillData).forEach((socketId, grantedNodeIds) -> {
            Color grantColor = frameColorByItemId.apply(skillData.getSocketedItem(socketId));
            if (grantColor == null) {
                return;
            }
            List<String> activeNodeIds = new ArrayList<>(grantedNodeIds.size());
            for (String grantedNodeId : grantedNodeIds) {
                NodeGrantStatus grantStatus = allocation.nodeGrantStatus(socketId, grantedNodeId);
                if (grantStatus != null && grantStatus.state() == NodeGrantStatus.State.ACTIVE) {
                    activeNodeIds.add(grantedNodeId);
                    colors.put(grantedNodeId, grantColor);
                }
            }
            if (!activeNodeIds.isEmpty()) {
                activeNodeIdsBySocketId.put(socketId, List.copyOf(activeNodeIds));
                socketColors.put(socketId, grantColor);
            }
        });
        colorByNodeId = colors.isEmpty() ? Map.of() : Collections.unmodifiableMap(colors);
        nodeIdsBySocketId = activeNodeIdsBySocketId.isEmpty() ? Map.of() : Collections.unmodifiableMap(activeNodeIdsBySocketId);
        colorBySocketId = socketColors.isEmpty() ? Map.of() : Collections.unmodifiableMap(socketColors);
        grantingSocketIds = List.copyOf(activeNodeIdsBySocketId.keySet());
    }

    Map<String, Color> colorByNodeId() {
        return colorByNodeId;
    }

    Color colorFor(String nodeId) {
        return colorByNodeId.get(nodeId);
    }

    List<String> grantingSocketIds() {
        return grantingSocketIds;
    }

    List<String> grantedBy(String socketId) {
        return nodeIdsBySocketId.getOrDefault(socketId, List.of());
    }

    Color colorForSocket(String socketId) {
        return colorBySocketId.get(socketId);
    }

    boolean isEmpty() {
        return colorByNodeId.isEmpty();
    }
}
