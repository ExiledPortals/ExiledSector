package exiledsector.ui.util;

import org.apache.log4j.Logger;

import java.awt.Color;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static Color parseHexColor(String hex, Color fallback) {
        if (hex == null || hex.isEmpty()) return fallback;
        try {
            return parseHexColorOrThrow(hex);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static Color parseHexColor(String hex, Color fallback, Logger logger, String context) {
        if (hex == null || hex.isEmpty()) return fallback;
        try {
            return parseHexColorOrThrow(hex);
        } catch (NumberFormatException e) {
            logger.warn("Invalid " + context + " \"" + hex + "\", using default");
            return fallback;
        }
    }

    public static Color mix(Color base, Color other, float amount) {
        return new Color(Math.round(base.getRed() + (other.getRed() - base.getRed()) * amount),
                Math.round(base.getGreen() + (other.getGreen() - base.getGreen()) * amount),
                Math.round(base.getBlue() + (other.getBlue() - base.getBlue()) * amount));
    }

    public static Color lighten(Color color, float amount) {
        return mix(color, Color.WHITE, amount);
    }

    public static Color parseHexColorOrNull(String hex) {
        return parseHexColor(hex, null);
    }

    private static Color parseHexColorOrThrow(String hex) {
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        if (cleaned.length() == 6) cleaned = "FF" + cleaned;
        long argb = Long.parseLong(cleaned, 16);
        return new Color((int) argb, true);
    }
}
