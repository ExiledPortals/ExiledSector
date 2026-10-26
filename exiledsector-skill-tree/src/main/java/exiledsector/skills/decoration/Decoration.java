package exiledsector.skills.decoration;

import exiledsector.skills.layout.SkillTreeObject;

import java.util.List;

public abstract class Decoration extends SkillTreeObject {

    protected Decoration(String id, float x, float y, List<String> tags) {
        super(id, x, y, tags);
    }

    public abstract DecorationKind kind();
}
