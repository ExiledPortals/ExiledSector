package exiledsector.ui.decoration;

public record InspectedShip(float screenX, float screenY, float screenScale, float longestSidePixels, float facingDeg) {

    public float pointScreenX(HullPoint point) {
        double facing = Math.toRadians(facingDeg);
        return screenX + HullFrame.rotatedX(point.forward(), point.left(), (float) Math.cos(facing), (float) Math.sin(facing)) * screenScale;
    }

    public float pointScreenY(HullPoint point) {
        double facing = Math.toRadians(facingDeg);
        return screenY + HullFrame.rotatedY(point.forward(), point.left(), (float) Math.cos(facing), (float) Math.sin(facing)) * screenScale;
    }
}
