package exiledsector.charts;

public enum ChartRunState {
    OPEN,
    INSIDE,
    CLOSING,
    CLOSED;

    public boolean isLive() {
        return this == OPEN || this == INSIDE;
    }

    public boolean keepsRunEffects() {
        return this != CLOSED;
    }
}
