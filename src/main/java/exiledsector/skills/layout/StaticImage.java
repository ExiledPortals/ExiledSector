package exiledsector.skills.layout;

import java.util.List;

public class StaticImage extends SkillTreeObject {

    private final float width;
    private final float height;
    private final String imagePath;
    private final Rotation rotation;

    public record Shape(float width, float height, String imagePath, Rotation rotation) {
    }

    public StaticImage(String id, float x, float y, Shape shape, List<String> tags) {
        super(id, x, y, tags);
        this.width = shape.width();
        this.height = shape.height();
        this.imagePath = shape.imagePath();
        this.rotation = shape.rotation();
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public String getImagePath() {
        return imagePath;
    }

    public float getRotation() {
        return rotation.degrees();
    }

    public float getRotationSpeed() {
        return rotation.speed();
    }
}
