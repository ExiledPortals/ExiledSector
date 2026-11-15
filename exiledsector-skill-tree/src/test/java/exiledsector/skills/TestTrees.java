package exiledsector.skills;

import exiledsector.skills.layout.SkillNodeDecoration;

import java.util.List;

public final class TestTrees {

    private TestTrees() {
    }

    public static void clear() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    public static SkillType.Builder type(String id, SkillTier tier) {
        return new SkillType.Builder(id, id + " name", "a.png", tier);
    }

    public static SkillType registered(SkillType type) {
        SkillTree.registerType(type);
        return type;
    }

    public static SkillNode node(String id, SkillType type, String... connectedTo) {
        SkillNode node = new SkillNode(id, type, List.of(connectedTo), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    public static SkillNode node(String id, SkillTier tier, String... connectedTo) {
        return node(id, type(id + "_type", tier).build(), connectedTo);
    }

    public static SkillNode small(String id, String... connectedTo) {
        return node(id, SkillTier.SMALL, connectedTo);
    }

    public static SkillNode taggedNode(String id, SkillTier tier, String tag, String... connectedTo) {
        SkillNode node = new SkillNode(id, type(id + "_type", tier).build(), List.of(connectedTo), 0f, 0f, SkillNodeDecoration.NONE, List.of(tag));
        SkillTree.register(node);
        return node;
    }
}
