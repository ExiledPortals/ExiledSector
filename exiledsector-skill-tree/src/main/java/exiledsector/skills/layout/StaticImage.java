package exiledsector.skills.layout;

import java.util.List;

public class StaticImage extends SkillTreeObject {

    private final float imageWidth;
    private final float imageHeight;
    private final String imagePath;
    private final Rotation rotation;
    private final boolean behindStars;

    public record Shape(float width, float height, String imagePath, Rotation rotation) {
    }

    public StaticImage(String id, float x, float y, Shape shape, List<String> tags) {
        this(id, x, y, shape, false, tags);
    }

    public StaticImage(String id, float x, float y, Shape shape, boolean behindStars, List<String> tags) {
        super(id, x, y, tags);
        this.imageWidth = shape.width();
        this.imageHeight = shape.height();
        this.imagePath = shape.imagePath();
        this.rotation = shape.rotation();
        this.behindStars = behindStars;
    }

    public boolean isBehindStars() {
        return behindStars;
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
