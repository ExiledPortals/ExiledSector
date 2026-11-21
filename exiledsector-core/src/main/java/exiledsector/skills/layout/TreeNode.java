package exiledsector.skills.layout;

import exiledsector.skills.SkillTier;

import java.util.List;

public abstract class TreeNode extends SkillTreeObject {

    private final List<String> connectedNodeIds;
    private final SkillNodeDecoration decoration;

    protected TreeNode(String id, List<String> connectedNodeIds, float x, float y, SkillNodeDecoration decoration, TreeTags treeTags) {
        super(id, x, y, treeTags);
        this.connectedNodeIds = connectedNodeIds == null ? List.of() : connectedNodeIds;
        this.decoration = decoration == null ? SkillNodeDecoration.NONE : decoration;
    }

    public abstract SkillTier getTier();

    public abstract String getDisplayName();

    public abstract String getIconPath();

    public List<String> getConnectedNodeIds() {
        return connectedNodeIds;
    }

    public SkillNodeDecoration getDecoration() {
        return decoration;
    }

    public float getOffsetX() {
        return getX();
    }

    public float getOffsetY() {
        return getY();
    }

    public String getRingBeltPath() {
        return decoration.ringBeltPath();
    }

    public String getRingBeltColor() {
        return decoration.ringBeltColor();
    }

    public Float getRingBeltWidth() {
        return decoration.ringBeltWidth();
    }

    public String getWormholeColor() {
        return decoration.wormholeColor();
    }

    public String getPairedNodeId() {
        return decoration.pairedNodeId();
    }
}
