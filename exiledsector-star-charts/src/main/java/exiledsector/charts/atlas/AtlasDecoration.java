package exiledsector.charts.atlas;

import java.awt.Color;
import java.util.List;

public record AtlasDecoration(String id, List<String> sprites, String glow, int columns, int rows, float size, float sizeJitter, int count,
                              float spread, float spin, float drift, Color colour, Color glowColour, float alpha,
                              List<String> scatter) {

    public AtlasDecoration {
        sprites = List.copyOf(sprites);
        glow = glow == null ? "" : glow;
        columns = Math.max(1, columns);
        rows = Math.max(1, rows);
        size = Math.max(0f, size);
        sizeJitter = Math.max(0f, Math.min(1f, sizeJitter));
        count = Math.max(1, count);
        spread = Math.max(0f, spread);
        spin = Math.max(0f, spin);
        drift = Math.max(0f, drift);
        alpha = Math.max(0f, Math.min(1f, alpha));
        scatter = scatter == null ? List.of() : List.copyOf(scatter);
    }
}
