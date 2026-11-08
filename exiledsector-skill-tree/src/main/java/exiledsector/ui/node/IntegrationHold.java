package exiledsector.ui.node;

final class IntegrationHold {

    static final float HOLD_SECONDS = 3f;
    static final float CLICK_SECONDS = 0.25f;
    static final float RETRACT_SECONDS = 0.4f;

    enum Step { NONE, CLAIMED, COMPLETED }

    private String heldNodeId;
    private boolean affordable;
    private boolean holding;
    private boolean claimed;
    private float heldSeconds;
    private float progress;
    private String warning;

    void begin(String nodeId, boolean affordable) {
        heldNodeId = nodeId;
        this.affordable = affordable;
        holding = true;
        claimed = false;
        heldSeconds = 0f;
        progress = 0f;
        warning = null;
    }

    Step advance(float amount) {
        if (heldNodeId == null) {
            return Step.NONE;
        }
        if (!holding) {
            progress -= amount / RETRACT_SECONDS;
            if (progress <= 0f) {
                clear();
            }
            return Step.NONE;
        }
        heldSeconds += amount;
        if (affordable) {
            progress = Math.min(1f, heldSeconds / HOLD_SECONDS);
        }
        if (affordable && heldSeconds >= HOLD_SECONDS) {
            clear();
            claimed = true;
            return Step.COMPLETED;
        }
        if (!claimed && heldSeconds >= CLICK_SECONDS) {
            claimed = true;
            return Step.CLAIMED;
        }
        return Step.NONE;
    }

    boolean release() {
        boolean releaseClaimed = claimed;
        holding = false;
        claimed = false;
        warning = null;
        return releaseClaimed;
    }

    void cancel() {
        release();
    }

    void warn(String warning) {
        this.warning = warning;
    }

    String heldNodeId() {
        return holding ? heldNodeId : null;
    }

    boolean isAffordable() {
        return affordable;
    }

    String warningFor(String nodeId) {
        return nodeId != null && nodeId.equals(heldNodeId) ? warning : null;
    }

    float progressFor(String nodeId) {
        return nodeId != null && nodeId.equals(heldNodeId) ? progress : 0f;
    }

    private void clear() {
        heldNodeId = null;
        holding = false;
        heldSeconds = 0f;
        progress = 0f;
        warning = null;
    }
}
