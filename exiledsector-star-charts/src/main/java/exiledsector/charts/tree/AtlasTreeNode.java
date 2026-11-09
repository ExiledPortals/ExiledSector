package exiledsector.charts.tree;

import exiledsector.skills.SkillTier;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.layout.TreeNode;
import exiledsector.skills.layout.TreeTags;

import java.util.List;

public final class AtlasTreeNode extends TreeNode {

    private final AtlasTreeType type;

    public AtlasTreeNode(String id, AtlasTreeType type, List<String> connectedNodeIds, float x, float y, SkillNodeDecoration decoration,
                         TreeTags treeTags) {
        super(id, connectedNodeIds, x, y, decoration, treeTags);
        this.type = type;
    }

    public AtlasTreeType type() {
        return type;
    }

    @Override
    public SkillTier getTier() {
        return type.tier();
    }

    @Override
    public String getDisplayName() {
        return type.displayName();
    }

    @Override
    public String getIconPath() {
        return type.iconPath();
    }
}
