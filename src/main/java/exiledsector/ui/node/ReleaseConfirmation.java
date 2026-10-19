package exiledsector.ui.node;

final class ReleaseConfirmation {

    static final float ARM_SECONDS = 4f;

    private String armedActionKey;
    private String armedNodeId;
    private String armedWarning;
    private float remainingSeconds;

    boolean confirm(String actionKey) {
        if (actionKey.equals(armedActionKey)) {
            disarm();
            return true;
        }
        return false;
    }

    void arm(String actionKey, String nodeId, String warning) {
        armedActionKey = actionKey;
        armedNodeId = nodeId;
        armedWarning = warning;
        remainingSeconds = ARM_SECONDS;
    }

    void advance(float amount) {
        if (armedActionKey == null) {
            return;
        }
        remainingSeconds -= amount;
        if (remainingSeconds <= 0f) {
            disarm();
        }
    }

    String warningFor(String nodeId) {
        return nodeId != null && nodeId.equals(armedNodeId) ? armedWarning : null;
    }

    String warningForAction(String actionKey) {
        return actionKey != null && actionKey.equals(armedActionKey) ? armedWarning : null;
    }

    void disarm() {
        armedActionKey = null;
        armedNodeId = null;
        armedWarning = null;
        remainingSeconds = 0f;
    }
}
