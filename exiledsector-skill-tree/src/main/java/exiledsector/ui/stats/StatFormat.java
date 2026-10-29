package exiledsector.ui.stats;

import exiledsector.i18n.Translation;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class StatFormat {

    private static final float EPSILON = 0.0005f;
    private static final float WHOLE_NUMBER_THRESHOLD = 10f;

    private StatFormat() {
    }

    static String number(float value) {
        return number(value, decimalsFor(value));
    }

    private static int decimalsFor(float value) {
        float magnitude = Math.abs(value);
        if (magnitude >= WHOLE_NUMBER_THRESHOLD) {
            return 0;
        }
        return magnitude >= 1f ? 1 : 2;
    }

    static String number(float value, int decimals) {
        String text = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return "-0".equals(text) ? "0" : text;
    }

    static String signedNumber(float value) {
        return (value < 0f ? "-" : "+") + number(Math.abs(value));
    }

    static String percent(float percent) {
        return number(percent) + "%";
    }

    static String signedPercent(float percent) {
        return signedNumber(percent) + "%";
    }

    static String multiplier(float mult) {
        float percent = Math.abs(mult - 1f) * 100f;
        return Translation.msg(mult >= 1f ? "ui.stats.amount.more" : "ui.stats.amount.less").arg("value", number(percent)).text();
    }

    static String change(float flat, float factor, boolean points) {
        StringBuilder text = new StringBuilder();
        if (Math.abs(flat) > EPSILON) {
            text.append(points ? signedPercent(flat * 100f) : signedNumber(flat));
        }
        if (Math.abs(factor - 1f) > EPSILON) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(signedPercent((factor - 1f) * 100f));
        }
        return text.isEmpty() ? "0%" : text.toString();
    }
}
