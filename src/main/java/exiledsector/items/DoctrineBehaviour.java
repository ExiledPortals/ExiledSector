package exiledsector.items;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum DoctrineBehaviour {

    TRANSFORM,
    AUGMENT;

    public String id() {
        return name();
    }

    public static DoctrineBehaviour byIdOrNull(String id) {
        if (id == null) {
            return null;
        }
        String normalisedId = id.trim().toUpperCase(Locale.ROOT);
        for (DoctrineBehaviour behaviour : values()) {
            if (behaviour.name().equals(normalisedId)) {
                return behaviour;
            }
        }
        return null;
    }

    public static String ids() {
        return Arrays.stream(values()).map(DoctrineBehaviour::id).collect(Collectors.joining(", "));
    }
}
