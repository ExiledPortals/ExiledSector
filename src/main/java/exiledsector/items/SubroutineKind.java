package exiledsector.items;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum SubroutineKind {

    VOLUME,
    DOCTRINE,
    BYPASS,
    UNIQUE;

    public String id() {
        return name();
    }

    public static SubroutineKind byIdOrNull(String id) {
        if (id == null) {
            return null;
        }
        String normalisedId = id.trim().toUpperCase(Locale.ROOT);
        for (SubroutineKind kind : values()) {
            if (kind.name().equals(normalisedId)) {
                return kind;
            }
        }
        return null;
    }

    public static String ids() {
        return Arrays.stream(values()).map(SubroutineKind::id).collect(Collectors.joining(", "));
    }
}
