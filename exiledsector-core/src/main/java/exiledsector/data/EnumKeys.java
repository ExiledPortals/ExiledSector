package exiledsector.data;

import java.util.Locale;

public final class EnumKeys {

    private EnumKeys() {
    }

    public static String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String text) {
        if (text == null) {
            return null;
        }
        String wanted = text.trim();
        for (E value : type.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(wanted)) {
                return value;
            }
        }
        return null;
    }

    public static <E extends Enum<E>> E parseOr(Class<E> type, String text, E blankOrUnknown) {
        E parsed = parse(type, text);
        return parsed == null ? blankOrUnknown : parsed;
    }
}
