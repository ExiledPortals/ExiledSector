package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.NodeReplacements;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.socketables.HullFrameworkData;
import exiledsector.socketables.NpcSocketables;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.SocketableItemData;
import org.apache.log4j.Logger;

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
    static final String FRAMEWORK_MARKER = "framework:";
    static final String FRAMEWORK_SOCKETS_MARKER = "frameworkSockets:";
    private static final int MIN_FIELDS = 3;
    private static final int MAX_FIELDS = 6;
    private static final String[] NO_FIELDS = new String[0];
    private static final Logger LOG = Logger.getLogger(NpcTreeTag.class);

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
        StringBuilder encodedTag = new StringBuilder(tag);
        if (!socketEntries.isEmpty()) {
            encodedTag.append(FIELD_SEPARATOR).append(SOCKETS_MARKER).append(String.join(NODE_SEPARATOR, socketEntries));
        }
        String frameworkId = shipData.getInstalledFrameworkId();
        if (HullFrameworkData.isNpcId(frameworkId)) {
            encodedTag.append(FIELD_SEPARATOR).append(FRAMEWORK_MARKER).append(frameworkId);
            List<String> frameworkSocketEntries = new ArrayList<>();
            shipData.getFrameworkSocketedItems().forEach((slotIndex, socketableId) -> {
                if (NpcSocketables.isNpcId(socketableId)) {
                    frameworkSocketEntries.add(slotIndex + OPTION_SEPARATOR + socketableId);
                }
            });
            if (!frameworkSocketEntries.isEmpty()) {
                encodedTag.append(FIELD_SEPARATOR).append(FRAMEWORK_SOCKETS_MARKER).append(String.join(NODE_SEPARATOR, frameworkSocketEntries));
            }
        }
        return encodedTag.toString();
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
        for (int fieldIndex = MIN_FIELDS; fieldIndex < tagFields.length; fieldIndex++) {
            restoreField(shipData, tagFields[fieldIndex]);
        }
        return shipData;
    }

    private static void restoreField(ShipSkillData shipData, String tagField) {
        if (tagField.startsWith(SOCKETS_MARKER)) {
            restoreSockets(shipData, tagField.substring(SOCKETS_MARKER.length()));
        } else if (tagField.startsWith(FRAMEWORK_MARKER)) {
            String frameworkId = tagField.substring(FRAMEWORK_MARKER.length());
            if (HullFrameworkData.ofNpcId(frameworkId) != null) {
                shipData.installFramework(frameworkId);
            }
        } else if (tagField.startsWith(FRAMEWORK_SOCKETS_MARKER) && shipData.getInstalledFrameworkId() != null) {
            restoreFrameworkSockets(shipData, tagField.substring(FRAMEWORK_SOCKETS_MARKER.length()));
        }
    }

    private static void restoreFrameworkSockets(ShipSkillData shipData, String socketEntries) {
        for (String entry : socketEntries.split(NODE_SEPARATOR)) {
            int separator = entry.indexOf(OPTION_SEPARATOR);
            String socketableId = separator <= 0 ? null : entry.substring(separator + 1);
            if (socketableId != null && NpcSocketables.item(socketableId) != null) {
                try {
                    shipData.socketFrameworkItem(Integer.parseInt(entry.substring(0, separator)), socketableId);
                } catch (NumberFormatException e) {
                    LOG.warn("Skipping an NPC framework socket entry with a bad slot: " + entry);
                }
            }
        }
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
