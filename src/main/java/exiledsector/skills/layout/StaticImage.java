package exiledsector.skills.layout;

import java.util.List;

public class StaticImage extends SkillTreeObject {

    private final float imageWidth;
    private final float imageHeight;
    private final String imagePath;
    private final Rotation rotation;

    public record Shape(float width, float height, String imagePath, Rotation rotation) {
    }

    public StaticImage(String id, float x, float y, Shape shape, List<String> tags) {
        super(id, x, y, tags);
        this.imageWidth = shape.width();
        this.imageHeight = shape.height();
        this.imagePath = shape.imagePath();
        this.rotation = shape.rotation();
    }

    public float getWidth() {
        return imageWidth;
    }

    public float getHeight() {
        return imageHeight;
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
