package exiledsector.ui.decoration;

public final class LayerDrift {

    private final float layerSpeed;
    private final float leash;
    private final float settleSeconds;
    private boolean anchored;
    private float anchorX;
    private float anchorY;
    private float cameraX;
    private float cameraY;

    public LayerDrift(float layerSpeed, float mostShift, float settleSeconds) {
        this.layerSpeed = layerSpeed;
        float lag = Math.abs(1f - layerSpeed);
        this.leash = lag > 0f ? mostShift / lag : 0f;
        this.settleSeconds = settleSeconds;
    }

    public void follow(float newCameraX, float newCameraY) {
        cameraX = newCameraX;
        cameraY = newCameraY;
        if (!anchored) {
            anchorX = cameraX;
            anchorY = cameraY;
            anchored = true;
            return;
        }
        float awayX = cameraX - anchorX;
        float awayY = cameraY - anchorY;
        float away = (float) Math.sqrt(awayX * awayX + awayY * awayY);
        if (away > leash) {
            float pull = (away - leash) / away;
            anchorX += awayX * pull;
            anchorY += awayY * pull;
        }
    }

    public void advance(float amount) {
        if (!anchored || amount <= 0f) {
            return;
        }
        float settle = 1f - (float) Math.exp(-amount / settleSeconds);
        anchorX += (cameraX - anchorX) * settle;
        anchorY += (cameraY - anchorY) * settle;
    }

    public float shiftX() {
        return (1f - layerSpeed) * (cameraX - anchorX);
    }

    public float shiftY() {
        return (1f - layerSpeed) * (cameraY - anchorY);
    }
}
