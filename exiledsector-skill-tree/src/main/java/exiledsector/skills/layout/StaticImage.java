package exiledsector.skills.layout;

import java.util.List;

public class StaticImage extends SkillTreeObject {

    private final float imageWidth;
    private final float imageHeight;
    private final String imagePath;
    private final Rotation rotation;
    private final Depth depth;

    public record Shape(float width, float height, String imagePath, Rotation rotation) {
    }

    public record Depth(boolean behindStars, float parallax) {
        public static final Depth TREE = new Depth(false, 1f);
    }

    public StaticImage(String id, float x, float y, Shape shape, List<String> tags) {
        this(id, x, y, shape, Depth.TREE, tags);
    }

    public StaticImage(String id, float x, float y, Shape shape, Depth depth, List<String> tags) {
        super(id, x, y, tags);
        this.imageWidth = shape.width();
        this.imageHeight = shape.height();
        this.imagePath = shape.imagePath();
        this.rotation = shape.rotation();
        this.depth = depth;
    }

    public boolean isBehindStars() {
        return depth.behindStars();
    }

    public float getParallax() {
        return depth.parallax();
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
