package exiledsector.i18n;

import java.math.BigDecimal;

public final class NumberText {

    private static final float LARGEST_EXACT_WHOLE = 1e15f;

    private NumberText() {
    }

    public static String format(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return String.valueOf(value);
        }
        if (value == Math.rint(value) && Math.abs(value) < LARGEST_EXACT_WHOLE) {
            return Long.toString((long) value);
        }
        return new BigDecimal(Float.toString(value)).stripTrailingZeros().toPlainString();
    }
}
