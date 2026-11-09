package exiledsector.skills.decoration;

import exiledsector.skills.layout.SkillTreeObject;
import exiledsector.skills.layout.TreeTags;

public abstract class Decoration extends SkillTreeObject {

    protected Decoration(String id, float x, float y, TreeTags tags) {
        super(id, x, y, tags);
    }

    public abstract DecorationKind kind();
}
