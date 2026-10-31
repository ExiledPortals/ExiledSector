package exiledsector.charts.anomaly;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class StateFields {

    static final String SEPARATOR = ",";
    static final int SEED_RADIX = Character.MAX_RADIX;
    static final String TRUE = "1";
    static final String FALSE = "0";

    private final String[] parts;

    private StateFields(String[] parts) {
        this.parts = parts;
    }

    public static String join(String... fields) {
        return String.join(SEPARATOR, fields);
    }

    public static String join(List<String> fields) {
        return String.join(SEPARATOR, fields);
    }

    public static String flag(boolean value) {
        return value ? TRUE : FALSE;
    }

    public static String seed(long value) {
        return Long.toString(value, SEED_RADIX);
    }

    public static String decimal(float value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    public static boolean fits(String text) {
        return text != null && !text.isEmpty() && !text.contains(SEPARATOR) && !text.contains(";") && !text.contains("=");
    }

    public static List<String> list(String encoded) {
        List<String> entries = new ArrayList<>();
        for (String entry : encoded == null ? new String[0] : encoded.split(SEPARATOR)) {
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public static <T> T decode(String encoded, int count, Function<StateFields, T> reader) {
        String[] parts = encoded == null ? new String[0] : encoded.split(SEPARATOR, -1);
        if (parts.length != count) {
            return null;
        }
        try {
            return reader.apply(new StateFields(parts));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String text(int index) {
        return parts[index];
    }

    public boolean flag(int index) {
        return TRUE.equals(parts[index]);
    }

    public long seed(int index) {
        return Long.parseLong(parts[index], SEED_RADIX);
    }

    public float decimal(int index) {
        return Float.parseFloat(parts[index]);
    }

    public <E extends Enum<E>> E enumValue(int index, Class<E> type) {
        return Enum.valueOf(type, parts[index]);
    }
}
