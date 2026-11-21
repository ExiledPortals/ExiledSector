package exiledsector.skills.decoration;

import exiledsector.skills.layout.Rotation;
import exiledsector.skills.layout.TreeTags;

public class RingBelt extends Decoration {

    private final float innerRadius;
    private final float outerRadius;
    private final String ringArtPath;
    private final Rotation rotation;

    public record Shape(float innerRadius, float outerRadius, String ringArtPath, Rotation rotation) {
    }

    public RingBelt(String id, float x, float y, Shape shape, TreeTags tags) {
        super(id, x, y, tags);
        this.innerRadius = shape.innerRadius();
        this.outerRadius = shape.outerRadius();
        this.ringArtPath = shape.ringArtPath();
        this.rotation = shape.rotation();
    }

    @Override
    public DecorationKind kind() {
        return DecorationKind.RING_BELT;
    }

    public float getInnerRadius() {
        return innerRadius;
    }

    public float getOuterRadius() {
        return outerRadius;
    }

    public String getRingArtPath() {
        return ringArtPath;
    }

    public float getRotation() {
        return rotation.degrees();
    }

    public float getRotationSpeed() {
        return rotation.speed();
    }
}
