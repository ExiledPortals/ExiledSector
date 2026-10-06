package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.NodeReplacements;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.socketables.NpcSocketables;

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

    public static String encode(ShipSkillData data) {
        List<String> nodes = new ArrayList<>();
        for (String nodeId : data.getAllocatedNodeIds()) {
            String option = data.getOptionalSelection(nodeId);
            nodes.add(option == null ? nodeId : nodeId + OPTION_SEPARATOR + option);
        }
        String tag = PREFIX + GENERATED + FIELD_SEPARATOR + data.getLevel() + FIELD_SEPARATOR + String.join(NODE_SEPARATOR, nodes);
        List<String> sockets = new ArrayList<>();
        data.getSocketedItems().forEach((nodeId, socketableId) -> {
            if (NpcSocketables.isNpcId(socketableId)) {
                sockets.add(nodeId + OPTION_SEPARATOR + socketableId);
            }
        });
        return sockets.isEmpty() ? tag : tag + FIELD_SEPARATOR + SOCKETS_MARKER + String.join(NODE_SEPARATOR, sockets);
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
        String[] fields = fields(tag);
        if (fields.length == 0) {
            return null;
        }
        int level;
        try {
            level = Integer.parseInt(fields[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        String[] entries = fields[2].isEmpty() ? new String[0] : fields[2].split(NODE_SEPARATOR, -1);
        SkillNode root = entries.length == 0 ? null : SkillTree.get(entries[0]);
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(root, level);
        if (data == null) {
            return NpcSkillTreeBuilder.emptyTree();
        }
        for (int i = 1; i < entries.length; i++) {
            restore(data, entries[i]);
        }
        if (fields.length == 4 && fields[3].startsWith(SOCKETS_MARKER)) {
            restoreSockets(data, fields[3].substring(SOCKETS_MARKER.length()));
        }
        return data;
    }

    private static void restoreSockets(ShipSkillData data, String sockets) {
        for (String entry : sockets.split(NODE_SEPARATOR)) {
            int separator = entry.indexOf(OPTION_SEPARATOR);
            if (separator <= 0) {
                continue;
            }
            String nodeId = NodeReplacements.resolve(entry.substring(0, separator));
            String socketableId = entry.substring(separator + 1);
            SkillNode node = SkillTree.get(nodeId);
            if (node != null && node.getType().getTier() == SkillTier.SOCKET && NpcSocketables.item(socketableId) != null) {
                data.socketItem(nodeId, socketableId);
            }
        }
    }

    private static void restore(ShipSkillData data, String entry) {
        int optionAt = entry.indexOf(OPTION_SEPARATOR);
        String nodeId = NodeReplacements.resolve(optionAt < 0 ? entry : entry.substring(0, optionAt));
        SkillNode node = SkillTree.get(nodeId);
        if (node == null || data.isAllocated(nodeId)) {
            return;
        }
        if (optionAt < 0 || !node.getType().isOptional()) {
            if (!node.getType().isOptional()) {
                data.allocate(node, CHARGED_NODE_COST);
            }
            return;
        }
        String optionId = entry.substring(optionAt + 1);
        SkillType option = SkillTree.getType(optionId);
        if (option != null && node.getType().getOptionalOptionIds().contains(optionId)) {
            data.selectOption(node, option, CHARGED_NODE_COST);
        }
    }

    private static String[] fields(String tag) {
        int prefixLength = prefixLength(tag);
        if (prefixLength == 0) {
            return NO_FIELDS;
        }
        String[] fields = tag.substring(prefixLength).split("\\" + FIELD_SEPARATOR, -1);
        return (fields.length == 3 || fields.length == 4) && !fields[0].isEmpty() ? fields : NO_FIELDS;
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
