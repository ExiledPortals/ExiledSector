package exiledsector.ui;

final class DeleteConfirmation {

    static final float ARM_SECONDS = 3f;

    private String armedId;
    private float remainingSeconds;

    boolean click(String id) {
        if (id.equals(armedId)) {
            disarm();
            return true;
        }
        armedId = id;
        remainingSeconds = ARM_SECONDS;
        return false;
    }

    void advance(float amount) {
        if (armedId == null) {
            return;
        }
        remainingSeconds -= amount;
        if (remainingSeconds <= 0f) {
            disarm();
        }
    }

    boolean isArmed(String id) {
        return id != null && id.equals(armedId);
    }

    void disarm() {
        armedId = null;
        remainingSeconds = 0f;
    }
}
