package exiledsector.skills.decoration;

import java.util.List;

public class ShroudedMaw extends Decoration {

    private final float bodyLength;
    private final float rotationDegrees;

    public ShroudedMaw(String id, float x, float y, float bodyLength, float rotationDegrees, List<String> tags) {
        super(id, x, y, tags);
        this.bodyLength = bodyLength;
        this.rotationDegrees = rotationDegrees;
    }

    @Override
    public DecorationKind kind() {
        return DecorationKind.SHROUDED_MAW;
    }

    public float getLength() {
        return bodyLength;
    }

    public float getRotation() {
        return rotationDegrees;
    }
}
