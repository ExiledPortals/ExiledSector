package exiledsector.skills.layout;

public class ConnectorCurve {

    private final float controlOffsetX;
    private final float controlOffsetY;

    public ConnectorCurve(float controlOffsetX, float controlOffsetY) {
        this.controlOffsetX = controlOffsetX;
        this.controlOffsetY = controlOffsetY;
    }

    public float getControlOffsetX() {
        return controlOffsetX;
    }

    public float getControlOffsetY() {
        return controlOffsetY;
    }
}
