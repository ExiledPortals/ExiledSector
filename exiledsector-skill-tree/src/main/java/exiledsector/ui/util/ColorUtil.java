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

    private static Color parseHexColorOrThrow(String hex) {
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        if (cleaned.length() == 6) cleaned = "FF" + cleaned;
        long argb = Long.parseLong(cleaned, 16);
        return new Color((int) argb, true);
    }
}
