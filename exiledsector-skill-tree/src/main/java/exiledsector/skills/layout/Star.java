package exiledsector.skills.layout;

import java.util.List;

public class Star extends SkillTreeObject {

    private final float radius;
    private final String starType;
    private final String color;
    private final Overlay overlay;

    public record Overlay(String path, float size, float alpha) {
    }

    public Star(String id, float x, float y, float radius, String starType, String color, List<String> tags) {
        this(id, x, y, radius, starType, color, null, tags);
    }

    public Star(String id, float x, float y, float radius, String starType, String color, Overlay overlay, List<String> tags) {
        super(id, x, y, tags);
        this.radius = radius;
        this.starType = starType;
        this.color = color;
        this.overlay = overlay;
    }

    public Overlay getOverlay() {
        return overlay;
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
