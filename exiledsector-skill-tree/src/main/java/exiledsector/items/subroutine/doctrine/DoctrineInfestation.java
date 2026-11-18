package exiledsector.items.subroutine.doctrine;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum DoctrineInfestation {

    THREAT("threat"),
    SHROUD("shroud");

    private final String dataKey;

    DoctrineInfestation(String dataKey) {
        this.dataKey = dataKey;
    }

    public String dataKey() {
        return dataKey;
    }

    public static DoctrineInfestation byDataKeyOrNull(String dataKey) {
        if (dataKey == null) {
            return null;
        }
        String normalisedKey = dataKey.trim().toLowerCase(Locale.ROOT);
        for (DoctrineInfestation infestation : values()) {
            if (infestation.dataKey.equals(normalisedKey)) {
                return infestation;
            }
        }
        return null;
    }

    public static String dataKeys() {
        return Arrays.stream(values()).map(DoctrineInfestation::dataKey).collect(Collectors.joining(", "));
    }
}
