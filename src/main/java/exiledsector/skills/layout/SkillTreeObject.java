package exiledsector.skills.layout;

import exiledsector.skills.tags.SkillTags;

import java.util.Collections;
import java.util.List;

public abstract class SkillTreeObject {

    private final String id;
    private final float treeX;
    private final float treeY;
    private final List<String> tags;

    protected SkillTreeObject(String id, float treeX, float treeY, List<String> tags) {
        this.id = id;
        this.treeX = treeX;
        this.treeY = treeY;
        this.tags = tags == null ? Collections.emptyList() : tags;
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
        return tags;
    }

    public String getRegion() {
        for (String tag : tags) {
            if (SkillTags.isRegion(tag)) {
                return tag;
            }
        }
        return null;
    }
}
