package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.NodeReplacements;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.socketables.NpcSocketables;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.SocketableItemData;

import java.util.ArrayList;
import java.util.List;

public final class NpcTreeTag {

    public static final String PREFIX = "exiledSector_npcTree|";
    static final String LEGACY_PREFIX = "exiledSector_enemyTree|";
    private static final String FIELD_SEPARATOR = "|";
    private static final String NODE_SEPARATOR = ",";
    private static final String OPTION_SEPARATOR = "=";
    private static final int CHARGED_NODE_COST = 1;
    static final String GENERATED = "generated";
    static final String SOCKETS_MARKER = "sockets:";
    private static final String[] NO_FIELDS = new String[0];

    private NpcTreeTag() {
    }

    public static String encode(ShipSkillData shipData) {
        List<String> nodeEntries = new ArrayList<>();
        for (String nodeId : shipData.getAllocatedNodeIds()) {
            String optionId = shipData.getOptionalSelection(nodeId);
            nodeEntries.add(optionId == null ? nodeId : nodeId + OPTION_SEPARATOR + optionId);
        }
        String tag = PREFIX + GENERATED + FIELD_SEPARATOR + shipData.getLevel() + FIELD_SEPARATOR + String.join(NODE_SEPARATOR, nodeEntries);
        List<String> socketEntries = new ArrayList<>();
        shipData.getSocketedItems().forEach((nodeId, socketableId) -> {
            if (NpcSocketables.isNpcId(socketableId)) {
                socketEntries.add(nodeId + OPTION_SEPARATOR + socketableId);
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
        SkillNode rootNode = nodeEntries.length == 0 ? null : SkillTree.get(nodeEntries[0]);
        ShipSkillData shipData = NpcSkillTreeBuilder.rootedTree(rootNode, shipLevel);
        if (shipData == null) {
            return NpcSkillTreeBuilder.emptyTree();
        }
        for (int i = 1; i < nodeEntries.length; i++) {
            restore(shipData, nodeEntries[i]);
        }
        if (tagFields.length == 4 && tagFields[3].startsWith(SOCKETS_MARKER)) {
            restoreSockets(shipData, tagFields[3].substring(SOCKETS_MARKER.length()));
        }
        return shipData;
    }

    private static void restoreSockets(ShipSkillData shipData, String socketEntries) {
        for (String entry : socketEntries.split(NODE_SEPARATOR)) {
            int separator = entry.indexOf(OPTION_SEPARATOR);
            if (separator <= 0) {
                continue;
            }
            String nodeId = NodeReplacements.resolve(entry.substring(0, separator));
            String socketableId = entry.substring(separator + 1);
            SkillNode node = SkillTree.get(nodeId);
            SocketableItemData itemData = NpcSocketables.item(socketableId);
            boolean fitsTreeSocket = itemData != null
                    && (itemData.definition() == null || itemData.definition().kind() == SocketType.SUBROUTINE);
            if (node != null && node.getType().getTier() == SkillTier.SOCKET && fitsTreeSocket) {
                shipData.socketItem(nodeId, socketableId);
            }
        }
    }

    private static void restore(ShipSkillData shipData, String entry) {
        int optionAt = entry.indexOf(OPTION_SEPARATOR);
        String nodeId = NodeReplacements.resolve(optionAt < 0 ? entry : entry.substring(0, optionAt));
        SkillNode node = SkillTree.get(nodeId);
        if (node == null || shipData.isAllocated(nodeId)) {
            return;
        }
        if (optionAt < 0 || !node.getType().isOptional()) {
            if (!node.getType().isOptional()) {
                shipData.allocate(node, CHARGED_NODE_COST);
            }
            return;
        }
        String optionId = entry.substring(optionAt + 1);
        SkillType optionType = SkillTree.getType(optionId);
        if (optionType != null && node.getType().getOptionalOptionIds().contains(optionId)) {
            shipData.selectOption(node, optionType, CHARGED_NODE_COST);
        }
    }

    private static String[] fields(String tag) {
        int prefixLength = prefixLength(tag);
        if (prefixLength == 0) {
            return NO_FIELDS;
        }
        String[] tagFields = tag.substring(prefixLength).split("\\" + FIELD_SEPARATOR, -1);
        return (tagFields.length == 3 || tagFields.length == 4) && !tagFields[0].isEmpty() ? tagFields : NO_FIELDS;
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
