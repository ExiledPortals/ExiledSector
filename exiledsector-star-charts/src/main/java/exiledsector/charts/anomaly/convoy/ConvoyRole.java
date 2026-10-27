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

    public static ConvoyRole parse(String key) {
        for (ConvoyRole role : values()) {
            if (role.key.equals(key)) {
                return role;
            }
        }
        return null;
    }
}
