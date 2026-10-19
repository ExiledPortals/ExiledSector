package exiledsector.skills.radius;

public enum RadiusSize {
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large");

    private final String dataKey;

    RadiusSize(String dataKey) {
        this.dataKey = dataKey;
    }

    public String dataKey() {
        return dataKey;
    }
}
