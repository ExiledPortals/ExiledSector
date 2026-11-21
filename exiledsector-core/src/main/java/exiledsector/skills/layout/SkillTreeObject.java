package exiledsector.skills.layout;

import java.util.List;

public abstract class SkillTreeObject {

    private final String id;
    private final float treeX;
    private final float treeY;
    private final TreeTags treeTags;

    protected SkillTreeObject(String id, float treeX, float treeY, TreeTags treeTags) {
        this.id = id;
        this.treeX = treeX;
        this.treeY = treeY;
        this.treeTags = treeTags == null ? TreeTags.NONE : treeTags;
    }

    public String getId() {
        return id;
    }

    public float getX() {
        return treeX;
    }

    public float getY() {
        return treeY;
    }

    public List<String> getTags() {
        return treeTags.tags();
    }

    public TreeTags getTreeTags() {
        return treeTags;
    }

    public String getVolume() {
        return treeTags.volume();
    }
}
