package exiledsector.ui;

import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.ModSettings;

public final class TooltipTextSize {

    public static final String LARGE_FIELD_ID = "exiledSector_largeTooltipText";
    public static final boolean DEFAULT_LARGE = false;

    private static volatile boolean large = DEFAULT_LARGE;

    private TooltipTextSize() {
    }

    public static void reloadSettings() {
        large = ModSettings.booleanOr(LARGE_FIELD_ID, DEFAULT_LARGE);
    }

    public static boolean large() {
        return large;
    }

    public static void applyTitleFont(TooltipMakerAPI tooltip) {
        if (large) {
            tooltip.setTitleOrbitronVeryLarge();
        }
    }

    public static void applyNameFont(TooltipMakerAPI tooltip) {
        tooltip.setParaFont(large ? Fonts.ORBITRON_24AA : Fonts.ORBITRON_20AA);
    }

    public static void applyBodyFont(TooltipMakerAPI tooltip) {
        if (large) {
            tooltip.setParaInsigniaVeryLarge();
        } else {
            tooltip.setParaFontDefault();
        }
    }

    public static void setLargeForTests(boolean testLarge) {
        large = testLarge;
    }
}
