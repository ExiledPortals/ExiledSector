package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.NodeEntry;
import exiledsector.skills.NodeReplacements;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.items.NpcItems;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NpcTreeTag {

    public static final String PREFIX = "exiledSector_npcTree|";
    static final String LEGACY_PREFIX = "exiledSector_enemyTree|";
    private static final String FIELD_SEPARATOR = "|";
    private static final String NODE_SEPARATOR = ",";
    private static final String OPTION_SEPARATOR = "=";
    private static final int CHARGED_NODE_COST = 1;
    static final String GENERATED = "generated";
    static final String SOCKETS_MARKER = "sockets:";
    static final String LEGACY_ANCHORS_MARKER = "anchors:";
    private static final int MIN_FIELDS = 3;
    private static final int MAX_FIELDS = 7;
    private static final String[] NO_FIELDS = new String[0];

    private NpcTreeTag() {
    }

    public static String encode(ShipSkillData shipData) {
        List<String> nodeEntries = new ArrayList<>();
        for (String nodeId : shipData.getAllocatedNodeIds()) {
            nodeEntries.add(new NodeEntry(nodeId, shipData.getOptionalSelection(nodeId)).format());
        }
        String tag = PREFIX + GENERATED + FIELD_SEPARATOR + shipData.getLevel() + FIELD_SEPARATOR + String.join(NODE_SEPARATOR, nodeEntries);
        List<String> socketEntries = new ArrayList<>();
        shipData.getSocketedItems().forEach((nodeId, itemId) -> {
            if (NpcItems.isNpcId(itemId)) {
                socketEntries.add(nodeId + OPTION_SEPARATOR + itemId);
            }
        });
        return socketEntries.isEmpty() ? tag : tag + FIELD_SEPARATOR + SOCKETS_MARKER + String.join(NODE_SEPARATOR, socketEntries);
    }

    public static String find(ShipVariantAPI variant) {
        if (variant == null || variant.getTags() == null) {
            return null;
        }
        for (String tag : variant.getTags()) {
            if (prefixLength(tag) > 0) {
                return tag;
            }
        }
        return null;
    }

    public static void removeAll(ShipVariantAPI variant) {
        for (String tag = find(variant); tag != null; tag = find(variant)) {
            variant.removeTag(tag);
        }
    }

    public static int allocatedNodeCount(String tag) {
        String[] tagFields = fields(tag);
        if (tagFields.length == 0 || tagFields[2].isEmpty()) {
            return 0;
        }
        return tagFields[2].split(NODE_SEPARATOR, -1).length - 1;
    }

    public static ShipSkillData decode(String tag) {
        String[] tagFields = fields(tag);
        if (tagFields.length == 0) {
            return null;
        }
        int shipLevel;
        try {
            shipLevel = Integer.parseInt(tagFields[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        String[] nodeEntries = tagFields[2].isEmpty() ? new String[0] : tagFields[2].split(NODE_SEPARATOR, -1);
        NodeEntry rootEntry = nodeEntries.length == 0 ? null : NodeEntry.parse(nodeEntries[0]);
        SkillNode rootNode = rootEntry == null ? null : SkillTree.get(rootEntry.nodeId());
        ShipSkillData shipData = NpcSkillTreeBuilder.rootedTree(rootNode, shipLevel);
        if (shipData == null) {
            return NpcSkillTreeBuilder.emptyTree();
        }
        Map<String, String> anchorsBySocketId = new LinkedHashMap<>();
        for (int fieldIndex = MIN_FIELDS; fieldIndex < tagFields.length; fieldIndex++) {
            if (tagFields[fieldIndex].startsWith(SOCKETS_MARKER)) {
                readAnchors(anchorsBySocketId, tagFields[fieldIndex].substring(SOCKETS_MARKER.length()));
            } else if (tagFields[fieldIndex].startsWith(LEGACY_ANCHORS_MARKER)) {
                readLegacyAnchors(anchorsBySocketId, tagFields[fieldIndex].substring(LEGACY_ANCHORS_MARKER.length()));
            }
        }
        for (int i = 1; i < nodeEntries.length; i++) {
            NodeEntry entry = NodeEntry.parse(nodeEntries[i]);
            if (entry != null) {
                restore(shipData, entry, anchorsBySocketId);
            }
        }
        ShipTrees.releaseUnreachable(shipData, SkillTree.topology(), rootNode.getId());
        return shipData;
    }

    private static void readAnchors(Map<String, String> anchorsBySocketId, String socketEntries) {
        for (String entry : socketEntries.split(NODE_SEPARATOR)) {
            int separator = entry.indexOf(OPTION_SEPARATOR);
            String itemId = separator <= 0 ? null : entry.substring(separator + 1);
            if (itemId != null && NpcItems.isAnchor(itemId)) {
                anchorsBySocketId.putIfAbsent(NodeReplacements.current(entry.substring(0, separator)), itemId);
            }
        }
    }

    private static void readLegacyAnchors(Map<String, String> anchorsBySocketId, String anchorEntries) {
        for (String entry : anchorEntries.split(NODE_SEPARATOR)) {
            int separator = entry.indexOf(OPTION_SEPARATOR);
            SkillNode exit = separator <= 0 ? null : SkillTree.get(entry.substring(separator + 1));
            String anchorItemId = exit == null || exit.getType().getTier() != SkillTier.WORMHOLE_EXIT ? null : NpcItems.anchorFor(exit.getRegion());
            if (anchorItemId != null) {
                anchorsBySocketId.putIfAbsent(NodeReplacements.current(entry.substring(0, separator)), anchorItemId);
            }
        }
    }

    private static void restore(ShipSkillData shipData, NodeEntry entry, Map<String, String> anchorsBySocketId) {
        String nodeId = entry.nodeId();
        SkillNode node = SkillTree.get(nodeId);
        if (node == null || shipData.isAllocated(nodeId)) {
            return;
        }
        if (node.getType().getTier() == SkillTier.WORMHOLE_EXIT && ShipTreeLinks.anchorSocket(shipData, nodeId) != null) {
            ShipTrees.openAnchorWormholes(shipData, Integer.MAX_VALUE);
            if (shipData.isAllocated(nodeId)) {
                return;
            }
        }
        if (!node.getType().isOptional()) {
            ShipTrees.allocate(shipData, node, CHARGED_NODE_COST);
            socketAnchor(shipData, node, anchorsBySocketId.get(nodeId));
            return;
        }
        SkillType optionType = entry.optionId() == null ? null : NodeEligibility.validOption(node.getType(), entry.optionId());
        if (optionType != null) {
            ShipTrees.selectOption(shipData, node, optionType, CHARGED_NODE_COST);
        }
    }

    private static void socketAnchor(ShipSkillData shipData, SkillNode node, String anchorItemId) {
        if (anchorItemId != null && node.getType().getTier() == SkillTier.SOCKET) {
            shipData.socketItem(node.getId(), anchorItemId);
        }
    }

    private static String[] fields(String tag) {
        int prefixLength = prefixLength(tag);
        if (prefixLength == 0) {
            return NO_FIELDS;
        }
        String[] tagFields = tag.substring(prefixLength).split("\\" + FIELD_SEPARATOR, -1);
        return tagFields.length >= MIN_FIELDS && tagFields.length <= MAX_FIELDS && !tagFields[0].isEmpty() ? tagFields : NO_FIELDS;
    }

    private static int prefixLength(String tag) {
        if (tag == null) {
            return 0;
        }
        if (tag.startsWith(PREFIX)) {
            return PREFIX.length();
        }
        return tag.startsWith(LEGACY_PREFIX) ? LEGACY_PREFIX.length() : 0;
    }
}
