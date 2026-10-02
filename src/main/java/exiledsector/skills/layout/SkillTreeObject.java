package exiledsector.skills.layout;

import exiledsector.skills.tags.SkillTags;

import java.util.Collections;
import java.util.List;

public abstract class SkillTreeObject {

    private final String id;
    private final float x;
    private final float y;
    private final List<String> tags;

    protected SkillTreeObject(String id, float x, float y) {
        this(id, x, y, null);
    }

    protected SkillTreeObject(String id, float x, float y, List<String> tags) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.tags = tags == null ? Collections.emptyList() : tags;
    }

    public String getId() {
        return id;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
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
