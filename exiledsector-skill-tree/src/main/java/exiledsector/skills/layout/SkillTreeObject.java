package exiledsector.skills.layout;

import exiledsector.skills.tags.SkillTags;

import java.util.Collections;
import java.util.List;

public abstract class SkillTreeObject {

    private final String id;
    private final float treeX;
    private final float treeY;
    private final List<String> tags;
    private final String volume;

    protected SkillTreeObject(String id, float treeX, float treeY, List<String> tags) {
        this.id = id;
        this.treeX = treeX;
        this.treeY = treeY;
        this.tags = tags == null ? Collections.emptyList() : tags;
        this.volume = firstVolume(this.tags);
    }

    private static String firstVolume(List<String> tags) {
        for (String tag : tags) {
            if (SkillTags.isVolume(tag)) {
                return tag;
            }
        }
        return null;
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

    public String getVolume() {
        return volume;
    }
}
