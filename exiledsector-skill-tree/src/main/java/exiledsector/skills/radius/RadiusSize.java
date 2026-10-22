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

    public static RadiusSize byDataKeyOrNull(String key) {
        if (key == null) {
            return null;
        }
        String trimmedKey = key.trim();
        for (RadiusSize size : values()) {
            if (size.dataKey.equalsIgnoreCase(trimmedKey)) {
                return size;
            }
        }
        return null;
    }
}
