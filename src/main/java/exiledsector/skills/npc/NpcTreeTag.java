package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;

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

    private NpcTreeTag() {
    }

    public static String encode(ShipSkillData data) {
        List<String> nodes = new ArrayList<>();
        for (String nodeId : data.getAllocatedNodeIds()) {
            String option = data.getOptionalSelection(nodeId);
            nodes.add(option == null ? nodeId : nodeId + OPTION_SEPARATOR + option);
        }
        return PREFIX + GENERATED + FIELD_SEPARATOR + data.getLevel() + FIELD_SEPARATOR + String.join(NODE_SEPARATOR, nodes);
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
        if (fields == null) {
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
        return data;
    }

    private static void restore(ShipSkillData data, String entry) {
        int optionAt = entry.indexOf(OPTION_SEPARATOR);
        String nodeId = optionAt < 0 ? entry : entry.substring(0, optionAt);
        SkillNode node = SkillTree.get(nodeId);
        if (node == null || data.isAllocated(nodeId) || node.getType().getTier() == SkillTier.ROOT) {
            return;
        }
        if (optionAt < 0) {
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
            return null;
        }
        String[] fields = tag.substring(prefixLength).split("\\" + FIELD_SEPARATOR, -1);
        return (fields.length == 3 || fields.length == 4) && !fields[0].isEmpty() ? fields : null;
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
