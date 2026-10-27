package exiledsector.charts.layout.abyss;

import java.awt.Color;

public record AbyssPalette(String id, Color colour, Color darkColour, float vignette) {

    public AbyssPalette {
        vignette = Math.max(0f, Math.min(1f, vignette));
    }
}
