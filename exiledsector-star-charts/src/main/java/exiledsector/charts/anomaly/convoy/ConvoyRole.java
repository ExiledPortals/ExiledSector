package exiledsector.charts.anomaly.convoy;

public enum ConvoyRole {
    FREIGHTERS("freighters"),
    SCREEN("screen"),
    ESCORT("escort");

    private final String key;

    ConvoyRole(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
