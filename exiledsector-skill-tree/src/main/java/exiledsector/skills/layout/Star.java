package exiledsector.skills.layout;

import java.util.List;

public class Star extends SkillTreeObject {

    private final float radius;
    private final String starType;
    private final String color;

    public Star(String id, float x, float y, float radius, String starType, String color, List<String> tags) {
        super(id, x, y, tags);
        this.radius = radius;
        this.starType = starType;
        this.color = color;
    }

    public float getRadius() {
        return radius;
    }

    public String getStarType() {
        return starType;
    }

    public String getColor() {
        return color;
    }
}
